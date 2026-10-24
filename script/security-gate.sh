#!/usr/bin/env bash
# 泛微E9 落地护栏 CI 门禁（SpringBlade 后端）
# 对应《泛微E9风险-SpringBlade落地护栏清单》§5：SQL 拼接 / MyBatis ${} / GET 写操作 / 缺 @PreAuth
# 用法：bash scripts/security-gate.sh   （CI 中非零退出即阻断）
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCAN_DIRS=("$ROOT/blade-service" "$ROOT/blade-service-api")
found=0

# 白名单：内部 Feign（约定不加 @PreAuth，网关 InnerFilter 隔离）、测试、生成代码
is_allow() {
  case "$1" in
    */feign/*|*/test/*|*Test.java|*.generated.*) return 0 ;;
    *) return 1 ;;
  esac
}

echo "== [1/3] SQL 字符串拼接 (DOC-P0-2/5, WF-P0-1/2, ODOC-P0-3) =="
while IFS=: read -r f l line; do
  [ -z "$f" ] && continue
  is_allow "$f" && continue
  echo "  VIOLATION: $f:$l  $line"
  found=1
done < <(grep -rnE '("|\x27).*(select|where|from|update|delete|insert|join)\s+.*"\s*\+' "${SCAN_DIRS[@]}" --include=*.java --exclude-dir=target)

echo "== [2/3] MyBatis \${} 非参数化插值（仅 xml 映射） =="
while IFS=: read -r f l line; do
  [ -z "$f" ] && continue
  case "$f" in *.xml) ;; *) continue ;; esac
  echo "  VIOLATION: $f:$l  $line"
  found=1
done < <(grep -rnE '\$\{' "${SCAN_DIRS[@]}" --include=*Mapper.xml --exclude-dir=target | grep -v 'ew\.customSqlSegment')

echo "== [3/3] GET 写操作 (G1) =="
while IFS=: read -r f l line; do
  [ -z "$f" ] && continue
  is_allow "$f" && continue
  echo "  VIOLATION: $f:$l  $line"
  found=1
done < <(grep -rnE '@GetMapping\("[^"]*(delete|remove|update|save|submit|publish|invalidate|settop)' "${SCAN_DIRS[@]}" --include=*.java -i --exclude-dir=target | grep -vi 'delete-check')

echo "== [4/?] 映射方法缺 @PreAuth（启发式，仅提示，不阻断） =="
while IFS= read -r f; do
  is_allow "$f" && continue
  awk '
    { line[NR] = $0 }
    /@(Get|Post|Put|Delete|Patch)Mapping/ {
      hitline = NR
      ctx = ""
      start = (NR - 3 > 1) ? NR - 3 : 1
      for (i = start; i < NR; i++) ctx = ctx line[i] "\n"
      if (ctx !~ /@PreAuth|@PermitAll|@PreIgnore|@SaCheckPermission/) {
        printf "  REVIEW: %s:%d  (no @PreAuth within 3 lines above mapping)\n", FILENAME, hitline
      }
    }
  ' "$f"
done < <(grep -rlE '@(Get|Post|Put|Delete|Patch)Mapping' "${SCAN_DIRS[@]}" --include=*.java)

echo "== [5/?] 过度放行 @PreAuth(\"permitAll()\")（含 WfOdocController:232 同款，需复核是否真公开）= "
grep -rnE '@PreAuth\("permitAll\(\)"\)' "${SCAN_DIRS[@]}" --include=*.java --exclude-dir=target | while IFS=: read -r f l line; do
  [ -z "$f" ] && continue
  echo "  REVIEW: $f:$l  $line"
done

if [ "$found" -eq 1 ]; then
  echo "SECURITY GATE FAILED"
  exit 1
fi
echo "SECURITY GATE PASSED"

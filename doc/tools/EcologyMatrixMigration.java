import java.sql.*;
import java.util.*;

/**
 * E9 (ecology) → SpringBlade 组织矩阵迁移工具（单文件 JDBC，零框架依赖，JDK 21 源码模式运行）。
 *
 * 与 {@code EcologyOrgMigration.java} 同款风格：直连 E9(SQL Server) 读矩阵，直连 blade(MySQL) 落库。
 *
 * 用法：
 *   java -cp "mssql-jdbc-*.jre11.jar;mysql-connector-j-*.jar" EcologyMatrixMigration.java [--dry-run] \
 *        [--eco-url ... --eco-user ... --eco-pass ...] [--blade-url ... --blade-user ... --blade-pass ...]
 *
 * 同步范围：E9 matrixinfo 中全部矩阵（含 2 个系统矩阵 1 分部矩阵 / 2 部门矩阵）。
 *
 * 映射规则（已与组织迁移对齐，并经实测）：
 *   ① 矩阵定义 / 字段：直接搬；系统矩阵 2 额外补 departmentDefineField 的 6 个结果维度（空值待 UI 填）。
 *   ② 结果维度（人力资源，browserType 1/17）：E9 HrmResource.id == blade_user.id（账号一致），原值直搬；
 *      若某用户 id 在 blade 不存在则保留原值并计入报告。
 *   ③ 条件维度（部门 browserType 4/57）：正值 E9 部门 id + 2_000_000_000 → blade_dept.id；
 *      （分部 browserType 164/194）：正值 E9 分部 id + 1_000_000_000 → blade_dept.id；
 *      经 blade_dept 存在性校验，校验通过才改写，否则保留原值。
 *   ④ 负值 / 非正整 / 非部门分部类型（如项目 browserType 8）：保留原值并计入报告「待人工核对清单」。
 *
 * 幂等：每次对目标 matrix_id 先物理 DELETE wf_matrix_field / wf_matrix_data / wf_matrix 再 INSERT，
 *       故可重复运行（重跑覆盖）。字段/数据 id 用 9e18 命名空间，与 blade 既有雪花 id 不冲突。
 *
 * 回滚：执行前请先备份 wf_matrix / wf_matrix_field / wf_matrix_data（如 CREATE TABLE ..._bak LIKE ...）。
 */
public class EcologyMatrixMigration {

    static final long SUB_OFFSET = 1_000_000_000L;    // 分部 id 命名空间（对齐 EcologyOrgMigration）
    static final long DEPT_OFFSET = 2_000_000_000L;   // 部门 id 命名空间
    static final long ID_BASE = 9_000_000_000_000_000_000L; // 字段/数据 id 命名空间（< Long.MAX_VALUE）
    static final String TENANT = "000000";

    // 默认连接参数（与 EcologyDataSourceConfig.java / EcologyOrgMigration.java 一致）
    static String ecoUrl = "jdbc:sqlserver://192.168.1.5:1433;databaseName=ecology2020_demo;encrypt=false;trustServerCertificate=true";
    static String ecoUser = "sa";
    static String ecoPass = "1";
    static String bladeUrl = "jdbc:mysql://localhost:3306/blade?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useAffectedRows=true";
    static String bladeUser = "root";
    static String bladePass = "123456";

    static boolean dryRun = false;

    // 统计与报告
    static int matCnt = 0, fieldCnt = 0, dataCnt = 0;
    static List<String> report = new ArrayList<>(); // 待核清单
    static Set<String> sbDeptIds = new HashSet<>();
    static Set<String> sbUserIds = new HashSet<>();

    public static void main(String[] args) throws Exception {
        parseArgs(args);
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection eco = DriverManager.getConnection(ecoUrl, ecoUser, ecoPass);
             Connection blade = DriverManager.getConnection(bladeUrl, bladeUser, bladePass)) {
            eco.setReadOnly(true);
            blade.setAutoCommit(false);
            System.out.println("[connect] E9 OK, blade OK. dryRun=" + dryRun);

            loadBladeIds(blade);

            // 读 E9 元数据
            List<Map<String, Object>> matrixinfo = queryAll(eco, "SELECT id, name, descr, issystem FROM matrixinfo ORDER BY id");
            List<Map<String, Object>> fieldinfo = queryAll(eco, "SELECT matrixid, fieldname, displayname, browsertypeid, fieldtype, priority, colwidth FROM matrixfieldinfo");
            List<Map<String, Object>> defineFields = queryAll(eco, "SELECT billid, fieldname, type FROM departmentDefineField WHERE billid=0");

            // 按 matrixid 归集字段
            Map<Long, List<Map<String, Object>>> fieldsByM = new LinkedHashMap<>();
            for (Map<String, Object> f : fieldinfo) {
                long mid = getLong(f, "matrixid");
                fieldsByM.computeIfAbsent(mid, k -> new ArrayList<>()).add(f);
            }

            long idSeq = ID_BASE;
            StringBuilder delSql = new StringBuilder();
            List<Object[]> mIns = new ArrayList<>();
            List<Object[]> fIns = new ArrayList<>();
            List<Object[]> dIns = new ArrayList<>();

            for (Map<String, Object> m : matrixinfo) {
                long mid = getLong(m, "id");
                String name = getStr(m, "name");
                String descr = blankToNull(getStr(m, "descr"));
                long issystem = getLong(m, "issystem");
                int isSys = (issystem == 1 || issystem == 2) ? 1 : 0;

                matCnt++;
                mIns.add(new Object[]{mid, TENANT, name, descr, isSys, 1, 1, 0L});

                // 字段：matrixfieldinfo + 系统矩阵补充
                List<FieldDef> fields = new ArrayList<>();
                for (Map<String, Object> f : fieldsByM.getOrDefault(mid, Collections.emptyList())) {
                    fields.add(new FieldDef(getStr(f, "fieldname"),
                            blankToNull(getStr(f, "displayname")) != null ? getStr(f, "displayname") : getStr(f, "fieldname"),
                            getLongOrNull(f, "browsertypeid"), getLong(f, "fieldtype"),
                            (int) getLong(f, "priority"), getLongOrNull(f, "colwidth")));
                }
                if (mid == 2) { // 系统矩阵2 补 6 个结果维度
                    int i = 0;
                    for (Map<String, Object> d : defineFields) {
                        fields.add(new FieldDef(getStr(d, "fieldname"), getStr(d, "fieldname"), getLong(d, "type"), 1, 100 + i, null));
                        i++;
                    }
                }

                // 读 Matrixtable 动态列，补充系统矩阵额外结果列（matrix_code 等）
                List<String> mtCols = new ArrayList<>();
                List<List<Object>> mtRows = new ArrayList<>();
                boolean hasTable = readMatrixtable(eco, mid, mtCols, mtRows);
                if (hasTable) {
                    Set<String> known = new HashSet<>();
                    for (FieldDef f : fields) known.add(f.key);
                    int extra = 0;
                    for (String c : mtCols) {
                        if ("uuid".equals(c) || "dataorder".equals(c)) continue;
                        if (known.contains(c)) continue;
                        fields.add(new FieldDef(c, c, null, 1, 200 + (extra++), null));
                    }
                }

                for (FieldDef f : fields) {
                    long fid = idSeq++;
                    fieldCnt++;
                    fIns.add(new Object[]{fid, TENANT, mid, f.key, f.name, f.bt, f.ft, f.order, f.cw, 1, 0});
                }

                if (!hasTable) continue;
                int dataIdx = mtCols.indexOf("dataorder");
                // 按 dataorder 排序后重排为 1..N（保证 uk 唯一）
                mtRows.sort((a, b) -> Double.compare(toDouble(a.get(dataIdx)), toDouble(b.get(dataIdx))));
                int seq = 1;
                for (List<Object> row : mtRows) {
                    Map<String, Integer> idxKey = new HashMap<>();
                    for (int i = 0; i < mtCols.size(); i++) idxKey.put(mtCols.get(i), i);
                    Map<String, String> json = new LinkedHashMap<>();
                    for (FieldDef f : fields) {
                        Integer ci = idxKey.get(f.key);
                        String raw = ci != null ? blankToEmpty(getStrSafe(row.get(ci))) : "";
                        String[] mv = mapValue(mid, name, f.key, f.ft, f.bt, raw);
                        json.put(f.key, mv[0]);
                        if (mv[1] != null) report.add(String.format("%d,%s,%s,%d,%s,%s,%s",
                                mid, name, f.key, f.ft, raw, mv[0], mv[1]));
                    }
                    long did = idSeq++;
                    dataCnt++;
                    dIns.add(new Object[]{did, TENANT, mid, seq, toJson(json), 1, 0});
                    seq++;
                }
            }

            if (dryRun) {
                System.out.println("[dry-run] 不落库。预估 matrices=" + matCnt + " fields=" + fieldCnt + " data=" + dataCnt + " 待核=" + report.size());
            } else {
                // 幂等清理（物理删除，绕过 @TableLogic）
                try (Statement st = blade.createStatement()) {
                    for (Object[] m : mIns) {
                        long mid = (long) m[0];
                        st.addBatch("DELETE FROM wf_matrix_field WHERE matrix_id=" + mid);
                        st.addBatch("DELETE FROM wf_matrix_data WHERE matrix_id=" + mid);
                        st.addBatch("DELETE FROM wf_matrix WHERE id=" + mid);
                    }
                    st.executeBatch();
                }
                batchInsert(blade, "wf_matrix",
                        new String[]{"id", "tenant_id", "matrix_name", "matrix_desc", "is_system", "status", "create_user", "is_deleted"},
                        new int[]{Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.BIGINT, Types.INTEGER},
                        mIns);
                batchInsert(blade, "wf_matrix_field",
                        new String[]{"id", "tenant_id", "matrix_id", "field_key", "display_name", "browser_type", "field_type", "field_order", "col_width", "status", "is_deleted"},
                        new int[]{Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.BIGINT, Types.INTEGER, Types.INTEGER, Types.BIGINT, Types.INTEGER, Types.INTEGER},
                        fIns);
                batchInsert(blade, "wf_matrix_data",
                        new String[]{"id", "tenant_id", "matrix_id", "data_order", "data_json", "status", "is_deleted"},
                        new int[]{Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER},
                        dIns);
                blade.commit();
                System.out.println("[exec] 已落库 matrices=" + matCnt + " fields=" + fieldCnt + " data=" + dataCnt + " 待核=" + report.size());
            }

            printReport();
        }
    }

    // ============ 值映射 ============
    static String[] mapValue(long mid, String mname, String fkey, long ftype, Long btype, String raw) {
        if (raw == null || raw.isEmpty()) return new String[]{"", null};
        // 结果维度（用户）
        if (ftype == 1 || (btype != null && (btype == 1 || btype == 17))) {
            List<String> missing = new ArrayList<>();
            for (String id : raw.split(",")) {
                id = id.trim();
                if (!id.isEmpty() && !sbUserIds.contains(id)) missing.add(id);
            }
            return new String[]{raw, missing.isEmpty() ? null : "结果用户ID在SB不存在:" + String.join(",", missing)};
        }
        if (btype != null && (btype == 4 || btype == 57)) {
            long num = parseLongOrZero(raw);
            if (num <= 0) return new String[]{raw, "条件[" + fkey + "]部门值非正整(" + raw + ")未映射"};
            long cand = DEPT_OFFSET + num;
            if (sbDeptIds.contains(String.valueOf(cand))) return new String[]{String.valueOf(cand), null};
            return new String[]{raw, "条件[" + fkey + "]部门 " + raw + "->期望" + cand + " 在SB不存在"};
        }
        if (btype != null && (btype == 164 || btype == 194)) {
            long num = parseLongOrZero(raw);
            if (num <= 0) return new String[]{raw, "条件[" + fkey + "]分部值非正整(" + raw + ")未映射"};
            long cand = SUB_OFFSET + num;
            if (sbDeptIds.contains(String.valueOf(cand))) return new String[]{String.valueOf(cand), null};
            return new String[]{raw, "条件[" + fkey + "]分部 " + raw + "->期望" + cand + " 在SB不存在"};
        }
        return new String[]{raw, "条件[" + fkey + "]browserType=" + btype + " 非部门/分部,未映射"};
    }

    // ============ 读 Matrixtable 动态列 ============
    static boolean readMatrixtable(Connection eco, long mid, List<String> cols, List<List<Object>> rows) {
        String sql = "SELECT * FROM Matrixtable_" + mid;
        try (Statement st = eco.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));
            while (rs.next()) {
                List<Object> r = new ArrayList<>();
                for (int i = 1; i <= n; i++) r.add(rs.getObject(i));
                rows.add(r);
            }
            return true;
        } catch (SQLException e) {
            // 表不存在（如矩阵12 人员 无数据表）
            return false;
        }
    }

    // ============ 批量插入 ============
    static void batchInsert(Connection c, String table, String[] cols, int[] types, List<Object[]> rows) throws SQLException {
        if (rows.isEmpty()) return;
        StringBuilder sb = new StringBuilder("INSERT INTO ").append(table).append(" (");
        sb.append(String.join(",", cols)).append(") VALUES (");
        for (int i = 0; i < cols.length; i++) sb.append(i == 0 ? "?" : ",?");
        sb.append(")");
        try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (Object[] vals : rows) {
                for (int i = 0; i < vals.length; i++) setParam(ps, i + 1, vals[i], types[i]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    static void setParam(PreparedStatement ps, int i, Object v, int type) throws SQLException {
        if (v == null) { ps.setNull(i, type); return; }
        if (type == Types.BIGINT) ps.setLong(i, ((Number) v).longValue());
        else if (type == Types.INTEGER) ps.setInt(i, ((Number) v).intValue());
        else ps.setString(i, v.toString());
    }

    // ============ blade id 集 ============
    static void loadBladeIds(Connection blade) throws SQLException {
        try (Statement st = blade.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM blade_dept WHERE is_deleted=0")) {
            while (rs.next()) sbDeptIds.add(rs.getString(1));
        }
        try (Statement st = blade.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM blade_user WHERE is_deleted=0")) {
            while (rs.next()) sbUserIds.add(rs.getString(1));
        }
        System.out.println("[blade] dept_ids=" + sbDeptIds.size() + " user_ids=" + sbUserIds.size());
    }

    // ============ JSON ============
    static String toJson(Map<String, String> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":\"").append(escape(e.getValue())).append("\"");
        }
        return sb.append("}").toString();
    }

    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // ============ 工具 ============
    static List<Map<String, Object>> queryAll(Connection c, String sql) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                for (int i = 1; i <= n; i++) m.put(md.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                list.add(m);
            }
        }
        return list;
    }

    static String getStr(Map<String, Object> m, String k) {
        Object o = m.get(k);
        return o == null ? null : o.toString();
    }

    static String getStrSafe(Object o) { return o == null ? null : o.toString(); }

    static String blankToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    static String blankToEmpty(String s) { return s == null ? "" : s; }

    static long getLong(Map<String, Object> m, String k) {
        Object o = m.get(k);
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return 0; }
    }

    static Long getLongOrNull(Map<String, Object> m, String k) {
        Object o = m.get(k);
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return null; }
    }

    static long parseLongOrZero(String s) {
        if (s == null) return 0;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; }
    }

    static double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString().trim()); } catch (Exception e) { return 0; }
    }

    static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dry-run": dryRun = true; break;
                case "--eco-url": ecoUrl = args[++i]; break;
                case "--eco-user": ecoUser = args[++i]; break;
                case "--eco-pass": ecoPass = args[++i]; break;
                case "--blade-url": bladeUrl = args[++i]; break;
                case "--blade-user": bladeUser = args[++i]; break;
                case "--blade-pass": bladePass = args[++i]; break;
                default: System.out.println("[warn] 未知参数: " + args[i]);
            }
        }
    }

    static void printReport() {
        System.out.println("\n========== 矩阵迁移报告 ==========");
        System.out.println("模式: " + (dryRun ? "DRY-RUN（未落库）" : "EXECUTE"));
        System.out.println("矩阵 wf_matrix: " + matCnt);
        System.out.println("字段 wf_matrix_field: " + fieldCnt);
        System.out.println("数据 wf_matrix_data: " + dataCnt);
        if (!report.isEmpty()) {
            System.out.println("---- 待人工核对清单(" + report.size() + ") ----");
            System.out.println("matrix_id,matrix_name,field_key,field_type,original,mapped,issue");
            for (String s : report) System.out.println(s);
        } else {
            System.out.println("---- 无待核项，全部映射成功 ----");
        }
        System.out.println("==============================");
    }

    /** 字段定义 */
    static class FieldDef {
        final String key, name;
        final Long bt;       // browser_type
        final long ft;       // field_type
        final int order;
        final Long cw;       // col_width
        FieldDef(String key, String name, Long bt, long ft, int order, Long cw) {
            this.key = key; this.name = name; this.bt = bt; this.ft = ft; this.order = order; this.cw = cw;
        }
    }
}

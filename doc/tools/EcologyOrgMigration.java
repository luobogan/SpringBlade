import java.sql.*;
import java.util.*;

/**
 * E9 (ecology) → SpringBlade 组织迁移工具（单文件 JDBC，零框架依赖，JDK 21 源码模式运行）。
 *
 * 用法：
 *   java -cp "mssql-jdbc-12.8.1.jre11.jar;mysql-connector-j-9.7.0.jar" EcologyOrgMigration.java [--dry-run] \
 *        [--eco-url ... --eco-user ... --eco-pass ...] [--blade-url ... --blade-user ... --blade-pass ...]
 *
 * 顺序：① 自愈 DDL（补齐 blade_user/blade_dept/blade_post 追加列与 ecology_id 追溯列）→
 *       ② 迁移分部(hrmsubcompany) → 部门(hrmdepartment) → 岗位(hrmjobtitles, v1.5 补) → 人员(hrmresource)（父先子后）→
 *       ③ 二次遍历计算 ancestors → ④ 输出迁移报告。
 *
 * 名称归一化（维森→鼎泰）：分部/部门名称文本（dept_name/full_name）中的“维森”替换为“鼎泰”，
 * 仅作用于分部/部门节点，人员姓名不改；替换后同父同名做“ (2)/(3)”后缀去重；pinyin 字段本工具不生成
 * （由 blade 侧组织保存时维护），避免拼音与已替换的新名不一致。
 *
 * 岗位映射（v1.5 补）：dst_id = 3e9 + src_id；post_code = jobtitlecode → jobtitlemark → JT+id（截断 12、
 * 租户内重复回退 JT+id、重跑时本行原编码豁免）；canceled='1' → status=0 且不进入人员 post_id 映射。
 *
 * 集团树 regroup（可选 --regroup-under-group，2026-10-09）：迁移后把根级分公司挂到「鼎泰集团股份有限公司」
 * （id=1000000005, ecology_id=5）下再计算 ancestors——E9 源数据是平铺（supsubcomid=0），不传此参则保持平铺。
 * 对应 SQL：doc/sql/regroup_group_tree.sql（另含 blade_tenant.tenant_name 同步）。
 *
 * 幂等：blade_dept/blade_user/blade_post 通过显式 id + ON DUPLICATE KEY UPDATE 实现重跑覆盖；ecology_id 为追溯列。
 * 回滚：执行前请先备份 blade_user/blade_dept/blade_post（如 CREATE TABLE ..._bak LIKE ...; INSERT ... SELECT *）。
 */
public class EcologyOrgMigration {

    static final long SUB_OFFSET = 1_000_000_000L;   // 分部 id 命名空间
    static final long DEPT_OFFSET = 2_000_000_000L;  // 部门 id 命名空间
    static final long POST_OFFSET = 3_000_000_000L;  // 岗位 id 命名空间（v1.5 补）
    static final String TENANT = "000000";
    static final String OLD = "维森";
    static final String NEW = "鼎泰";

    // 默认连接参数（取自项目内 EcologyDataSourceConfig.java 与 blade-system/application.yml）
    static String ecoUrl = "jdbc:sqlserver://192.168.1.5:1433;databaseName=ecology2020_demo;encrypt=false;trustServerCertificate=true";
    static String ecoUser = "sa";
    static String ecoPass = "1";
    static String bladeUrl = "jdbc:mysql://localhost:3306/blade?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useAffectedRows=true";
    static String bladeUser = "root";
    static String bladePass = "123456";

    static boolean dryRun = false;
    static boolean noBackup = false;
    static boolean regroupUnderGroup = false;

    // 统计与日志
    static int subIns = 0, subUpd = 0, depIns = 0, depUpd = 0, postIns = 0, postUpd = 0, usrIns = 0, usrUpd = 0;
    static List<String> skipLog = new ArrayList<>();
    static Set<String> seenDeptNames = new HashSet<>();
    // 岗位映射：ecology hrmjobtitles.id → blade_post.id（仅未封存岗位进入；供人员 post_id 使用）
    static Map<Long, Long> jobTitleDstMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        parseArgs(args);
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection eco = DriverManager.getConnection(ecoUrl, ecoUser, ecoPass);
             Connection blade = DriverManager.getConnection(bladeUrl, bladeUser, bladePass)) {
            eco.setReadOnly(true);
            System.out.println("[connect] E9 OK, blade OK. dryRun=" + dryRun);

            if (!dryRun && !noBackup) backupTables(blade);
            if (!dryRun) selfHealDDL(blade);

            migrateSubcompanies(eco, blade);
            migrateDepartments(eco, blade);
            migrateJobTitles(eco, blade);
            migratePersons(eco, blade);

            if (!dryRun && regroupUnderGroup) applyGroupRegroup(blade);
            if (!dryRun) computeAncestors(blade);

            printReport();
        }
    }

    // ============ 参数解析 ============
    static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dry-run": dryRun = true; break;
                case "--no-backup": noBackup = true; break;
                case "--regroup-under-group": regroupUnderGroup = true; break;
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

    // ============ 执行前备份（幂等：先删旧 bak 再重建）============
    static void backupTables(Connection blade) throws SQLException {
        String ts = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        backupOne(blade, "blade_user", "blade_user_bak_" + ts);
        backupOne(blade, "blade_dept", "blade_dept_bak_" + ts);
        backupOne(blade, "blade_post", "blade_post_bak_" + ts);
        System.out.println("[backup] 已备份至 blade_user_bak_" + ts + " / blade_dept_bak_" + ts + " / blade_post_bak_" + ts);
    }

    static void backupOne(Connection blade, String src, String bak) throws SQLException {
        try (Statement st = blade.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS " + bak);
            st.executeUpdate("CREATE TABLE " + bak + " LIKE " + src);
            st.executeUpdate("INSERT INTO " + bak + " SELECT * FROM " + src);
        }
    }

    // ============ 自愈 DDL ============
    static void selfHealDDL(Connection blade) throws SQLException {
        addCol(blade, "blade_user", "work_code", "varchar(50) NULL");
        addCol(blade, "blade_user", "person_status", "tinyint NULL");
        addCol(blade, "blade_user", "certificate_num", "varchar(30) NULL");
        addCol(blade, "blade_user", "salt", "varchar(64) NULL");
        addCol(blade, "blade_user", "ecology_id", "bigint NULL");
        addCol(blade, "blade_dept", "dept_type", "tinyint NOT NULL DEFAULT 2");
        addCol(blade, "blade_dept", "dept_code", "varchar(50) NULL");
        addCol(blade, "blade_dept", "subcompany_id", "bigint NULL");
        addCol(blade, "blade_dept", "canceled", "tinyint NOT NULL DEFAULT 0");
        addCol(blade, "blade_dept", "manager_user_id", "bigint NULL");
        addCol(blade, "blade_dept", "out_key", "varchar(64) NULL");
        addCol(blade, "blade_dept", "sync_uuid", "varchar(32) NULL");
        addCol(blade, "blade_dept", "pinyin", "varchar(100) NULL");
        addCol(blade, "blade_dept", "limit_users", "int NULL");
        addCol(blade, "blade_dept", "dept_level", "int NULL");
        addCol(blade, "blade_dept", "ecology_id", "bigint NULL");
        addCol(blade, "blade_post", "ecology_id", "bigint NULL");
        System.out.println("[ddl] 自愈 DDL 完成");
    }

    static void addCol(Connection c, String table, String col, String def) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?")) {
            ps.setString(1, table);
            ps.setString(2, col);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return; // 已存在
            }
        }
        try (Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + col + " " + def);
            System.out.println("[ddl] + " + table + "." + col);
        }
    }

    // ============ 分部迁移 ============
    static void migrateSubcompanies(Connection eco, Connection blade) throws SQLException {
        List<Map<String, Object>> rows = queryAll(eco, "SELECT * FROM hrmsubcompany");
        System.out.println("[sub] 读取 " + rows.size() + " 行");
        for (Map<String, Object> m : rows) {
            long srcId = getLong(m, "id");
            long id = SUB_OFFSET + srcId;
            String name = replaceName(getStr(m, "subcompanyname"));
            String full = replaceName(getStr(m, "subcompanydesc"));
            String code = blankToNull(getStr(m, "subcompanycode"));
            long sup = getLong(m, "supsubcomid");
            long parent = sup == 0 ? 0 : SUB_OFFSET + sup;
            int sort = getInt(m, "showorder");
            int canceled = "1".equals(getStr(m, "canceled")) ? 1 : 0;

            name = resolveDeptName(blade, parent, name, srcId);

            upsertDept(blade, id, parent, name, full, sort, canceled, 1, null, code, null, null, null, srcId, true);
        }
    }

    // ============ 部门迁移 ============
    static void migrateDepartments(Connection eco, Connection blade) throws SQLException {
        List<Map<String, Object>> rows = queryAll(eco, "SELECT * FROM hrmdepartment");
        System.out.println("[dep] 读取 " + rows.size() + " 行");
        for (Map<String, Object> m : rows) {
            long srcId = getLong(m, "id");
            long id = DEPT_OFFSET + srcId;
            String name = replaceName(getStr(m, "departmentmark"));
            String full = replaceName(getStr(m, "departmentname"));
            String code = blankToNull(getStr(m, "departmentcode"));
            long sup = getLong(m, "supdepid");
            long subId = getLong(m, "subcompanyid1");
            long subcompanyId = subId > 0 ? SUB_OFFSET + subId : null;

            long parent;
            if (sup == 0 && subId > 0) parent = SUB_OFFSET + subId;      // 顶级部门挂到其分部节点
            else if (sup > 0) parent = DEPT_OFFSET + sup;
            else parent = 0;

            int sort = getInt(m, "showorder");
            int canceled = "1".equals(getStr(m, "canceled")) ? 1 : 0;

            name = resolveDeptName(blade, parent, name, srcId);

            upsertDept(blade, id, parent, name, full, sort, canceled, 2, subcompanyId, code, null, null, null, srcId, false);
        }
    }

    // ============ 岗位迁移（v1.5 补，对齐 §10.1 hrmjobtitles → blade_post）============
    static void migrateJobTitles(Connection eco, Connection blade) throws SQLException {
        // 既有岗位编码（租户 000000）+ 重跑时本行原编码（按 ecology_id 定位，豁免自身）
        Set<String> existCodes = new HashSet<>();
        Map<Long, String> ownCodeBySrc = new HashMap<>();
        if (!dryRun) {
            try (Statement st = blade.createStatement();
                 ResultSet rs = st.executeQuery("SELECT post_code, ecology_id FROM blade_post WHERE tenant_id='" + TENANT + "'")) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    long ecoId = rs.getLong(2);
                    if (code != null && !code.isEmpty()) {
                        existCodes.add(code);
                        if (ecoId > 0) ownCodeBySrc.put(ecoId, code);
                    }
                }
            }
        }

        List<Map<String, Object>> rows = queryAll(eco, "SELECT * FROM hrmjobtitles");
        System.out.println("[post] 读取 " + rows.size() + " 行");
        for (Map<String, Object> m : rows) {
            long srcId = getLong(m, "id");
            String name = blankToNull(getStr(m, "jobtitlename"));
            String mark = blankToNull(getStr(m, "jobtitlemark"));
            String code = blankToNull(getStr(m, "jobtitlecode"));
            String remark = blankToNull(getStr(m, "jobtitleremark"));
            int canceled = "1".equals(getStr(m, "canceled")) ? 1 : 0;
            if (name == null) name = mark != null ? mark : ("未命名岗位" + srcId);
            name = truncate(name, 64);          // blade_post.post_name varchar(64)
            remark = truncate(remark, 255);     // blade_post.remark varchar(255)

            // post_code：jobtitlecode → jobtitlemark → JT+id；截断 12；租户内重复回退 JT+id（重跑时本行原编码豁免）
            String base = code != null ? code : (mark != null ? mark : ("JT" + srcId));
            base = truncate(base, 12);
            String own = ownCodeBySrc.get(srcId);
            String postCode;
            if (base.equals(own)) {
                postCode = base; // 重跑且编码未变
            } else if (existCodes.contains(base)) {
                String fb = truncate("JT" + srcId, 12);
                if (fb.equals(own)) postCode = fb;
                else if (existCodes.contains(fb)) {
                    skipLog.add("岗位编码冲突保留原值 post_code=" + base + " (ecology id=" + srcId + ")");
                    postCode = base;
                } else {
                    skipLog.add("岗位编码重复回退 post_code=" + base + " -> " + fb + " (ecology id=" + srcId + ")");
                    postCode = fb;
                }
            } else {
                postCode = base;
            }
            existCodes.add(postCode);

            long dstId = POST_OFFSET + srcId;
            upsertPost(blade, dstId, postCode, name, remark, canceled, srcId);

            // 封存岗位不进入人员 post_id 映射（人员侧置 NULL + 日志）
            if (canceled == 0) jobTitleDstMap.put(srcId, dstId);
        }
    }

    // ============ 人员迁移 ============
    static void migratePersons(Connection eco, Connection blade) throws SQLException {
        // 既有 blade 账号/工号，用于重复预检（记录归属 ecology_id：重跑时豁免本行自身，否则自碰撞
        // 会导致已迁移行被误判「账号重复跳过」/「工号重复置NULL」，post_id 永远补不上）
        Map<String, Long> existAccounts = new HashMap<>();
        Map<String, Long> existWorkCodes = new HashMap<>();
        if (!dryRun) {
            try (Statement st = blade.createStatement(); ResultSet rs = st.executeQuery("SELECT account,work_code,ecology_id FROM blade_user")) {
                while (rs.next()) {
                    String a = rs.getString(1);
                    String w = rs.getString(2);
                    long ecoId = rs.getLong(3);
                    if (a != null) existAccounts.putIfAbsent(a.toLowerCase(), ecoId);
                    if (w != null) existWorkCodes.putIfAbsent(w, ecoId);
                }
            }
        }

        // 密码不可迁移：统一取 admin 当前密码哈希
        String adminHash = null;
        try (Statement st = blade.createStatement(); ResultSet rs = st.executeQuery("SELECT password FROM blade_user WHERE account='admin' LIMIT 1")) {
            if (rs.next()) adminHash = rs.getString(1);
        }
        if (adminHash == null) System.out.println("[warn] 未取到 admin 密码哈希，迁移用户密码将为空，需管理员重置");

        List<Map<String, Object>> rows = queryAll(eco, "SELECT * FROM hrmresource");
        System.out.println("[usr] 读取 " + rows.size() + " 行");
        for (Map<String, Object> m : rows) {
            long srcId = getLong(m, "id");
            String loginid = blankToNull(getStr(m, "loginid"));
            String lastname = blankToNull(getStr(m, "lastname"));
            String workcode = blankToNull(getStr(m, "workcode"));
            int status = getInt(m, "status");
            String cert = blankToNull(getStr(m, "certificatenum"));
            long departmentid = getLong(m, "departmentid");
            long managerid = getLong(m, "managerid");
            int accounttype = getInt(m, "accounttype");
            String mobile = blankToNull(getStr(m, "mobile"));
            String email = blankToNull(getStr(m, "email"));
            Integer sex = getIntOrNull(m, "sex");
            String birthday = datePart(getStr(m, "birthday"));

            // 次账号：不迁移账号、工号置 NULL
            if (accounttype == 1) workcode = null;

            // 账号重复预检（归属非本行才跳过）
            if (loginid != null) {
                Long owner = existAccounts.get(loginid.toLowerCase());
                if (owner != null && owner != srcId) {
                    skipLog.add("账号重复跳过 account=" + loginid + " (ecology id=" + srcId + ")");
                    continue;
                }
            }
            // 工号重复：首个保留，其余置 NULL（迁移日志；归属本行则保留原值）
            if (workcode != null) {
                Long owner = existWorkCodes.get(workcode);
                if (owner != null && owner != srcId) {
                    skipLog.add("工号重复置NULL work_code=" + workcode + " (ecology id=" + srcId + ")");
                    workcode = null;
                } else if (owner == null) {
                    existWorkCodes.put(workcode, srcId);
                }
            }

            String deptId = departmentid > 0 ? String.valueOf(DEPT_OFFSET + departmentid) : null;
            Long managerId = managerid > 0 ? managerid : null; // blade 人员 id == E9 id

            // 岗位映射（v1.5 补）：3e9+jobtitle；缺失或封存 → NULL + 日志（对齐 §10.2）
            long jt = getLong(m, "jobtitle");
            Long postId = jt > 0 ? jobTitleDstMap.get(jt) : null;
            if (jt > 0 && postId == null) {
                skipLog.add("岗位缺失/封存置NULL jobtitle=" + jt + " (ecology id=" + srcId + ")");
            }

            upsertUser(blade, srcId, loginid, adminHash, lastname, mobile, email, sex, birthday,
                    deptId, managerId, postId, workcode, status, cert, srcId);
        }
    }

    // ============ 集团树 regroup（--regroup-under-group，见 doc/sql/regroup_group_tree.sql）============
    static void applyGroupRegroup(Connection blade) throws SQLException {
        int n;
        try (PreparedStatement ps = blade.prepareStatement(
                "UPDATE blade_dept SET parent_id = 1000000005 " +
                " WHERE ecology_id IS NOT NULL AND dept_type = 1 AND parent_id = 0 AND id <> 1000000005")) {
            n = ps.executeUpdate();
        }
        System.out.println("[regroup] 根级分公司挂到鼎泰集团(1000000005)下: " + n + " 行");
        // ancestors 由随后的 computeAncestors 按 new parent 链全量重算，无需在此处理
    }

    // ============ ancestors 二次计算 ============
    static void computeAncestors(Connection blade) throws SQLException {
        Map<Long, Long> parentMap = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        try (Statement st = blade.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,parent_id FROM blade_dept WHERE ecology_id IS NOT NULL")) {
            while (rs.next()) {
                long id = rs.getLong(1);
                long p = rs.getLong(2);
                parentMap.put(id, p);
                ids.add(id);
            }
        }
        System.out.println("[anc] 计算 " + ids.size() + " 个组织节点祖先链");
        try (PreparedStatement ps = blade.prepareStatement("UPDATE blade_dept SET ancestors=? WHERE id=?")) {
            for (long id : ids) {
                ps.setString(1, ancestorsOf(id, parentMap));
                ps.setLong(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    static String ancestorsOf(long id, Map<Long, Long> parentMap) {
        List<Long> chain = new ArrayList<>();
        Long cur = parentMap.get(id);
        int guard = 0;
        while (cur != null && cur != 0 && !chain.contains(cur) && guard++ < 32) {
            chain.add(cur);
            cur = parentMap.get(cur);
        }
        Collections.reverse(chain);
        StringBuilder sb = new StringBuilder("0");
        for (Long a : chain) sb.append(",").append(a);
        return sb.toString();
    }

    // ============ 名称归一化 ============
    static String replaceName(String s) {
        if (s == null) return null;
        return s.replace(OLD, NEW);
    }

    /** 同父同名去重：先查内存 seen 集合，再（非 dry-run）查库确认（排除本行自身 ecology_id，防重跑自碰撞改名），必要时追加 (2)/(3)。 */
    static String resolveDeptName(Connection blade, long parent, String name, long selfEcoId) {
        String base = (name == null || name.isEmpty()) ? "未命名组织" : name;
        String cand = base;
        int i = 2;
        while (seenDeptNames.contains(TENANT + "|" + parent + "|" + cand)) {
            cand = base + " (" + (i++) + ")";
        }
        if (!dryRun) {
            while (dbDeptNameExists(blade, parent, cand, selfEcoId)) {
                cand = base + " (" + (i++) + ")";
            }
        }
        if (!cand.equals(base)) {
            skipLog.add("分部/部门同名去重: '" + base + "' -> '" + cand + "' (parent=" + parent + ")");
        }
        seenDeptNames.add(TENANT + "|" + parent + "|" + cand);
        return cand;
    }

    static boolean dbDeptNameExists(Connection blade, long parent, String name, long selfEcoId) {
        try (PreparedStatement ps = blade.prepareStatement(
                "SELECT 1 FROM blade_dept WHERE tenant_id=? AND parent_id=? AND dept_name=? AND (ecology_id IS NULL OR ecology_id<>?) LIMIT 1")) {
            ps.setString(1, TENANT);
            ps.setLong(2, parent);
            ps.setString(3, name);
            ps.setLong(4, selfEcoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    // ============ upsert ============
    static void upsertDept(Connection blade, long id, long parent, String name, String full, int sort, int canceled,
                           int deptType, Long subcompanyId, String code, Long managerUserId, String outKey,
                           String syncUuid, Long ecologyId, boolean isSub) throws SQLException {
        String[] cols = {"id", "tenant_id", "parent_id", "ancestors", "dept_name", "full_name", "sort",
                "remark", "is_deleted", "status", "dept_type", "dept_code", "subcompany_id", "canceled",
                "manager_user_id", "out_key", "sync_uuid", "pinyin", "ecology_id"};
        Object[] vals = {id, TENANT, parent, "0", name, full, sort, null, 0, 1, deptType, code,
                subcompanyId, canceled, managerUserId, outKey, syncUuid, null, ecologyId};
        int[] types = {Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR,
                Types.BIGINT, Types.INTEGER, Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BIGINT};
        int n = upsert(blade, "blade_dept", cols, vals, types);
        if (n == 1) { if (isSub) subIns++; else depIns++; }
        else if (n == 2) { if (isSub) subUpd++; else depUpd++; }
    }

    static void upsertUser(Connection blade, long id, String account, String password, String realName,
                           String phone, String email, Integer sex, String birthday, String deptId,
                           Long managerId, Long postId, String workCode, int personStatus, String cert, long ecologyId) throws SQLException {
        String[] cols = {"id", "tenant_id", "account", "password", "name", "real_name", "phone", "email",
                "sex", "birthday", "dept_id", "manager_id", "post_id", "work_code", "person_status", "certificate_num",
                "status", "is_deleted", "ecology_id"};
        Object[] vals = {id, TENANT, account, password, realName, realName, phone, email,
                sex, birthday, deptId, managerId, postId, workCode, personStatus, cert, 1, 0, ecologyId};
        int[] types = {Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.BIGINT,
                Types.BIGINT, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.BIGINT};
        int n = upsert(blade, "blade_user", cols, vals, types);
        if (n == 1) usrIns++; else if (n == 2) usrUpd++;
    }

    static void upsertPost(Connection blade, long id, String postCode, String postName, String remark,
                           int canceled, long ecologyId) throws SQLException {
        String[] cols = {"id", "tenant_id", "category", "post_code", "post_name", "sort", "remark",
                "status", "is_deleted", "ecology_id"};
        Object[] vals = {id, TENANT, 1, postCode, postName, 0, remark,
                canceled == 1 ? 0 : 1, 0, ecologyId};
        int[] types = {Types.BIGINT, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR,
                Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.BIGINT};
        int n = upsert(blade, "blade_post", cols, vals, types);
        if (n == 1) postIns++; else if (n == 2) postUpd++;
    }

    /** 按 MySQL varchar 字符数截断（utf8mb4 按字符计），null 安全。 */
    static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 返回 1=插入 2=更新 0=无变化（dry-run 时返回 1 并仅计数，不落库）。 */
    static int upsert(Connection c, String table, String[] cols, Object[] vals, int[] types) throws SQLException {
        if (dryRun) return 1;
        StringBuilder sb = new StringBuilder("INSERT INTO ").append(table).append(" (");
        sb.append(String.join(",", cols)).append(") VALUES (");
        for (int i = 0; i < cols.length; i++) sb.append(i == 0 ? "?" : ",?");
        sb.append(") ON DUPLICATE KEY UPDATE ");
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(cols[i]).append("=VALUES(").append(cols[i]).append(")");
        }
        try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < vals.length; i++) setParam(ps, i + 1, vals[i], types[i]);
            return ps.executeUpdate();
        }
    }

    static void setParam(PreparedStatement ps, int i, Object v, int type) throws SQLException {
        if (v == null) { ps.setNull(i, type); return; }
        if (type == Types.BIGINT) ps.setLong(i, ((Number) v).longValue());
        else if (type == Types.INTEGER) ps.setInt(i, ((Number) v).intValue());
        else ps.setString(i, v.toString());
    }

    // ============ 工具 ============
    static List<Map<String, Object>> queryAll(Connection c, String sql) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
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

    static String blankToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    static long getLong(Map<String, Object> m, String k) {
        Object o = m.get(k);
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return 0; }
    }

    static int getInt(Map<String, Object> m, String k) {
        Object o = m.get(k);
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString().trim()); } catch (Exception e) { return 0; }
    }

    static Integer getIntOrNull(Map<String, Object> m, String k) {
        Object o = m.get(k);
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString().trim()); } catch (Exception e) { return null; }
    }

    static String datePart(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        // 取日期部分 yyyy-MM-dd
        int sp = s.indexOf(' ');
        String d = sp > 0 ? s.substring(0, sp) : s;
        return d.length() >= 10 ? d.substring(0, 10) : d;
    }

    static void printReport() {
        System.out.println("\n========== 迁移报告 ==========");
        System.out.println("模式: " + (dryRun ? "DRY-RUN（未落库）" : "EXECUTE"));
        System.out.println("分部 blade_dept(dept_type=1): 插入=" + subIns + " 更新=" + subUpd);
        System.out.println("部门 blade_dept(dept_type=2): 插入=" + depIns + " 更新=" + depUpd);
        System.out.println("岗位 blade_post:            插入=" + postIns + " 更新=" + postUpd);
        System.out.println("人员 blade_user:            插入=" + usrIns + " 更新=" + usrUpd);
        if (!skipLog.isEmpty()) {
            System.out.println("---- 跳过/调整明细(" + skipLog.size() + ") ----");
            for (String s : skipLog) System.out.println("  - " + s);
        }
        System.out.println("==============================");
        if (dryRun) System.out.println("以上为预估；去掉 --dry-run 后才会写入。pinyin 字段本工具不生成，由 blade 侧维护。");
    }
}

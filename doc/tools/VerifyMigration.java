import java.sql.*;
import java.util.*;

public class VerifyMigration {
    static final String URL = "jdbc:mysql://localhost:3306/blade?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    static final String U = "root";
    static final String P = "123456";

    static long q(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : -1;
        }
    }

    static boolean colExists(Connection c, String table, String col) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?")) {
            ps.setString(1, table);
            ps.setString(2, col);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(URL, U, P)) {
            System.out.println("分部(ecology_id IS NOT NULL): " + q(c, "SELECT COUNT(*) FROM blade_dept WHERE ecology_id IS NOT NULL"));
            System.out.println("部门(ecology_id IS NOT NULL): " + q(c, "SELECT COUNT(*) FROM blade_dept WHERE ecology_id IS NOT NULL AND dept_type=2"));
            System.out.println("人员(ecology_id IS NOT NULL): " + q(c, "SELECT COUNT(*) FROM blade_user WHERE ecology_id IS NOT NULL"));

            System.out.println("【维森残留】分部/部门名称含'维森'数(应为0): " +
                    q(c, "SELECT COUNT(*) FROM blade_dept WHERE ecology_id IS NOT NULL AND (dept_name LIKE '%维森%' OR full_name LIKE '%维森%')"));
            System.out.println("【鼎泰生效】分部/部门名称含'鼎泰'数(应>0): " +
                    q(c, "SELECT COUNT(*) FROM blade_dept WHERE ecology_id IS NOT NULL AND (dept_name LIKE '%鼎泰%' OR full_name LIKE '%鼎泰%')"));
            System.out.println("【ancestors】迁移节点中 ancestors 为 NULL 数(应为0): " +
                    q(c, "SELECT COUNT(*) FROM blade_dept WHERE ecology_id IS NOT NULL AND (ancestors IS NULL OR ancestors='')"));

            // 抽样：含鼎泰的节点（id 为数字，名称可能为终端编码，仅作存在性确认）
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id,dept_type,dept_name FROM blade_dept WHERE ecology_id IS NOT NULL AND (dept_name LIKE '%鼎泰%' OR full_name LIKE '%鼎泰%') LIMIT 8")) {
                System.out.println("---- 抽样含'鼎泰'节点 ----");
                while (rs.next()) System.out.println("  id=" + rs.getLong(1) + " type=" + rs.getInt(2) + " name=" + rs.getString(3));
            }

            // 抽样：迁移人员关键字段回读
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id,account,real_name,dept_id,manager_id,person_status,work_code,post_id FROM blade_user WHERE ecology_id IS NOT NULL LIMIT 5")) {
                System.out.println("---- 抽样迁移人员 ----");
                while (rs.next()) System.out.println("  id=" + rs.getLong(1) + " account=" + rs.getString(2)
                        + " name=" + rs.getString(3) + " dept_id=" + rs.getString(4) + " mgr=" + rs.getLong(5)
                        + " pstatus=" + rs.getInt(6) + " work=" + rs.getString(7) + " post_id=" + rs.getString(8));
            }

            // ===== 幂等完整性（v1.5 补迁新增核对项）=====
            System.out.println("---- 幂等完整性 ----");
            System.out.println("blade_user 总行数: " + q(c, "SELECT COUNT(*) FROM blade_user"));
            System.out.println("迁移人员 id<>ecology_id 行数(应为0，防重复插入): " +
                    q(c, "SELECT COUNT(*) FROM blade_user WHERE ecology_id IS NOT NULL AND id <> ecology_id"));
            System.out.println("组织节点名称带' (2)'后缀数: " +
                    q(c, "SELECT COUNT(*) FROM blade_dept WHERE ecology_id IS NOT NULL AND (dept_name LIKE '% (2)' OR full_name LIKE '% (2)')"));
            System.out.println("迁移岗位名称带' (2)'后缀数: " +
                    q(c, "SELECT COUNT(*) FROM blade_post WHERE ecology_id IS NOT NULL AND post_name LIKE '% (2)'"));

            // ===== 岗位（v1.5 补迁核对）=====
            if (colExists(c, "blade_post", "ecology_id")) {
                System.out.println("---- 岗位核对 ----");
                System.out.println("岗位(blade_post 总数): " + q(c, "SELECT COUNT(*) FROM blade_post"));
                System.out.println("岗位(ecology_id IS NOT NULL): " + q(c, "SELECT COUNT(*) FROM blade_post WHERE ecology_id IS NOT NULL"));
                System.out.println("迁移人员 post_id 已填数: " + q(c, "SELECT COUNT(*) FROM blade_user WHERE ecology_id IS NOT NULL AND post_id IS NOT NULL"));
                System.out.println("迁移人员 post_id 为 NULL 数: " + q(c, "SELECT COUNT(*) FROM blade_user WHERE ecology_id IS NOT NULL AND post_id IS NULL"));
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT p.id,p.post_code,p.post_name,p.status,COUNT(u.id) cnt FROM blade_post p LEFT JOIN blade_user u ON FIND_IN_SET(p.id,u.post_id)>0 WHERE p.ecology_id IS NOT NULL GROUP BY p.id,p.post_code,p.post_name,p.status ORDER BY cnt DESC LIMIT 8")) {
                    System.out.println("---- 抽样迁移岗位(含使用人数) ----");
                    while (rs.next()) System.out.println("  id=" + rs.getLong(1) + " code=" + rs.getString(2)
                            + " name=" + rs.getString(3) + " status=" + rs.getInt(4) + " users=" + rs.getLong(5));
                }
            } else {
                System.out.println("---- blade_post 尚无 ecology_id 列（岗位迁移未执行）----");
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT column_name,column_type,is_nullable,column_default FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='blade_post' ORDER BY ordinal_position")) {
                    System.out.println("blade_post 现有列：");
                    while (rs.next()) System.out.println("  " + rs.getString(1) + " " + rs.getString(2)
                            + " null=" + rs.getString(3) + " default=" + rs.getString(4));
                }
            }
        }
    }
}

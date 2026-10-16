import java.sql.*;

public class CheckMenu {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/blade?useSSL=false&allowMultiQueries=true"
            + "&serverTimezone=GMT%2B8&characterEncoding=utf-8&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url, "root", "123456");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT m.id, m.parent_id, m.code, m.name, m.path, m.category, m.is_open, m.is_component, "
                 + " (SELECT COUNT(*) FROM blade_role_menu rm WHERE rm.menu_id = m.id) AS granted "
                 + " FROM blade_menu m WHERE m.code LIKE 'portal%' AND m.is_deleted = 0 ORDER BY m.sort, m.id")) {
            while (rs.next()) {
                System.out.println(rs.getString(3) + " | name=" + rs.getString(4) + " | path=" + rs.getString(5)
                    + " | cat=" + rs.getInt(6) + " | open=" + rs.getInt(7) + " | comp=" + rs.getInt(8)
                    + " | granted=" + rs.getInt(9));
            }
        }
    }
}

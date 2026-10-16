import java.sql.*;

public class CheckPortalConfig {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/blade?useSSL=false&allowMultiQueries=true"
            + "&serverTimezone=GMT%2B8&characterEncoding=utf-8&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url, "root", "123456")) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT id, element_type, element_name, col_span, sort_order, config_json FROM blade_portal_element ORDER BY sort_order")) {
                System.out.println("id | type | name | span | sort | config_json");
                while (rs.next()) {
                    System.out.println(rs.getLong(1) + " | " + rs.getString(2) + " | " + rs.getString(3)
                        + " | " + rs.getInt(4) + " | " + rs.getInt(5) + " | " + rs.getString(6));
                }
            }
        }
    }
}

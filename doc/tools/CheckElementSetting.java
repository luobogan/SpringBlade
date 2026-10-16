import java.sql.*;

public class CheckElementSetting {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/blade?useSSL=false&allowMultiQueries=true"
            + "&serverTimezone=GMT%2B8&characterEncoding=utf-8&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url, "root", "123456");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT s.id, s.element_id, e.element_name, s.user_id, s.per_page, s.show_title, s.is_deleted "
                 + " FROM blade_portal_element_setting s LEFT JOIN blade_portal_element e ON e.id = s.element_id "
                 + " ORDER BY s.id")) {
            while (rs.next()) {
                System.out.println("setting#" + rs.getLong(1) + " | el=" + rs.getLong(2) + "/" + rs.getString(3)
                    + " | user=" + rs.getLong(4) + " | perPage=" + rs.getObject(5) + " | showTitle=" + rs.getObject(6)
                    + " | deleted=" + rs.getInt(7));
            }
        }
    }
}

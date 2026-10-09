/* 简化 SQL 执行器：按分号(不在引号/注释内)拆分并执行，用于本地 dev 库执行 doc/sql 下的脚本。
 * 用法（JDK 21 单文件源码模式）：
 *   java -cp "<mysql-connector-j-9.7.0.jar 绝对路径>" RunSql.java <sqlFilePath> [jdbcUrl] [user] [password]
 * 默认：jdbc:mysql://localhost:3306/blade  root/123456
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RunSql {
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("USAGE: java -cp <mysql-connector-j.jar> RunSql.java <sqlFilePath> [jdbcUrl] [user] [password]");
			return;
		}
		String file = args[0];
		String url = args.length > 1 ? args[1] : "jdbc:mysql://localhost:3306/blade?useSSL=false&allowMultiQueries=true&serverTimezone=GMT%2B8&characterEncoding=utf-8";
		String user = args.length > 2 ? args[2] : "root";
		String pwd = args.length > 3 ? args[3] : "123456";
		String sql = Files.readString(Path.of(file), StandardCharsets.UTF_8);
		List<String> stmts = split(sql);
		int ok = 0;
		try (Connection conn = DriverManager.getConnection(url, user, pwd); Statement st = conn.createStatement()) {
			for (String s : stmts) {
				try {
					boolean hasRs = st.execute(s);
					ok++;
					if (hasRs) {
						printRs(st.getResultSet());
					} else {
						System.out.println("OK(" + st.getUpdateCount() + " rows) -> " + preview(s));
					}
				} catch (Exception e) {
					System.out.println("FAIL -> " + preview(s) + "  [" + e.getMessage() + "]");
				}
			}
		}
		System.out.println("EXECUTED " + ok + "/" + stmts.size());
	}

	/** SELECT 结果集紧凑打印（最多 50 行，列以 | 分隔）。 */
	private static void printRs(ResultSet rs) throws Exception {
		ResultSetMetaData md = rs.getMetaData();
		int n = md.getColumnCount();
		StringBuilder head = new StringBuilder();
		for (int i = 1; i <= n; i++) head.append(i == 1 ? "" : " | ").append(md.getColumnLabel(i));
		System.out.println("RS  -> " + head);
		int rows = 0;
		while (rs.next() && rows < 50) {
			StringBuilder row = new StringBuilder("     ");
			for (int i = 1; i <= n; i++) row.append(i == 1 ? "" : " | ").append(rs.getString(i));
			System.out.println(row);
			rows++;
		}
		if (rows == 50) System.out.println("     ...(截断，最多显示 50 行)");
		System.out.println("     [" + rows + " rows]");
	}

	private static String preview(String s) {
		String one = s.replaceAll("\\s+", " ").trim();
		return one.length() > 90 ? one.substring(0, 90) + "..." : one;
	}

	/** 按分号拆分，忽略 -- 行注释与引号内的分号 */
	private static List<String> split(String sql) {
		List<String> out = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inStr = false;
		char quote = 0;
		for (int i = 0; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (inStr) {
				sb.append(c);
				if (c == quote) inStr = false;
				continue;
			}
			if (c == '\'' || c == '"' || c == '`') { inStr = true; quote = c; sb.append(c); continue; }
			if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
				while (i < sql.length() && sql.charAt(i) != '\n') i++;
				sb.append('\n');
				continue;
			}
			if (c == ';') {
				if (sb.toString().trim().length() > 0) out.add(sb.toString().trim());
				sb.setLength(0);
				continue;
			}
			sb.append(c);
		}
		if (sb.toString().trim().length() > 0) out.add(sb.toString().trim());
		return out;
	}
}

package alexmog.apilib.config;

import java.util.HashMap;
import java.util.Map;

public class DatabasesConfig extends JsonConfiguration {
	public Map<String, Database> databases = new HashMap<>();
	
	public DatabasesConfig() {
		databases.put("default", new Database());
	}
	
	public static class Database {
		public String driver = "com.mysql.jdbc.Driver",
				url = "jdbc:mysql://server:3306/db",
				user = "user",
				password = "password";
	}
}

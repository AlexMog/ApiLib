package alexmog.apilib.managers;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.Properties;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import alexmog.apilib.Server;
import alexmog.apilib.config.DatabasesConfig;
import alexmog.apilib.dao.DAO;
import alexmog.apilib.dao.DataSourceThreadLocal;

@alexmog.apilib.managers.Managers.Manager
public class DaoManager extends Manager {
	private Map<String, DataSourceThreadLocal> mDataSources = new HashMap<>();
	private Map<Class<?>, DAO> mDaos = new HashMap<>();

	@Override
	public void shutdown() {
		for (DataSourceThreadLocal ds : mDataSources.values()) ds.close();
	}
	
	public void releaseConnectionsForThisThread() {
		for (DataSourceThreadLocal dataSource : mDataSources.values()) {
			try {
				dataSource.releaseConnection();
			} catch (SQLException e) {
				Server.LOGGER.log(Level.SEVERE, "Error while releasing connection", e);
			}
		}
	}
	
	private void initDaos() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Server.LOGGER.info("Searching for Daos using annotations...");
		Reflections reflections = new Reflections(".*");
		
		Set<Class<?>> classSet = reflections.getTypesAnnotatedWith(Dao.class);

		for (Class<?> c : classSet) {
			Server.LOGGER.info("Found Dao: " + c.getName() + "...");
			DataSourceThreadLocal dataSourceThreadLocal = mDataSources.get(c.getAnnotation(Dao.class).database());
			if (dataSourceThreadLocal == null) {
				Server.LOGGER.warning("DataSource not found '" + c.getAnnotation(Dao.class).database() + "' for DAO '" + c.getName() + "'");
				continue;
			}
			DAO dao = (DAO) c.getConstructor().newInstance();
			dao.setDataSource(dataSourceThreadLocal);
			mDaos.put(c, dao);
			Server.LOGGER.info("Dao added successfully.");
		}
		Server.LOGGER.info("Dao init done.");
	}
	
	private void injectDaos() throws IllegalAccessException, DaoNotFoundException {
		Server.LOGGER.info("Injecting Daos...");
		Reflections reflections = new Reflections(".*", new FieldAnnotationsScanner());
		Set<Field> fieldsSet = reflections.getFieldsAnnotatedWith(DaoInject.class);
		for (Field f : fieldsSet) {
			Server.LOGGER.info("Injecting field " + f + "...");
			if (!Modifier.isStatic(f.getModifiers())) throw new IllegalAccessException("Field '" + f + "' is not static.");
			DAO dao = mDaos.get(f.getType());
			if (dao == null) {
				if (!f.getAnnotation(DaoInject.class).needed()) continue;
				throw new DaoNotFoundException(f.getType().toGenericString());
			}
			f.setAccessible(true);
			f.set(null, dao);
		}
		Server.LOGGER.info("Injection done.");
	}
	
	private void initDatabases(Properties config, DatabasesConfig dbsConfig) throws Exception {
		File databaseConfigFiles = new File("databases");
        if (databaseConfigFiles.exists() && databaseConfigFiles.isDirectory()
            && databaseConfigFiles.listFiles().length > 0) {
        	Server.LOGGER.info("Databases enabled, initializing DataSources...");
            for (File f : databaseConfigFiles.listFiles((f) -> f.getName().endsWith(".properties"))) {
                String dbName = f.getName().substring(0, f.getName().lastIndexOf("."));

                Server.LOGGER.info("Adding dataSource '" + dbName + "'...");

                HikariConfig cfg = new HikariConfig(f.getAbsolutePath());
                cfg.addDataSourceProperty("allowMultiQueries", "true"); // TODO: Improve me
                HikariDataSource dataSource = new HikariDataSource(cfg);
                Server.LOGGER.info("Testing database '" + dbName + "' connection...");
                dataSource.getConnection().close();
                Server.LOGGER.info("Done.");
    			Server.LOGGER.info("Applying DB migrations... (migration files location: \"classpath:db/" + dbName + "/sql\"");
    			int retrys = 3;
    			do {
    				try {
    					Flyway flyway = Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).locations("classpath:db/" + dbName + "/sql").load();
    					flyway.migrate();
    					break;
    				} catch (Exception e) {
    					if (retrys <= 0) throw e;
    					retrys--;
    					Thread.sleep(500);
    				}
    			} while (true);
    			Server.LOGGER.info("Done.");
                mDataSources.put(dbName, new DataSourceThreadLocal(dataSource));
            }
        } else
            Server.LOGGER.warning("No databases config set. This can be problematic.");
	}

	@Override
	public void init(Properties config, DatabasesConfig databasesConfig) throws Exception {
		initDatabases(config, databasesConfig);
		initDaos();
		injectDaos();
	}
	
	/**
	 * This Annotation is used to register a new DAO to the DaoManager
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface Dao {
		String database();
	}
	
	/**
	 * Define wich static variables are used to inject the Daos
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public static @interface DaoInject {
		boolean needed() default true;
	}

	@SuppressWarnings("serial")
	public class DaoNotFoundException extends Exception {
		public DaoNotFoundException(String dao) {
			super("Dao not found: " + dao);
		}
	}
}

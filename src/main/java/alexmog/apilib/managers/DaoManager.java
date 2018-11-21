package alexmog.apilib.managers;

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

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import com.jolbox.bonecp.BoneCPDataSource;

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
		Reflections reflections = new Reflections("com.unexpectedstudio.*");
		
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
		Reflections reflections = new Reflections("com.unexpectedstudio.*", new FieldAnnotationsScanner());
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
		for (Entry<String, DatabasesConfig.Database> entry : dbsConfig.databases.entrySet()) {
			Server.LOGGER.info("Adding dataSource '" + entry.getKey() + "'...");
			DatabasesConfig.Database db = entry.getValue();
			BoneCPDataSource dataSource = new BoneCPDataSource();
			dataSource.setDriverClass(db.driver);
			dataSource.setJdbcUrl(db.url);
			dataSource.setUsername(db.user);
			dataSource.setPassword(db.password);
			dataSource.setIdleConnectionTestPeriodInMinutes(Integer.parseInt(config.getProperty("bonecp.idleConnectionTestPeriodInMinutes", "1")));
			dataSource.setIdleMaxAgeInMinutes(Integer.parseInt(config.getProperty("bonecp.idleMaxAgeInMinutes", "4")));
			dataSource.setMaxConnectionsPerPartition(Integer.parseInt(config.getProperty("bonecp.maxConnectionsPerPartition", "60")));
			dataSource.setMinConnectionsPerPartition(Integer.parseInt(config.getProperty("bonecp.minConnectionsPerPartition", "1")));
			dataSource.setPoolAvailabilityThreshold(Integer.parseInt(config.getProperty("bonecp.poolAvailabilityThreshold", "10")));
			dataSource.setPartitionCount(Integer.parseInt(config.getProperty("bonecp.partitionCount", "4")));
			dataSource.setAcquireIncrement(Integer.parseInt(config.getProperty("bonecp.acquireIncrement", "5")));
			dataSource.setStatementsCacheSize(Integer.parseInt(config.getProperty("bonecp.statementsCacheSize", "50")));
			dataSource.setConnectionTestStatement(config.getProperty("bonecp.connectionTestStatement", "SELECT 1"));
			dataSource.setLazyInit(Boolean.parseBoolean(config.getProperty("bonecp.lazyInit", "true")));

			Server.LOGGER.info("Testing database '" + entry.getKey() + "' connection...");
			dataSource.getConnection().close();
			Server.LOGGER.info("Done.");
			mDataSources.put(entry.getKey(), new DataSourceThreadLocal(dataSource));
		}
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

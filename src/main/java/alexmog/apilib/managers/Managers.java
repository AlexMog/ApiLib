package alexmog.apilib.managers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;

import alexmog.apilib.ApiServer;
import alexmog.apilib.config.DatabasesConfig;

public class Managers extends Manager {
	private Map<Class<?>, alexmog.apilib.managers.Manager> mManagers = new HashMap<>();

	@Override
	public void shutdown() {
		for (alexmog.apilib.managers.Manager m : mManagers.values()) m.shutdown();
	}
	
	public alexmog.apilib.managers.Manager getManager(int manager) {
		return mManagers.get(manager);
	}
	
	private void initManagers() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		ApiServer.LOGGER.info("Searching for Managers using annotations...");
		Reflections reflections = new Reflections(".*");
		
		Set<Class<?>> classSet = reflections.getTypesAnnotatedWith(Manager.class);

		for (Class<?> c : classSet) {
			ApiServer.LOGGER.info("Found Manager: " + c.getName() + "...");
			mManagers.put(c, (alexmog.apilib.managers.Manager) c.getConstructor().newInstance());
			ApiServer.LOGGER.info("Manager added successfully.");
		}
		ApiServer.LOGGER.info("Managers init done.");
	}
	
	private void injectManagers() throws IllegalAccessException, ManagerNotFoundException {
		ApiServer.LOGGER.info("Injecting Managers...");
		Reflections reflections = new Reflections(".*", new FieldAnnotationsScanner());
		Set<Field> fieldsSet = reflections.getFieldsAnnotatedWith(Manager.class);
		for (Field f : fieldsSet) {
			ApiServer.LOGGER.info("Injecting field " + f + "...");
			if (!Modifier.isStatic(f.getModifiers())) throw new IllegalAccessException("Field '" + f + "' is not static.");
			alexmog.apilib.managers.Manager manager = mManagers.get(f.getType());
			if (manager == null) throw new ManagerNotFoundException(f.getType().toGenericString());
			f.setAccessible(true);
			f.set(null, manager);
		}
		ApiServer.LOGGER.info("Injection done.");
	}

	@Override
	public void init(Properties config, DatabasesConfig databasesConfig) throws Exception {
		initManagers();
		ApiServer.LOGGER.info("Initializing managers...");
		for (alexmog.apilib.managers.Manager m : mManagers.values()) m.init(config, databasesConfig);
		ApiServer.LOGGER.info("Managers initialization done.");
		injectManagers();
	}

	/**
	 * This Annotation is used to register a new Manager to the Managers
	 * And define wich static variables are used to inject the Manager
	 *
	 */
	public static @interface Manager {}
	
	@SuppressWarnings("serial")
	public class ManagerNotFoundException extends Exception {
		public ManagerNotFoundException(String manager) {
			super("Manager not found: " + manager);
		}
	}
}

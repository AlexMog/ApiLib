package alexmog.apilib.managers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;

import alexmog.apilib.ApiServer;
import alexmog.apilib.config.DatabasesConfig;
import alexmog.apilib.managers.Managers.Manager;

@Manager
public class ServicesManager extends alexmog.apilib.managers.Manager {
	private final Map<Class<?>, alexmog.apilib.services.Service> mServices = new HashMap<>();
	private final List<Thread> mThreads = new ArrayList<>();
	private boolean mRunning = false;
	
	@Override
	public void shutdown() {
		stopServices(5000);
	}
	
	public boolean isRunning() {
		return mRunning;
	}
	
	public void stopServices(long timeout) {
		for (alexmog.apilib.services.Service s : mServices.values()) s.stop();
		for (Thread t : mThreads) {
			try {
				t.join(timeout);
				if (t.isAlive()) t.interrupt();
			} catch (InterruptedException e) {
				ApiServer.LOGGER.log(Level.WARNING, "Interruption exception", e);
			}
		}
		mRunning = false;
	}
	
	public void startServices() {
		mRunning = true;
		for (java.util.Map.Entry<Class<?>, alexmog.apilib.services.Service> e : mServices.entrySet()) {
			ApiServer.LOGGER.info("Starting service " + e.getKey().toString() + "...");
			Thread t = new Thread(e.getValue(), e.getKey().toString() + "-service-thread");
			mThreads.add(t);
			t.start();
		}
	}

	@Override
	public void init(Properties config, DatabasesConfig dbs) throws Exception {
		initServices();
		for (alexmog.apilib.services.Service s : mServices.values()) s.init(config);
		startServices();
		injectServices();
	}
	
	private void initServices() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		ApiServer.LOGGER.info("Searching for Services using annotations...");
		Reflections reflections = new Reflections(".*");
		
		Set<Class<?>> classSet = reflections.getTypesAnnotatedWith(Service.class);

		for (Class<?> c : classSet) {
			ApiServer.LOGGER.info("Found Service: " + c.getName() + "...");
			mServices.put(c, (alexmog.apilib.services.Service) c.getConstructor().newInstance());
			ApiServer.LOGGER.info("Service added successfully.");
		}
		ApiServer.LOGGER.info("Services init done.");
	}
	
	private void injectServices() throws IllegalAccessException, ServiceNotFoundException {
		ApiServer.LOGGER.info("Injecting Services...");
		Reflections reflections = new Reflections(".*", new FieldAnnotationsScanner());
		Set<Field> fieldsSet = reflections.getFieldsAnnotatedWith(Service.class);
		for (Field f : fieldsSet) {
			ApiServer.LOGGER.info("Injecting field " + f + "...");
			if (!Modifier.isStatic(f.getModifiers())) throw new IllegalAccessException("Field '" + f + "' is not static.");
			alexmog.apilib.services.Service service = mServices.get(f.getType());
			if (service == null) throw new ServiceNotFoundException(f.getType().toGenericString());
			f.setAccessible(true);
			f.set(null, service);
		}
		ApiServer.LOGGER.info("Injection done.");
	}

	/**
	 * This Annotation is used to register a new Service to the ServicesManager
	 * And define wich static variables are used to inject the Service
	 *
	 */
	public static @interface Service {}
	
	@SuppressWarnings("serial")
	public class ServiceNotFoundException extends Exception {
		public ServiceNotFoundException(String service) {
			super("Service not found: " + service);
		}
	}
}

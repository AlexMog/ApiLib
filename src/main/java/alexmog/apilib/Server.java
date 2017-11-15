package alexmog.apilib;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.restlet.engine.Engine;

import alexmog.apilib.config.DatabasesConfig;
import alexmog.apilib.config.JsonConfiguration;
import alexmog.apilib.managers.Managers;

public class Server {
	public static final Logger LOGGER = Engine.getLogger(Server.class);
	protected final Managers mManagers = new Managers();
	protected final Properties mConfig = new Properties();
	protected DatabasesConfig mDatabasesConfig = new DatabasesConfig();

	private void configureShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				LOGGER.info("Shutdown triggered...");
				mManagers.shutdown();
				LOGGER.info("Shutdown done.");
			}
		});
	}
	
	public Properties getConfig() {
		return mConfig;
	}
	
	public Managers getManagers() {
		return mManagers;
	}
	
	public void start() throws Exception {
		configureShutdownHook();
		
		mConfig.load(new FileInputStream("configs.properties"));

		File f = new File("databases.json");
		if (!f.exists()) {
			f.createNewFile();
			mDatabasesConfig.save(f);
		}
		
		mDatabasesConfig = JsonConfiguration.load(f, DatabasesConfig.class);
		
		mManagers.init(mConfig, mDatabasesConfig);
	}
}

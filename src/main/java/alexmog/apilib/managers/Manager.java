package alexmog.apilib.managers;

import java.util.Properties;

import alexmog.apilib.config.DatabasesConfig;

public abstract class Manager {
	public abstract void shutdown();
	public abstract void init(Properties config, DatabasesConfig databasesConfig) throws Exception;
}

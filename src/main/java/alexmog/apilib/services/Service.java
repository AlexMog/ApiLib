package alexmog.apilib.services;

import java.util.Properties;

public interface Service extends Runnable {
	public void stop();
	public void init(Properties config) throws Exception;
}

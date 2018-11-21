package alexmog.apilib;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Protocol;
import alexmog.apilib.api.ApiBase;
import alexmog.apilib.config.DatabasesConfig;
import alexmog.apilib.managers.DaoManager;
import alexmog.apilib.managers.Managers.Manager;
import lombok.Data;

public class ApiServer extends Server {
	@Manager
	private static DaoManager mDaoManager;
	
	public DatabasesConfig getDatabasesConfig() {
		return mDatabasesConfig;
	}
	
	public Component start(EndpointBuilder[] endpoints, ApiEndpointBuilder... apiEndpoints) throws Exception {
		start();
		
		Component component = new Component() {
			@Override
			public void handle(Request request, Response response) {
				
				try {
					super.handle(request, response);
				} finally {
					mDaoManager.releaseConnectionsForThisThread();
				}
			}
		};
		
		String maxThreads = mConfig.getProperty("webserver.maxThreads");
		org.restlet.Server server;
		
		if (mConfig.containsKey("webserver.https.port")) {
			server = component.getServers().add(Protocol.HTTPS, Integer.parseInt(mConfig.getProperty("webserver.https.port")));
			if (maxThreads != null) {
				server.getContext().getParameters().add("threadPool.maxThreads", maxThreads);
				server.getContext().getParameters().add("maxThreads", maxThreads);
			}
		}
		
		if (mConfig.containsKey("webserver.http.port")) server = component.getServers().add(Protocol.HTTP, Integer.parseInt(mConfig.getProperty("webserver.http.port")));
		else server = component.getServers().add(Protocol.HTTP);
		if (maxThreads != null) {
			server.getContext().getParameters().add("threadPool.maxThreads", maxThreads);
			server.getContext().getParameters().add("maxThreads", maxThreads);
		}
		
		if (apiEndpoints != null) {
			for (ApiEndpointBuilder endpoint : apiEndpoints) {
				endpoint.getApi().init(mConfig);
				component.getDefaultHost().attach(endpoint.getEndpoint(), endpoint.getApi());
			}
		}
		
		if (endpoints != null) {
			for (EndpointBuilder endpoint : endpoints) {
				component.getDefaultHost().attach(endpoint.getEndpoint(), endpoint.getApp());
			}
		}
		
		component.start();

		LOGGER.info("API Started.");
		return component;
	}
	
	public @Data static class EndpointBuilder {
		private final String endpoint;
		private final Application app;
	}
	
	public @Data static class ApiEndpointBuilder {
		private final String endpoint;
		private final ApiBase api;
	}
}

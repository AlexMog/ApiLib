package alexmog.apilib.api;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;

import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.engine.application.CorsFilter;
import org.restlet.ext.swagger.SwaggerApplication;
import org.restlet.routing.Router;
import org.restlet.security.Authorizer;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;
import org.restlet.service.CorsService;

import alexmog.apilib.ApiServer;

public abstract class ApiBase extends SwaggerApplication {
	private Verifier mVerifier;
	private Class<? extends Authorizer> mAuthorizer;
	private Authorizer mPublicAuthorizer;
	private int[] mGroups;
	private Router mPubRouter;
	
	public ApiBase(String apiName, String apiDescription, Verifier verifier, Class<? extends Authorizer> authorizer, Authorizer publicAuthorizer, int... groups) {
		setName(apiName);
		setDescription(apiDescription);
		setStatusService(new ApiStatusService());
		mVerifier = verifier;
		mGroups = groups;
		mAuthorizer = authorizer;
		mPublicAuthorizer = publicAuthorizer;
	}
	
	private Router generatePublicRouter() {
		Router publicRouter = new Router(getContext());
		
		if (mPublicAuthorizer != null) {
			Router router = new Router(getContext());
			configurePublicRouter(router);
			mPublicAuthorizer.setNext(router);
			publicRouter.attach(mPublicAuthorizer);
			mPubRouter = router;
			return publicRouter;
		}
		
		configurePublicRouter(publicRouter);
		
		return publicRouter;
	}
	
	public abstract void init(Properties config);
	
	protected abstract void configurePublicRouter(Router router);
	
	private ChallengeAuthenticator generateOAuthGuard() throws URISyntaxException {
		ChallengeAuthenticator guard = new ChallengeAuthenticator(getContext(),
				ChallengeScheme.HTTP_BASIC, getName());
		guard.setVerifier(mVerifier);
		return guard;
	}
	
	protected abstract void configureRouter(int group, Router router);
	
	protected abstract void configureAuthenticatedRouter(Router router);

	private Router generateAuthenticatedRouter() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Router authenticatedRouter = new Router(getContext());
    	
		if (mGroups == null || mGroups.length < 0) return authenticatedRouter;

    	Router lastRouter = authenticatedRouter;
		for (int g : mGroups) {
			ApiServer.LOGGER.info("Generating group:" + g + " router...");
			Router router = new Router(getContext());
			configureRouter(g, router);
			Authorizer authorizer = mAuthorizer.getConstructor(int.class).newInstance(g);
			authorizer.setNext(router);
			lastRouter.attach(authorizer);
			lastRouter = router;
		}
		return authenticatedRouter;
	}
	
	@Override
	public final Restlet createInboundRoot() {
		Router publicRouter = generatePublicRouter();
		
        CorsService corsService = new CorsService();
		corsService.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
		corsService.setAllowingAllRequestedHeaders(true);
		corsService.setAllowedCredentials(true);
		getServices().add(corsService);
        
        if (mVerifier != null) {
	        ChallengeAuthenticator guard = null;
			try {
				guard = generateOAuthGuard();
			} catch (URISyntaxException e) {
				ApiServer.LOGGER.log(Level.SEVERE, "Initialisation error.", e);
				System.exit(1);
			}
	        try {
	        	Router r = generateAuthenticatedRouter();
        		configureAuthenticatedRouter(r);
        		guard.setNext(r);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				ApiServer.LOGGER.log(Level.SEVERE, "Cannot use authorizer.", e);
				System.exit(1);
			}
	        mPubRouter.attachDefault(guard);
        }
		return publicRouter;
	}
}

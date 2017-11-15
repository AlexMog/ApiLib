package alexmog.apilib.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class OAuthChecker {
	private final String mOAuthAuthorizationValue;
	private final String mAuthorizationServerUrl;
	private final PoolingHttpClientConnectionManager mClientsManager = new PoolingHttpClientConnectionManager();
	private final CloseableHttpClient mHttpClient = HttpClients.custom().setConnectionManager(mClientsManager).setConnectionManagerShared(true).build();

	public OAuthChecker(Properties config) {
		mOAuthAuthorizationValue = "Basic ".concat(new String(
				Base64.encodeBase64(
						config.getProperty("authorizationservice.key").concat(":")
							.concat(config.getProperty("authorizationservice.secret")).getBytes()
				)
			)
		);
		mAuthorizationServerUrl = config.getProperty("authorizationservice.url");
		mClientsManager.setMaxTotal(Integer.parseInt(config.getProperty("httpclient.maxTotal", "512")));
		mClientsManager.setDefaultMaxPerRoute(Integer.parseInt(config.getProperty("httpclient.defaultMaxPerRoute", "512")));
	}
	
	public String check(String accessToken) throws URISyntaxException, ClientProtocolException, IOException {
		String json = null;
		HttpGet httpGet = new HttpGet(new URI(String.format(mAuthorizationServerUrl.concat("?access_token=%s"), accessToken)));
		httpGet.addHeader(HttpHeaders.AUTHORIZATION, mOAuthAuthorizationValue);
		
		try (CloseableHttpResponse resp = mHttpClient.execute(httpGet, HttpClientContext.create());) {
		
			if (resp.getStatusLine().getStatusCode() != 200) return null;
			
			InputStream stream = resp.getEntity().getContent();
			json = IOUtils.toString(stream);
		}
		
		httpGet.releaseConnection();

		return json;
	}
}

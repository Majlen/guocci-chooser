package cz.cesnet.cloud.sources;

import java.net.URI;
import java.util.Arrays;
import java.util.Properties;

public class Configuration {
	private final static String RESOURCE_PROPERTY = "guocci.chooser.resource.type";
	private final static String RESOURCE_URI_PROPERTY = "guocci.resource.uri";
	private final static String AUTH_CA_PATH_PROPERTY = "guocci.occi.x509.capath";
	private final static String CACHE_REFRESH_PROPERTY = "guocci.chooser.cache.refresh";
	private final static String CACHE_RESILIENCE_PROPERTY = "guocci.chooser.cache.resilience";
	private final static String MAIN_GUOCCI_URI = "guocci.chooser.main.uri";

	private Properties properties;

	public Configuration(Properties properties) {
		this.properties = properties;
	}

	public String getResource() {
		return properties.getProperty(RESOURCE_PROPERTY);
	}

	public URI[] getSourceURI() {
		String[] strArray = properties.getProperty(RESOURCE_URI_PROPERTY).split(",");

		return Arrays.stream(strArray)
				.map(URI::create)
				.toArray(URI[]::new);
	}

	public String getAuthCAPath() {
		return properties.getProperty(AUTH_CA_PATH_PROPERTY);
	}

	public int getCacheRefresh() {
		return Integer.parseInt(properties.getProperty(CACHE_REFRESH_PROPERTY));
	}

	public int getCacheResilience() {
		return Integer.parseInt(properties.getProperty(CACHE_RESILIENCE_PROPERTY));
	}

	public String getGuocciURI() {
		return properties.getProperty(MAIN_GUOCCI_URI);
	}
}

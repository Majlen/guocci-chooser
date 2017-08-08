package cz.cesnet.cloud.sources;

import java.net.URI;
import java.util.Properties;

public class Configuration {
	private final static String RESOURCE_PROPERTY = "guocci.chooser.resource.type";
	private final static String RESOURCE_URI_PROPERTY = "guocci.chooser.resource.uri";
	private final static String AUTH_CA_PATH_PROPERTY = "guocci.chooser.occi.x509.capath";

	private Properties properties;

	public Configuration(Properties properties) {
		this.properties = properties;
	}

	public String getResource() {
		return properties.getProperty(RESOURCE_PROPERTY);
	}

	public URI getSourceURI() {
		return URI.create(properties.getProperty(RESOURCE_URI_PROPERTY));
	}

	public String getAuthCAPath() {
		return properties.getProperty(AUTH_CA_PATH_PROPERTY);
	}
}

package cz.cesnet.cloud.sources.occi;

import cz.cesnet.cloud.occi.api.Client;
import cz.cesnet.cloud.occi.api.exception.CommunicationException;
import cz.cesnet.cloud.occi.api.http.HTTPClient;
import cz.cesnet.cloud.occi.api.http.auth.HTTPAuthentication;
import cz.cesnet.cloud.occi.api.http.auth.VOMSAuthentication;
import cz.cesnet.cloud.occi.core.Mixin;
import cz.cesnet.cloud.sources.*;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.configuration.Cache2kConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OCCI implements ResourceAdapter {
	private static final Logger logger = LoggerFactory.getLogger(OCCI.class);
	public static String certPath = "/tmp/x509up_u1000";

	private static OCCI occi;
	private Configuration configuration;

	private Cache<String, Model> cache;

	public static OCCI getOCCI(Configuration configuration) throws CommunicationException {
		if (occi == null) {
			occi = new OCCI(configuration);
		}
		return occi;
	}

	private OCCI(Configuration configuration) throws CommunicationException {
		this.configuration = configuration;
		cache = Cache2kBuilder.of(new Cache2kConfiguration<String, Model>())
				.expireAfterWrite(configuration.getCacheRefresh(), TimeUnit.SECONDS)
				.resilienceDuration(configuration.getCacheResilience(), TimeUnit.SECONDS)
				.loader(this::refresh)
				.build();
	}

	private Model refresh(String cacheKey) throws CommunicationException {
		logger.debug("Refreshing OCCI models");
		URI[] endpoints = configuration.getSourceURI();

		List<Service> services = new LinkedList<>();
		List<Image> images = new LinkedList<>();

		for (URI endpoint : endpoints) {
			logger.debug("Refreshing OCCI model for endpoint {}", endpoint);
			HTTPAuthentication auth = new VOMSAuthentication(cacheKey);
			auth.setCAPath(configuration.getAuthCAPath());

			Client client = new HTTPClient(endpoint, auth);

			cz.cesnet.cloud.occi.Model occiModel = client.getModel();
			Service service = new Service();
			service.setEndpoint(endpoint);
			service.setName(endpoint.getHost());

			List<Flavour> flavours = new LinkedList<>();
			List<Image> serviceImages = new LinkedList<>();

			for (Mixin m : occiModel.getMixins()) {
				for (Mixin n : m.getRelations()) {
					if (n.getTerm().equals("resource_tpl")) {
						Flavour f = new Flavour();
						try {
							f.setName(m.getTerm());
							f.setId(URI.create(m.getScheme() + m.getTerm()));
							f.setTitle(m.getTitle());
						} catch (Exception e) {
							logger.error("Exception parsing flavour.", e);
						}
						flavours.add(f);
					} else if (n.getTerm().equals("os_tpl")) {
						Image img = new Image();
						img.setName(m.getTitle());
						img.setId(URI.create(m.getScheme() + m.getTerm()));
						img.setKey(m.getScheme() + m.getTerm());
						img.setService(service);
						serviceImages.add(img);
					}
				}
			}

			service.setAppliances(serviceImages);
			service.setFlavours(flavours);

			services.add(service);
			images.addAll(serviceImages);
		}

		return new Model(services, images);
	}

	public Model getModel(String cacheKey) {
		return cache.get(cacheKey);
	}

}

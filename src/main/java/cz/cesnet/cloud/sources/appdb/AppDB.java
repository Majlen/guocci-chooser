package cz.cesnet.cloud.sources.appdb;

import cz.cesnet.cloud.sources.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.configuration.Cache2kConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AppDB implements ResourceAdapter {
	final static private String APPDB_NS = "http://appdb.egi.eu/api/1.0/appdb";
	final static private String APPDB_SITE_NS = "http://appdb.egi.eu/api/1.0/site";
	final static private String APPDB_REGIONAL_NS = "http://appdb.egi.eu/api/1.0/regional";
	final static private String APPDB_PROVIDER_NS = "http://appdb.egi.eu/api/1.0/provider";
	final static private String APPDB_PROVIDER_TEMPLATE_NS = "http://appdb.egi.eu/api/1.0/provider_template";
	final static private String APPDB_APPLICATION_NS = "http://appdb.egi.eu/api/1.0/application";
	final static private String APPDB_VO_NS = "http://appdb.egi.eu/api/1.0/vo";

	private static final Logger logger = LoggerFactory.getLogger(AppDB.class);

	private static AppDB appDB = null;

	//private Model model;
	private Map<String, List<Image>> images;
	private List<VO> vos;
	private URI endpoint;
	private Cache<Integer, Model> cache;

	public static AppDB getAppDB(Configuration configuration) {
		if (appDB == null) {
			appDB = new AppDB(configuration);
		}
		return appDB;
	}

	private AppDB(Configuration configuration) {
		this(configuration.getSourceURI());
		cache = Cache2kBuilder.of(new Cache2kConfiguration<Integer, Model>())
				.expireAfterWrite(configuration.getCacheRefresh(), TimeUnit.SECONDS)
				.resilienceDuration(configuration.getCacheResilience(), TimeUnit.SECONDS)
				.refreshAhead(true)
				.loader(this::refreshXML)
				.build();
	}

	private AppDB(URI endpoint) {
		this.endpoint = endpoint;
	}

	public Model getModel() {
		return cache.get(0);
	}

	private Model refreshXML(int cacheKey) throws IOException, ParserConfigurationException, SAXException {
		//cacheKey is unused, all data are global for all users (AppDB is not authenticated
		HttpClient http = HttpClients.createDefault();
		HttpGet get = new HttpGet(this.endpoint);

		HttpResponse response = http.execute(get);
		logger.debug(response.getStatusLine().toString());
		HttpEntity entity = response.getEntity();

		return parseXML(entity.getContent());
	}

	private Model parseXML(InputStream xmlStream) throws IOException, ParserConfigurationException, SAXException {
		images = new HashMap<>();
		vos = new LinkedList<>();

		logger.debug("Started XML parsing");
		Reader r = new InputStreamReader(xmlStream, Charset.defaultCharset());
		InputSource s = new InputSource(r);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);

		Document xml = factory.newDocumentBuilder().parse(s);
		xml.getDocumentElement().normalize();

		NodeList sites = xml.getElementsByTagNameNS(APPDB_SITE_NS, "service");

		List<Service> services = new LinkedList<>();

		for (int i = 0; i < sites.getLength(); i++) {
			Element e = (Element) sites.item(i);
			if (e.getAttribute("type").equals("occi") && e.getAttribute("in_production").equals("true")) {
				try {
					services.add(parseService(e));
				} catch (Exception ex) {
					logger.warn("Cannot parse service, reason.", ex);
				}
			}
		}

		logger.debug("Finished XML parsing");
		return new Model(services, images, vos);
	}

	private Service parseService(Element service) {
		Element site = (Element) service.getParentNode();
		logger.debug("Parsing service {}", site.getAttribute("name"));
		Service s = new Service();
		s.setName(site.getAttribute("name"));
		s.setCountry(((Element) (site.getElementsByTagNameNS(APPDB_REGIONAL_NS, "country").item(0))).getAttribute("isocode"));
		s.setEndpoint(URI.create(service.getElementsByTagNameNS(APPDB_SITE_NS, "occi_endpoint_url").item(0).getTextContent()));
		s.setSite_id(site.getAttribute("id"));
		s.setService_id(service.getAttribute("id"));


		NodeList flavour_nodes = service.getElementsByTagNameNS(APPDB_PROVIDER_NS, "template");
		List<Flavour> flavours = new LinkedList<>();

		for (int i = 0; i < flavour_nodes.getLength(); i++) {
			flavours.add(parseFlavour((Element) flavour_nodes.item(i)));
		}

		NodeList image_nodes = service.getElementsByTagNameNS(APPDB_SITE_NS, "occi");
		List<Image> appdbImages = new LinkedList<>();

		for (int i = 0; i < image_nodes.getLength(); i++) {
			Image img = parseImage((Element) image_nodes.item(i));
			if (img != null) {
				appdbImages.add(img);
			}
		}

		appdbImages.forEach(image -> {
			image.setService(s);

			if (!images.containsKey(image.getAppDBIdentifier())) {
				images.put(image.getAppDBIdentifier(), new LinkedList<>());
			}

			List<Image> list = images.get(image.getAppDBIdentifier());
			list.add(image);
		});

		s.setFlavours(flavours);
		s.setAppliances(appdbImages);

		return s;
	}

	private Flavour parseFlavour(Element template) {
		logger.debug("Parsing flavour {}", template.getElementsByTagNameNS(APPDB_PROVIDER_TEMPLATE_NS, "resource_name").item(0).getTextContent());
		Flavour f = new Flavour();
		f.setId(template.getElementsByTagNameNS(APPDB_PROVIDER_TEMPLATE_NS, "resource_id").item(0).getTextContent());
		f.setName(URI.create(template.getElementsByTagNameNS(APPDB_PROVIDER_TEMPLATE_NS, "resource_name").item(0).getTextContent()));
		f.setMemory(Integer.parseInt(template.getElementsByTagNameNS(APPDB_PROVIDER_TEMPLATE_NS, "main_memory_size").item(0).getTextContent()));
		f.setVcpu(Integer.parseInt(template.getElementsByTagNameNS(APPDB_PROVIDER_TEMPLATE_NS, "logical_cpus").item(0).getTextContent()));
		f.setCpu(Integer.parseInt(template.getElementsByTagNameNS(APPDB_PROVIDER_TEMPLATE_NS, "physical_cpus").item(0).getTextContent()));

		return f;
	}

	private Image parseImage(Element image) {
		Element parent = (Element) image.getParentNode();
		if (parent.getAttribute("isexpired").equals("true")) {
			return null;
		}
		logger.debug("Parsing image {}", parent.getElementsByTagNameNS(APPDB_APPLICATION_NS, "name").item(0).getTextContent());

		Image i = new Image();
		Element vo = (Element) image.getElementsByTagNameNS(APPDB_VO_NS, "vo").item(0);
		i.setId(URI.create(image.getAttribute("id")));

		VO org = parseVO(vo);
		i.setVo(org);
		org.addImage(i);

		i.setName(parent.getElementsByTagNameNS(APPDB_APPLICATION_NS, "name").item(0).getTextContent());
		i.setAppDBIdentifier(parent.getAttribute("identifier"));
		i.setAppDBID(Integer.parseInt(parent.getAttribute("id")));

		return i;
	}

	private VO parseVO(Element e) {
		logger.debug("Parsing VO {}", e == null ? "null" : e.getAttribute("name"));
		VO vo = new VO();
		if (e != null) {
			vo.setId(Integer.parseInt(e.getAttribute("id")));
			if (vos.contains(vo)) {
				return vos.get(vos.indexOf(vo));
			} else {
				vo.setName(e.getAttribute("name"));
				vos.add(vo);
				return vo;
			}
		} else {
			vo.setId(-1);
			if (vos.contains(vo)) {
				return vos.get(vos.indexOf(vo));
			} else {
				vo.setName("NO VO");
				vos.add(vo);
				return vo;
			}
		}
	}
}

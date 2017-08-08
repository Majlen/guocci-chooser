package cz.cesnet.cloud.sources.occi;

import cz.cesnet.cloud.occi.api.Client;
import cz.cesnet.cloud.occi.api.exception.CommunicationException;
import cz.cesnet.cloud.occi.api.http.HTTPClient;
import cz.cesnet.cloud.occi.api.http.auth.HTTPAuthentication;
import cz.cesnet.cloud.occi.api.http.auth.VOMSAuthentication;
import cz.cesnet.cloud.occi.core.Action;
import cz.cesnet.cloud.occi.core.Kind;
import cz.cesnet.cloud.occi.core.Mixin;
import cz.cesnet.cloud.sources.*;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class OCCI implements ResourceAdapter {
	private HTTPAuthentication auth;
	private Model model;
	private URI endpoint;

	public OCCI() throws CommunicationException {
		this("/tmp/x509up_u1000", URI.create("https://carach5.ics.muni.cz:11443"));
	}

	public OCCI(String certificate, URI endpoint) throws CommunicationException {
		this.endpoint = endpoint;
		auth = new VOMSAuthentication(certificate);
		auth.setCAPath("/etc/grid-security/certificates");
		Client client = new HTTPClient(endpoint, auth);

		cz.cesnet.cloud.occi.Model occiModel = client.getModel();
		Service service = new Service();
		service.setEndpoint(this.endpoint);
		service.setName(endpoint.getHost());

		List<Flavour> flavours = new LinkedList<>();
		List<Image> images = new LinkedList<>();

		for (Mixin m: occiModel.getMixins()) {
			for (Mixin n : m.getRelations()) {
				if (n.getTerm().equals("resource_tpl")) {
					Flavour f = new Flavour();
					try {
						f.setId(m.getTerm());
						f.setName(m.getLocation());
					} catch (Exception e) {
						System.out.println("Exception parsing flavour: " + e);
					}
					flavours.add(f);
				} else if (n.getTerm().equals("os_tpl")) {
					Image i = new Image();
					i.setName(m.getTitle());
					i.setId(m.getLocation());
					images.add(i);
				}
			}
		}

		service.setAppliances(images);
		service.setFlavours(flavours);

		model = new Model(service, images);
	}

	public Model getModel() {
		return model;
	}

}

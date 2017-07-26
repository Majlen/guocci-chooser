package cz.cesnet.cloud;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.*;
import com.vaadin.util.ReflectTools;
import cz.cesnet.cloud.occi.api.exception.CommunicationException;
import cz.cesnet.cloud.sources.*;
import cz.cesnet.cloud.sources.Image;
import cz.cesnet.cloud.sources.appdb.AppDB;
import cz.cesnet.cloud.sources.occi.OCCI;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.*;

public class ChooseView extends HorizontalLayout implements View {
	private final CheckBoxGroup<VO> vos = new CheckBoxGroup<>();
	private LinkedList<VO> vosList = new LinkedList<>();
	private ListDataProvider<VO> vosProvider = new ListDataProvider<>(vosList);

	private final CheckBoxGroup<Service> services = new CheckBoxGroup<>();
	private LinkedList<Service> servicesList = new LinkedList<>();
	private ListDataProvider<Service> servicesProvider = new ListDataProvider<>(servicesList);

	private final CheckBoxGroup<Image> images = new CheckBoxGroup<>();
	private Map<String, List<Image>> imagesMap;
	private LinkedList<Image> imagesList = new LinkedList<>();
	private LinkedList<Image> imagesFullList = new LinkedList<>();
	private ListDataProvider<Image> imagesProvider = new ListDataProvider<>(imagesList);

	private final CheckBoxGroup<Flavour> flavours = new CheckBoxGroup<>();
	private LinkedList<Flavour> flavoursList = new LinkedList<>();
	private ListDataProvider<Flavour> flavoursProvider = new ListDataProvider<>(flavoursList);

	private ResourceAdapter res;

	public ChooseView() {
		res = null;
		try {
			if (true) {
				res = new AppDB();
			} else {
				res = new OCCI();
			}
		} catch (SAXException e) {
			System.out.println("Error in XML parsing: " + e);
			return;
		} catch (ParserConfigurationException e) {
			System.out.println("Error configuring XML parser: " + e);
			return;
		} catch (IOException e) {
			System.out.println("Error reading AppDB: " + e);
			return;
		} catch (CommunicationException e) {
			System.out.println("Error reading OCCI: " + e);
		}


		vos.setDataProvider(vosProvider);
		services.setDataProvider(servicesProvider);
		images.setDataProvider(imagesProvider);
		flavours.setDataProvider(flavoursProvider);

		vos.addValueChangeListener(valueChangeEvent -> filtering());
		services.addValueChangeListener(valueChangeEvent -> filtering());
		images.addValueChangeListener(valueChangeEvent -> filtering());

		services.addValueChangeListener(valueChangeEvent -> {
			System.out.println(valueChangeEvent.getValue().size());
			if (valueChangeEvent.getValue().size() == 1) {
				flavoursList.addAll(valueChangeEvent.getValue().iterator().next().getFlavours());
				flavoursProvider.refreshAll();
			} else {
				flavours.deselectAll();
				flavoursList.clear();
				flavoursProvider.refreshAll();
			}
		});

		vos.addValueChangeListener(valueChangeEvent -> fireEvent(new CompletedEvent(this)));
		services.addValueChangeListener(valueChangeEvent -> fireEvent(new CompletedEvent(this)));
		images.addValueChangeListener(valueChangeEvent -> fireEvent(new CompletedEvent(this)));
		flavours.addValueChangeListener(valueChangeEvent -> fireEvent(new CompletedEvent(this)));


		final Panel vosPanel = new Panel("Virtual organizations", vos);
		final Panel servicesPanel = new Panel("Service sites", services);
		final Panel imagesPanel = new Panel("Image templates", images);
		final Panel flavoursPanel = new Panel("Flavours", flavours);

		if (res instanceof AppDB) {
			addComponent(vosPanel);
			addComponent(servicesPanel);
		}
		addComponent(imagesPanel);
		addComponent(flavoursPanel);
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent viewChangeEvent) {
		vosList.clear();
		servicesList.clear();
		imagesList.clear();
		imagesFullList.clear();
		flavoursList.clear();

		Model m = res.getModel();

		if (m.getVOs() != null) {
			vosList.addAll(m.getVOs());
		}
		if (m.getServices() != null) {
			servicesList.addAll(m.getServices());

			if (servicesList.size() == 1) {
				services.select(servicesList.get(0));
			}

			if (m.getImages() != null) {
				imagesMap = m.getImages();
				imagesMap.forEach((k,v) -> imagesList.add(v.get(0)));
				imagesMap.forEach((k,v) -> imagesFullList.addAll(v));
			}
		}
	}

	private void filtering() {
		Set<VO> vosSet = vos.getValue();
		Set<Service> servicesSet = services.getValue();
		Set<Image> imagesSet = images.getValue();

		if (vosSet.isEmpty() && servicesSet.isEmpty() && imagesSet.isEmpty()) {
			vosProvider.setFilter(null);
			servicesProvider.setFilter(null);
			imagesProvider.setFilter(null);
		} else {
			vosProvider.setFilter(vo -> {
				Collection<Service> servicesCollection;
				if (servicesSet.isEmpty()) {
					servicesCollection = servicesList;
				} else {
					servicesCollection = servicesSet;
				}

				for (Service s: servicesCollection) {
					if (!Collections.disjoint(s.getAppliances(), vo.getImages())) {
						if (imagesSet.isEmpty() || !Collections.disjoint(imagesSet, vo.getImages())) {
							return true;
						}
					}
				}

				return false;
			});
			servicesProvider.setFilter(service -> {
				if (vosSet.isEmpty()) {
					return imagesSet.isEmpty() || !Collections.disjoint(imagesSet, service.getAppliances());
				} else if (imagesSet.isEmpty() || !Collections.disjoint(imagesSet, service.getAppliances())) {
					for (Image i: service.getAppliances()) {
						if (vosSet.contains(i.getVo())) {
							return true;
						}
					}
				}

				return false;
			});
			imagesProvider.setFilter(image -> {
				if (servicesSet.isEmpty()) {
					if (vosSet.isEmpty()) {
						return true;
					} else {
						for (Image i: imagesFullList) {
							if (i.equals(image) && vosSet.contains(i.getVo())) {
								return true;
							}
						}
					}
				} else {
					for (Service s: servicesSet) {
						if (s.getAppliances().contains(image)) {
							if (vosSet.isEmpty()) {
								return true;
							} else {
								for (Image i: imagesFullList) {
									if (i.equals(image) && vosSet.contains(i.getVo())) {
										return true;
									}
								}
							}
						}
					}
				}

				return false;
			});
		}
	}

	public Registration addCompletedListener(CompletedListener listener) {
		return addListener(CompletedEvent.class, listener, CompletedListener.COMPLETED_METHOD);
	}

	public String getURLParams() {
		StringBuilder builder = new StringBuilder("#!create/");
		try {
			Service s = services.getValue().iterator().next();

			Image i = images.getValue().iterator().next();
			builder.append("image/");
			Image imageToSend = i;
			for (Image img: imagesMap.get(i.getAppDBIdentifier())) {
				if (img.getService() == s && img.getVo() == vos.getValue().iterator().next()) {
					imageToSend = img;
					break;
				}
			}
			builder.append(URLEncoder.encode(imageToSend.getId().toString(), "UTF-8"));
			builder.append("&");

			Flavour f = flavours.getValue().iterator().next();
			builder.append("flavour/");
			builder.append(URLEncoder.encode(f.getName().toString(), "UTF-8"));
			builder.append("&");
		} catch (UnsupportedEncodingException e) {
			//Should not happen, UTF-8 should be supported everywhere
		}

		builder.deleteCharAt(builder.lastIndexOf("&"));

		return builder.toString();
	}

	public static class CompletedEvent extends Component.Event {
		public CompletedEvent(Component source) {
			super(source);
		}

		public boolean getCompleted() {
			ChooseView source = (ChooseView) getSource();

			for (int i = 0; i < source.getComponentCount(); i++) {
				CheckBoxGroup boxGroup = (CheckBoxGroup) ((Panel) source.getComponent(i)).getContent();
				if (boxGroup.getValue().size() != 1) {
					return false;
				}
			}
			return true;
		}
	}

	@FunctionalInterface
	public interface CompletedListener extends Serializable {

		public static final Method COMPLETED_METHOD = ReflectTools.findMethod(CompletedListener.class, "completed", CompletedEvent.class);

		public void completed(CompletedEvent event);
	}
}

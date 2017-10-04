package cz.cesnet.cloud;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.Registration;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.ReflectTools;
import cz.cesnet.cloud.occi.api.exception.CommunicationException;
import cz.cesnet.cloud.sources.*;
import cz.cesnet.cloud.sources.Image;
import cz.cesnet.cloud.sources.appdb.AppDB;
import cz.cesnet.cloud.sources.occi.OCCI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.*;

public class ChooseView extends HorizontalLayout implements View {
	private static final Logger logger = LoggerFactory.getLogger(ChooseView.class);

	private final RadioButtonGroup<VO> vos = new RadioButtonGroup<>();
	private LinkedList<VO> vosList = new LinkedList<>();
	private ListDataProvider<VO> vosProvider = new ListDataProvider<>(vosList);

	private final RadioButtonGroup<Service> services = new RadioButtonGroup<>();
	private LinkedList<Service> servicesList = new LinkedList<>();
	private ListDataProvider<Service> servicesProvider = new ListDataProvider<>(servicesList);

	private final RadioButtonGroup<Image> images = new RadioButtonGroup<>();
	private Map<String, List<Image>> imagesMap;
	private LinkedList<Image> imagesList = new LinkedList<>();
	private LinkedList<Image> imagesFullList = new LinkedList<>();
	private ListDataProvider<Image> imagesProvider = new ListDataProvider<>(imagesList);

	private final RadioButtonGroup<Flavour> flavours = new RadioButtonGroup<>();
	private LinkedList<Flavour> flavoursList = new LinkedList<>();
	private ListDataProvider<Flavour> flavoursProvider = new ListDataProvider<>(flavoursList);

	private ResourceAdapter res;

	public ChooseView() {
		Configuration configuration = (Configuration) VaadinServlet.getCurrent().getServletContext().getAttribute("configuration");

		try {
			switch (configuration.getResource()) {
				case "AppDB":
					res = AppDB.getAppDB(configuration);
					break;
				case "OCCI":
					res = OCCI.getOCCI(configuration);
					break;
			}

			vos.setDataProvider(vosProvider);
			services.setDataProvider(servicesProvider);
			images.setDataProvider(imagesProvider);
			flavours.setDataProvider(flavoursProvider);

			vos.addValueChangeListener(valueChangeEvent -> filtering());
			services.addValueChangeListener(valueChangeEvent -> filtering());
			images.addValueChangeListener(valueChangeEvent -> filtering());

			services.addValueChangeListener(valueChangeEvent -> {
				flavours.setSelectedItem(null);
				flavoursList.clear();
				if (valueChangeEvent.getValue() != null) {
					flavoursList.addAll(valueChangeEvent.getValue().getFlavours());
				}
				flavoursProvider.refreshAll();
			});

			vos.addValueChangeListener(valueChangeEvent -> fireEvent(new CompletedEvent(this)));
			services.addValueChangeListener(valueChangeEvent -> fireEvent(new CompletedEvent(this)));
			images.addValueChangeListener(valueChangeEvent -> fireEvent(new CompletedEvent(this)));
			flavours.addValueChangeListener(valueChangeEvent -> fireEvent(new CompletedEvent(this)));

			Button nextButton = new Button("Continue", VaadinIcons.ANGLE_RIGHT);
			nextButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
			nextButton.setEnabled(false);

			nextButton.addClickListener(clickEvent -> Page.getCurrent().open(configuration.getGuocciURI() + getURLParams(), null));
			addCompletedListener(event -> nextButton.setEnabled(event.getCompleted()));

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
			addComponent(nextButton);
		} catch (CommunicationException e) {
			logger.error("Error reading OCCI.", e);
		}
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent viewChangeEvent) {
		vosList.clear();
		servicesList.clear();
		imagesList.clear();
		imagesFullList.clear();
		flavoursList.clear();

		String user = "";
		if (res instanceof OCCI) {
			user = OCCI.certPath;
		}
		Model m = res.getModel(user);

		if (m.getVOs() != null) {
			vosList.addAll(m.getVOs());
		}
		if (m.getServices() != null) {
			servicesList.addAll(m.getServices());

			if (servicesList.size() == 1) {
				services.setSelectedItem(servicesList.get(0));
			}

			if (m.getImages() != null) {
				imagesMap = m.getImages();
				imagesMap.forEach((k,v) -> imagesList.add(v.get(0)));
				imagesMap.forEach((k,v) -> imagesFullList.addAll(v));
			}
		}
	}

	private void filtering() {
		VO vo = vos.getValue();
		Service service = services.getValue();
		Image image = images.getValue();

		if (vo == null && service == null && image == null) {
			vosProvider.setFilter(null);
			servicesProvider.setFilter(null);
			imagesProvider.setFilter(null);
		} else {
			vosProvider.setFilter(selectedVO ->
				servicesList.parallelStream()
						.filter(s -> service == null || s == service)
						.filter(s -> !Collections.disjoint(s.getAppliances(), selectedVO.getImages()))
						.anyMatch(s -> image == null || selectedVO.getImages().contains(image))

			);
			servicesProvider.setFilter(selectedService ->
				selectedService.getAppliances().parallelStream()
						.filter(i -> vo == null || vo == i.getVo())
						.anyMatch(i -> image == null || i.equals(image))
			);
			imagesProvider.setFilter(selectedImage ->
				imagesFullList.parallelStream()
						.filter(i -> vo == null || i.getVo() == vo)
						.filter(i -> service == null || i.getService() == service)
						.anyMatch(i -> i.equals(selectedImage))
			);
		}
	}

	public Registration addCompletedListener(CompletedListener listener) {
		return addListener(CompletedEvent.class, listener, CompletedListener.COMPLETED_METHOD);
	}

	public String getURLParams() {
		StringBuilder builder = new StringBuilder("#!create/");
		try {
			Service s = services.getValue();

			Image i = images.getValue();
			builder.append("image/");
			Image imageToSend = i;
			for (Image img: imagesMap.get(i.getKey())) {
				if (img.getService() == s && img.getVo() == vos.getValue()) {
					imageToSend = img;
					break;
				}
			}
			builder.append(URLEncoder.encode(imageToSend.getId().toString(), "UTF-8"));
			builder.append("&");

			Flavour f = flavours.getValue();
			builder.append("flavour/");
			builder.append(URLEncoder.encode(f.getId().toString(), "UTF-8"));
			builder.append("&");
		} catch (UnsupportedEncodingException e) {
			logger.error("UTF-8 is unsupported! Should never happen.", e);
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

			// -1 to remove the button
			for (int i = 0; i < source.getComponentCount() - 1; i++) {
				RadioButtonGroup boxGroup = (RadioButtonGroup) ((Panel) source.getComponent(i)).getContent();
				if (boxGroup.getValue() == null) {
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

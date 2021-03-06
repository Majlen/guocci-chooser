package cz.cesnet.cloud;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import cz.cesnet.cloud.sources.Configuration;

@Theme("valo")
@Title("GUOCCI")
public class ChooserUI extends UI {
	private Navigator navigator;
	private HorizontalLayout breadcrumbs;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		Label guocci = new Label("OCCI Web Interface");
		guocci.addStyleName(ValoTheme.LABEL_HUGE);

		breadcrumbs = new HorizontalLayout();
		removeButtons();
		addButton("Choose", Page.getCurrent().getLocation().toString());


		HorizontalLayout titleBar = new HorizontalLayout(breadcrumbs, guocci);
		titleBar.setExpandRatio(guocci, 1);
		titleBar.setComponentAlignment(guocci, Alignment.MIDDLE_RIGHT);
		titleBar.setWidth("100%");
		titleBar.setMargin(false);

		Panel content = new Panel();
		content.addStyleName(ValoTheme.PANEL_BORDERLESS);

		navigator = new Navigator(this, content);
		navigator.addView("", new ChooseView());

		navigator.addViewChangeListener(viewChangeEvent -> {
			removeButtons();
			addButton("Choose", Page.getCurrent().getLocation().toString());
			return true;
		});


		VerticalLayout layout = new VerticalLayout(titleBar, content);
		setContent(layout);
	}

	public void addButton(Resource r, String link) {
		addNecessaryArrow();
		Button b = new Button(r);
		b.addClickListener(clickEvent -> Page.getCurrent().open(link, null));
		b.addStyleName(ValoTheme.BUTTON_QUIET);
		breadcrumbs.addComponent(b);
	}

	public void addButton(String s, String link) {
		addNecessaryArrow();
		Button b = new Button(s);
		b.addStyleName(ValoTheme.BUTTON_QUIET);
		b.addClickListener(clickEvent -> Page.getCurrent().open(link, null));
		breadcrumbs.addComponent(b);
	}

	private void addNecessaryArrow() {
		if (breadcrumbs.getComponentCount() > 0) {
			Button arrow = new Button(VaadinIcons.ANGLE_RIGHT);
			arrow.addStyleName(ValoTheme.BUTTON_BORDERLESS);
			arrow.setEnabled(false);
			breadcrumbs.addComponent(arrow);
		}
	}

	private void removeButtons() {
		Configuration configuration = (Configuration) VaadinServlet.getCurrent().getServletContext().getAttribute("configuration");
		breadcrumbs.removeAllComponents();
		addButton(VaadinIcons.HOME, configuration.getGuocciURI());
	}

	@WebServlet(urlPatterns = "/*", name = "GUOCCI chooser", asyncSupported = true)
	@VaadinServletConfiguration(ui = ChooserUI.class, productionMode = false)
	public static class MyUIServlet extends VaadinServlet {
	}
}

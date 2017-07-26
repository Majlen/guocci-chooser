package cz.cesnet.cloud;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

@Theme("valo")
@Title("GUOCCI")
public class ChooserUI extends UI {
	private Navigator navigator;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		Label guocci = new Label("OCCI Web Interface");
		guocci.addStyleName(ValoTheme.LABEL_HUGE);

		Button nextButton = new Button("Continue", VaadinIcons.ANGLE_RIGHT);
		nextButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
		nextButton.setEnabled(false);

		Button prevButton = new Button("Back", VaadinIcons.ANGLE_LEFT);
		if (vaadinRequest.getHeader("referrer") != null) {
			prevButton.setEnabled(false);
		} else {
			prevButton.setEnabled(true);
		}

		HorizontalLayout titleBar = new HorizontalLayout(prevButton, guocci, nextButton);
		titleBar.setExpandRatio(guocci, 1);
		titleBar.setComponentAlignment(guocci, Alignment.MIDDLE_CENTER);
		titleBar.setWidth("100%");

		Panel content = new Panel();
		content.addStyleName(ValoTheme.PANEL_BORDERLESS);

		ChooseView choose = new ChooseView();

		navigator = new Navigator(this, content);
		navigator.addView("", choose);

		nextButton.addClickListener(clickEvent -> Page.getCurrent().open("/guocci/" + choose.getURLParams(), null));
		choose.addCompletedListener(event -> nextButton.setEnabled(event.getCompleted()));

		prevButton.addClickListener(clickEvent -> Page.getCurrent().open(vaadinRequest.getHeader("referrer"), null));

		VerticalLayout layout = new VerticalLayout(titleBar, content);
		setContent(layout);
	}

	@WebServlet(urlPatterns = "/*", name = "GUOCCI chooser", asyncSupported = true)
	@VaadinServletConfiguration(ui = ChooserUI.class, productionMode = false)
	public static class MyUIServlet extends VaadinServlet {
	}
}

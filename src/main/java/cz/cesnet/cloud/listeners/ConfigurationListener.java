package cz.cesnet.cloud.listeners;

import cz.cesnet.cloud.sources.Configuration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

@WebListener
public class ConfigurationListener implements ServletContextListener {
	//This is not everything (defaults will be loaded from .war
	private static final String[] RESOURCE_FILES = {"/etc/guocci/config.properties"};

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		Properties properties = new Properties();
		ServletContext context = servletContextEvent.getServletContext();

		try {
			properties.load(context.getResourceAsStream("/config.properties"));
		} catch (IOException e) {
			System.out.println("Error reading default config file !");
		}

		for (String resource: RESOURCE_FILES) {
			try {
				properties.load(new FileInputStream(resource));
			} catch (FileNotFoundException e) {
				System.out.println("Config file " + resource + " was not found!");
			} catch (IOException e) {
				System.out.println("Error reading config file " + resource + "!");
			}
		}

		context.setAttribute("configuration", new Configuration(properties));
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		//Do nothing
	}
}

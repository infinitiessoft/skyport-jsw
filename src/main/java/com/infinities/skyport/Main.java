/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.infinities.skyport;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletRegistration;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.uri.UriComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.io.Closeables;
import com.infinities.skyport.cache.CachedServiceProvider;
import com.infinities.skyport.distributed.DistributedObjectFactory.Delegate;
import com.infinities.skyport.distributed.impl.hazelcast.HazelcastHelper;
import com.infinities.skyport.distributed.util.DistributedUtil;
import com.infinities.skyport.jpa.EntityManagerFactoryBuilder;
import com.infinities.skyport.model.SystemInfo;
import com.infinities.skyport.registrar.ConfigurationHomeFactory;
import com.infinities.skyport.registrar.DriverHomeImpl;
import com.infinities.skyport.registrar.ProfileHomeImpl;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.DriverHome;
import com.infinities.skyport.service.ModuleHome;
import com.infinities.skyport.service.WebsockifyService;
import com.infinities.skyport.ui.MainFrame;
import com.infinities.skyport.ui.ProgressDialog;
import com.infinities.skyport.util.Manifests;
import com.infinities.skyport.util.PropertiesHolder;

public class Main implements Skyport {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	protected String logbackFile = PropertiesHolder.CONFIG_FOLDER + File.separator + "logback.xml";
	protected String tomcatFile = PropertiesHolder.CONFIG_FOLDER + File.separator + "server.xml";
	protected String websockifyFile = PropertiesHolder.CONFIG_FOLDER + File.separator + "websockify.xml";
	private ConfigurationHome configurationHome;
	private DriverHome driverHomeService;
	private HttpServer server;
	// private ModuleHome moduleService;
	// private CustomCatalina catalina;
	// private WebsockifyService websockifyService;
	// private LocalWebsockifyServer skyportWebsockifyServer;
	private final boolean uiEnabled;
	private MainFrame frame;
	private String protocol, ip, port;
	private ProgressDialog startingDialog;
	private String title, version;


	// private Set<Connector> webServerConnector;
	// private ConnectorSet websockifyConnectorSet;

	public static void main(String[] args) {
		Main main = null;
		try {
			main = new Main(args);
			logger.trace("initialize skyport");
			main.initialize();
		} catch (Throwable e) {
			logger.error("Encounter error when initialize skyport.", e);
			try {
				if (main != null) {
					Closeables.close(main, true);
				}
			} catch (IOException e1) {
				logger.error("Encounter error when close skyport.", e1);
			}
		}
	}

	protected boolean arguments(String[] args) {
		boolean isLogback = false;
		boolean isTomcat = false;
		boolean isWebsockify = false;
		boolean isHazelcast = false;
		boolean isCluster = false;
		boolean isAccessconfig = false;

		for (int i = 0; i < args.length; i++) {
			if (isLogback) {
				logbackFile = args[i];
				isLogback = false;
			} else if ("-logback".equals(args[i])) {
				isLogback = true;
			} else if (isTomcat) {
				tomcatFile = args[i];
				isTomcat = false;
			} else if ("-server".equals(args[i])) {
				isTomcat = true;
			} else if (isWebsockify) {
				websockifyFile = args[i];
				isWebsockify = false;
			} else if ("-websockify".equals(args[i])) {
				isWebsockify = true;
			} else if (isCluster) {
				DistributedUtil.defaultDelegate = Delegate.valueOf(args[i]);
				isCluster = false;
			} else if ("-cluster".equals(args[i])) {
				isCluster = true;
			} else if (isHazelcast) {
				HazelcastHelper.HAZELCAST_FILE = args[i];
				isHazelcast = false;
			} else if ("-hazelcast".equals(args[i])) {
				isHazelcast = true;
			} else if (isAccessconfig) {
				ConfigurationHomeFactory.ACCESS_CONFIG_FILE = args[i];
				isAccessconfig = false;
			} else if ("-accessconfig".equals(args[i])) {
				isAccessconfig = true;
			}
		}

		return (true);

	}

	public Main(String[] args) {
		initializeLogger();
		setUpVersion();
		if (args != null) {
			if (!arguments(args)) {
				System.exit(0);
			}
		}

		String enabled = PropertiesHolder.getProperty(PropertiesHolder.UI_ENABLED);
		uiEnabled = Boolean.parseBoolean(enabled);
		logger.info("skyport ui enabled? {}", new Object[] { enabled });
		if (uiEnabled) {
			startingDialog = initStartingDialog();
			Runnable r = new Runnable() {

				@Override
				public void run() {
					logger.debug("show starting dialog");
					startingDialog.setVisible(true);
				}

			};

			Thread t = new Thread(r);
			t.start();
		}

		// Delegate delegate = DistributedUtil.getDelegate();
		// if (delegate == Delegate.disabled) {
		// interruptUnfinishTasks();
		// }
	}

	private void setUpVersion() {
		try {
			title = Manifests.getAttribute(Manifests.INPLEMENTATION_TITLE);
			version = Manifests.getAttribute(Manifests.INPLEMENTATION_VERSION);
			logger.info("{}{} start", new Object[] { title, version });
		} catch (IOException e) {
			logger.error("read META-INF/MANIFEST.MF file failed", e);
		}
	}

	private void initializeLogger() {
		logger.debug("Configure logback properties: {}", logbackFile);
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g.
			// default
			// configuration. For multi-step configuration, omit calling
			// context.reset().
			context.reset();
			configurator.doConfigure(logbackFile);
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);

	}

	// private void interruptUnfinishTasks() {
	// logger.debug("interrupting unfinish tasks");
	// TaskEventHome eventHome = new TaskEventHome();
	// TaskEventLogHome logHome = new TaskEventLogHome();
	// final List<TaskEvent> events = eventHome.findAllUnfinishTask();
	// final Date date = new Date();
	// for (final TaskEvent event : events) {
	// logger.debug("interrupting task: {}, config: {}, cmd: {}", new Object[] {
	// event.getId(), event.getConfig(),
	// event.getCmd() });
	// event.setStatus(Status.Fail);
	// TaskEvent evt = eventHome.merge(event);
	// logHome.persist(new TaskEventLog(evt, date, Status.Fail,
	// "Task has been interrupted unexpectively because skyport restart.",
	// null));
	// }
	// try {
	// EntityManagerHelper.commitAndClose();
	// } catch (Exception e) {
	// logger.error("interrupt unfinish tasks failed", e);
	// }
	//
	// }

	private ProgressDialog initStartingDialog() {
		return new ProgressDialog(null, "Dialog", "Starting.......", ModalityType.APPLICATION_MODAL);
	}

	@Override
	public void initialize() throws Throwable {
		// initializeParameters();
		printInfo();
		logger.debug("Configure JPA");
		EntityManagerFactoryBuilder.getEntityManagerFactory();
		driverHomeService = new DriverHomeImpl();
		driverHomeService.initialize();
		configurationHome = ConfigurationHomeFactory.getInstance();
		configurationHome.setDriverHome(driverHomeService);
		configurationHome.setProfileHome(new ProfileHomeImpl());
		configurationHome.initialize();

		setUpApi();

		// websockifyConnectorSet = XMLUtil.convertValue(new
		// File(websockifyFile), ConnectorSet.class);
		// logger.info("ConnectorSet size: {}",
		// websockifyConnectorSet.getConnectors().size());
		//
		// if (websockifyConnectorSet.getConnectors().size() <= 0) {
		// throw new
		// IllegalStateException("Number of Connector in Websockify.xml cannot be 0");
		// }
		// websockifyService = new
		// RoundRobinWebsockifyService(websockifyConnectorSet.getConnectors());
		// try {
		// websockifyService.initialize();
		// for (ServerConfiguration configuration :
		// websockifyConnectorSet.getConnectors()) {
		// if (Mode.local.equals(configuration.getMode())) {
		// skyportWebsockifyServer = new LocalWebsockifyServer();
		// skyportWebsockifyServer.activate();
		// websockifyService.setWebsockifyServer(skyportWebsockifyServer);
		// break;
		// }
		// }
		// } catch (Throwable e) {
		// logger.error("Start WebsockifyService failed", e);
		// throw e;
		// }

		// System.setProperty(Globals.CATALINA_HOME_PROP,
		// System.getProperty("user.dir") + "/tomcat.context/");
		// System.setProperty(Globals.CATALINA_BASE_PROP,
		// System.getProperty("user.dir") + "/tomcat.context/");
		// catalina = new CustomCatalina();
		// try {
		// initializeServlet();
		// } catch (Throwable e) {
		// logger.error("Start Servlet failed", e);
		// throw e;
		// }

		logger.trace("add shutdownhook");
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				try {
					close();
					logger.trace("skyport closed");
				} catch (Throwable e) {
					logger.error("Encounter Error while close Skyport", e);
					e.printStackTrace();
				}
			}
		});

		// Use hazelcast cluster group name as is Skyport ID
		// CamelHandler.initialize(getSystemInfo().getGroup(),
		// asyncDriverHomeService);

		// logger.debug("initialize modules");
		// try {
		// moduleService = new ModuleHomeImpl(this);
		// moduleService.initialize();
		// } catch (Throwable e) {
		// logger.warn("module service initialize failed, ignoring", e);
		// }

		if (uiEnabled) {
			startingDialog.dispose();
			frame = new MainFrame(driverHomeService, configurationHome, protocol, ip, port);
			frame.activate();
			frame.setVisible(true);
		}

	}

	private void setUpApi() {
		try {
			// ResourceConfig config = ResourceConfig.forApplication(new
			// NovaApplication(configurationHome));
			// ApplicationHandler appHandler = new ApplicationHandler(config);
			URI baseUri = UriBuilder.fromUri("http://localhost/").port(9999).build();

			// Initialize and register Jersey Servlet
			String path = String.format("/%s", UriComponent.decodePath(baseUri.getPath(), true).get(1).toString());

			WebappContext context = new WebappContext("GrizzlyContext", path);
			ServletRegistration registration = context.addServlet("ServletContainer", ServletContainer.class);
			registration.setInitParameter("javax.ws.rs.Application", NovaApplication.class.getName());
			// Add an init parameter - this could be loaded from a parameter
			// in the constructor
			registration.setInitParameter("myparam", "myvalue");
			registration.addMapping("/*");
			this.server = GrizzlyHttpServerFactory.createHttpServer(baseUri);
			context.deploy(server);
			// server.join();
		} catch (Exception e) {
			try {
				server.shutdownNow();
			} catch (Exception e1) {
				logger.error("stop server failed", e1);
			}
		}

	}

	private void printInfo() {
		System.out.println("=========================================================================");
		System.out.println();
		System.out.println("Skyport");
		System.out.println();
		System.out.println("Version: " + version);
		System.out.println();
		System.out.println("URL: " + protocol + "://" + ip + ":" + port);
		System.out.println();
		System.out.println("Copyright(c) 2011 InfinitiesSoft Solutions Inc.");
		System.out.println();
		System.out.println("=========================================================================");
		System.out.println();
	}

	// private void initializeParameters() throws Exception {
	// Server server = XMLUtil.convertValue(new File(tomcatFile), Server.class);
	// logger.info("Service size: {}", server.getServices());
	//
	// if (server.getServices().size() <= 0) {
	// throw new
	// IllegalStateException("Number of Service in server.xml cannot be 0");
	// }
	// Connector defaultConnector = null;
	// for (Service service : server.getServices()) {
	// if (service.getConnectors().size() <= 0) {
	// throw new
	// IllegalStateException("Number of Connector in server.xml cannot be 0");
	// }
	//
	// if (service.isDefaultService()) {
	// defaultConnector = null;
	// for (Connector connector : service.getConnectors()) {
	// if (connector.isDefaultConnector()) {
	// defaultConnector = connector;
	// break;
	// } else {
	// defaultConnector = connector;
	// }
	// }
	// if (defaultConnector != null) {
	// webServerConnector = service.getConnectors();
	// break;
	// }
	// } else {
	// for (Connector connector : service.getConnectors()) {
	// if (connector.isDefaultConnector()) {
	// defaultConnector = connector;
	// webServerConnector = service.getConnectors();
	// break;
	// } else {
	// defaultConnector = connector;
	// webServerConnector = service.getConnectors();
	// }
	// }
	// }
	// }
	//
	// protocol = defaultConnector.isEnableSSL() ? "https" : "http";
	// ip = Strings.isNullOrEmpty(defaultConnector.getIp()) ? "localhost" :
	// defaultConnector.getIp();
	// port = defaultConnector.getPort();
	// }

	// private void initializeServlet() throws Exception {
	// logger.debug("Configure Tomcat");
	// // tomcat.setBaseDir("./tomcat.context");
	// // File home = new File(basedir);
	// // File file = new File("./" + PropertiesHolder.CONFIG_FOLDER +
	// // File.separator + tomcatFile);
	// File file = new File(tomcatFile);
	// logger.debug("file exist? {}", file.getAbsolutePath());
	// catalina.setConfigFile(file.getAbsolutePath());
	// // logger.debug("Add webapp: {}", "vnc");
	// // tomcat.addWebapp("/vnc", "vnc");
	// //
	//
	// int limit =
	// Integer.parseInt(PropertiesHolder.getProperty(PropertiesHolder.LIMIT_MAX));
	//
	// logger.debug("Add webapp: {}", "skyport");
	// Context ctx = catalina.addWebapp("/", "skyport");
	// logger.debug("Add servlet: {}", "skyport");
	// CustomCatalina.addServlet(ctx, "skyport", new
	// SkyportServlet(configurationHome, websockifyService, limit));
	// logger.debug("Add servlet: {}", "skyportWeb");
	// CustomCatalina.addServlet(ctx, "skyportWeb", new SkyportWebServlet(this,
	// websockifyService, protocol, ip, port,
	// limit));
	// FilterDef emFilterDefForLegend = createFilterDef("emFilter",
	// EntityManagerFilter.class.getName());
	// ctx.addFilterDef(emFilterDefForLegend);
	// ctx.addFilterMap(createFilterMap("emFilter", "/skyport"));
	// logger.debug("Map path {} to {}", new Object[] { "skyport", "skyport" });
	// ctx.addServletMapping("/skyport", "skyport");
	// logger.debug("add EntityManagerFilter");
	// ctx.addApplicationListener(EntityManagerFilter.class.getCanonicalName());
	// FilterDef emFilterDef = createFilterDef("emFilter",
	// EntityManagerFilter.class.getName());
	// ctx.addFilterDef(emFilterDef);
	// ctx.addFilterMap(createFilterMap("emFilter", "/skyportWeb"));
	//
	// logger.debug("Map path {} to {}", new Object[] { "skyportWeb",
	// "skyportWeb" });
	// ctx.addServletMapping("/skyportWeb", "skyportWeb");
	//
	// logger.info("Start tomcat web server");
	// // tomcat.start();
	// catalina.start();
	// catalina.setAwait(true);
	// // catalina.setUseShutdownHook(false);
	//
	// logger.debug("catalina server state: {}",
	// catalina.getServer().getState());
	// if (catalina.getServer().getState().equals(LifecycleState.FAILED)) {
	// logger.error("Web server's start failed");
	// try {
	// catalina.stop();
	// } catch (Throwable e) {
	// logger.error("Stop tomcat failed", e);
	// }
	// JOptionPane.showMessageDialog(null,
	// "This Skyport cannot start web server.");
	// close();
	// }
	//
	// }

	// private FilterMap createFilterMap(String filterName, String urlPattern) {
	// FilterMap filterMap = new FilterMap();
	// filterMap.setFilterName(filterName);
	// filterMap.addURLPattern(urlPattern);
	// return filterMap;
	// }

	// private FilterDef createFilterDef(String filterName, String filterClass)
	// {
	// FilterDef filterDef = new FilterDef();
	// filterDef.setFilterName(filterName);
	// filterDef.setFilterClass(filterClass);
	// return filterDef;
	// }

	@Override
	public void close() {
		if (startingDialog != null) {
			startingDialog.dispose();
		}

		try {
			server.shutdownNow();
		} catch (Exception e) {
			logger.error("stop server failed", e);
		}

		// logger.info("close ModuleService");
		// if (moduleService != null) {
		// try {
		// moduleService.close();
		// moduleService = null;
		// } catch (Throwable e) {
		// logger.warn("module service initialize failed, ignoring", e);
		// }
		// }

		// catalina has it own runtimehook that will stop web server itself.
		// logger.info("close tomcat web server");
		// if (catalina != null) {
		// try {
		// catalina.stop();
		// catalina = null;
		// } catch (Exception e) {
		// logger.error("Stop tomcat failed", e);
		// }
		// }

		// First, shutdown the Apache Camel context!!
		// CamelHandler.getInstance().shutdownCamel();

		logger.info("close AsyncDriverHomeService");
		if (configurationHome != null) {
			try {
				configurationHome.close();
				configurationHome = null;
			} catch (Throwable e) {
				logger.error("Encounter error when close AsyncDriverHomeService.", e);
			}
		}

		logger.info("close DriverHomeService");
		if (driverHomeService != null) {
			try {
				driverHomeService.close();
				driverHomeService = null;
			} catch (Throwable e) {
				logger.error("Encounter error when close DriverHomeService.", e);
			}
		}

		// logger.info("close WebsockifyService");
		// if (websockifyService != null) {
		// try {
		// Closeables.close(websockifyService, true);
		// websockifyService = null;
		// } catch (IOException e) {
		// logger.error("Encounter error when close websockifyService.", e);
		// }
		// }
		// if (skyportWebsockifyServer != null) {
		// skyportWebsockifyServer.deactivate();
		// skyportWebsockifyServer = null;
		// }

		// logger.info("close EntityManagerBuilder");
		// DataCollectUtil.shutdown();
		// EntityManagerBuilder.shutdown(); bonecpconnection will close
		// EntityManager.
		// logger.info("EntityManagerBuilder closed");

		// System.exit(0);

	}

	@Override
	public ConfigurationHome getConfigurationHome() {
		return configurationHome;
	}

	@Override
	public DriverHome getDriverHome() {
		return driverHomeService;
	}

	@Override
	public SystemInfo getSystemInfo() throws Exception {
		SystemInfo info = new SystemInfo();
		info.setGroup(DistributedUtil.getDefaultDistributedObjectFactory().getGroup());
		info.getMembers().addAll(DistributedUtil.getDefaultDistributedObjectFactory().getMembers());
		info.setCluster(PropertiesHolder.getProperty(PropertiesHolder.CLUSTER_DELEGATE));
		info.setDriverCount(getDriverHome().findAll().size());
		int poolActive = 0;
		int poolInactive = 0;
		for (CachedServiceProvider serviceProvider : getConfigurationHome().findAll()) {
			if (serviceProvider.getConfiguration().getStatus()) {
				poolActive++;
			} else {
				poolInactive++;
			}
		}

		info.setPoolActive(poolActive);
		info.setPoolInactive(poolInactive);
		info.setTimeZone(PropertiesHolder.getProperty(PropertiesHolder.TIMEZONE));
		info.setTitle(title);
		info.setUi(uiEnabled);
		info.setVersion(version);
		// info.setWebServerConnectors(webServerConnector);
		// info.setWebsockifyServerConnectors(websockifyConnectorSet.getConnectors());

		return info;
	}

	@Override
	public ModuleHome getModuleHome() {
		return null; // moduleService;
	}

	@Override
	public WebsockifyService getWebsockifyService() {
		return null; // websockifyService;
	}
}

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
package com.infinities.skyport.webserver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import javax.naming.NamingException;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.naming.NamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.util.PropertiesHolder;

public class CustomTomcat extends Tomcat {

	private static final Logger logger = LoggerFactory.getLogger(CustomTomcat.class);
	private static final String GLOBAL_WEB_XML = "web.xml";
	private Catalina catalina;


	public CustomTomcat() {
		// initBaseDir();
	}

	public void setServer(Server server) throws LifecycleException {
		logger.debug("set Server: {}", server.getClass() + "   " + server.getPort() + "  " + server.getAddress() + "  "
				+ server.findServices().length);
		System.setProperty("catalina.useNaming", "true");
		try {
			final NamingContext globalNamingContext = new NamingContext(new Hashtable<String, Object>(), "ctxt");
			((StandardServer) server).setGlobalNamingContext(globalNamingContext);
			// globalNamingContext.bind(USER_DATABASE, createUserDatabase());
		} catch (final NamingException e) {
			throw new RuntimeException(e);
		}
		logger.debug("service {}", server.findServices()[0].getClass());
		StandardService service = (StandardService) server.findServices()[0];
		logger.debug("connector {}", service.findConnectors()[0].getClass());
		this.connector = service.findConnectors()[0];
		logger.debug("engine {}", service.getContainer().getClass());
		StandardEngine engine = (StandardEngine) service.getContainer();
		this.engine = engine;
		// this.defaultRealm = engine.getRealm();
		this.hostname = engine.getDefaultHost();
		this.port = connector.getPort();
		logger.debug("host {}", engine.findChildren()[0].getClass());

		this.host = (StandardHost) engine.findChildren()[0];
		((StandardHost) host).setUnpackWARs(false);
		((StandardHost) host).setCopyXML(true);
		((StandardHost) host).setStartChildren(true);
		((StandardHost) host).setDeployXML(true);
		host.setAutoDeploy(true);
		host.setDeployOnStartup(true);
		host.setUndeployOldVersions(true);
		host.setDeployIgnore("skyport");
		((StandardHost) engine.findChildren()[0]).setUnpackWARs(false);
		((StandardHost) engine.findChildren()[0]).setCopyXML(true);
		((StandardHost) engine.findChildren()[0]).setStartChildren(true);
		((StandardHost) engine.findChildren()[0]).setDeployXML(true);
		((StandardHost) engine.findChildren()[0]).setAutoDeploy(true);
		((StandardHost) engine.findChildren()[0]).setDeployOnStartup(true);
		((StandardHost) engine.findChildren()[0]).setUndeployOldVersions(true);
		((StandardHost) engine.findChildren()[0]).setDeployIgnore("skyport");

		// server.init();
		initBaseDir();
		System.setProperty("catalina.useNaming", "true");
		// StandardServer server2 = new StandardServer();
		// server2.setPort(server.getPort());
		// server2.addService(service);
		// logger.debug(" " + service.findLifecycleListeners().length+"  ");
		// // service.setServer(server2);
		this.service = service;
		// this.server = server2;
		catalina.setServer(server);
		this.server = server;

		// service = new StandardService();
		// service.setName("Tomcat");
		// server.addService(service);
		// this.server = server;
	}

	public void setCatalina(Catalina catalina) {
		this.catalina = catalina;
	}

	@Override
	public void start() throws LifecycleException {
		logger.debug("child {}", host.findChildren().length);
		super.start();
	}

	@Override
	protected void initBaseDir() {
		String catalinaHome = System.getProperty(Globals.CATALINA_HOME_PROP);
		if (basedir == null) {
			basedir = System.getProperty(Globals.CATALINA_BASE_PROP);
		}
		if (basedir == null) {
			basedir = catalinaHome;
		}
		if (basedir == null) {
			// Create a temp dir.
			basedir = System.getProperty("user.dir") + "/tomcat.context/"; // +
																			// port;
			File home = new File(basedir);
			home.mkdir();
			if (!home.isAbsolute()) {
				try {
					basedir = home.getCanonicalPath();
				} catch (IOException e) {
					basedir = home.getAbsolutePath();
				}
			}
		}
		if (catalinaHome == null) {
			System.setProperty(Globals.CATALINA_HOME_PROP, basedir);
		}
		System.setProperty(Globals.CATALINA_BASE_PROP, basedir);
	}

	@Override
	public Context addWebapp(Host host, String url, String name, String path) {
		silence(host, url);

		Context ctx = new StandardContext();
		ctx.setName(name);
		ctx.setPath(url);
		ctx.setDocBase(path);

		ctx.addLifecycleListener(new DefaultWebXmlListener());

		ContextConfig ctxCfg = new ContextConfig();
		ctx.addLifecycleListener(ctxCfg);

		// prevent it from looking ( if it finds one - it'll have dup error )
		String file = PropertiesHolder.CONFIG_FOLDER + File.separator + GLOBAL_WEB_XML;
		if (new File(file).exists()) {
			ctxCfg.setDefaultWebXml(file);
		} else {
			ctxCfg.setDefaultWebXml(noDefaultWebXmlPath());
		}
		ctxCfg.setDefaultWebXml(noDefaultWebXmlPath());

		try {
			String contextFile = path + "!/" + "META-INF/context.xml";
			if (new File(contextFile).exists()) {
				ctx.setConfigFile(new URL("jar:file:" + path + "!/" + "META-INF/context.xml"));
			}
		} catch (Exception e) {
			logger.warn("ignoring", e);
		}

		if (host == null) {
			getHost().addChild(ctx);
		} else {
			host.addChild(ctx);
		}

		logger.debug("host children: {}", host.findChildren().length);

		return ctx;
	}

	@Override
	public Server getServer() {
		return this.server;
	}

	private void silence(Host host, String ctx) {
		String base = "org.apache.catalina.core.ContainerBase.[default].[";
		if (host == null) {
			base += getHost().getName();
		} else {
			base += host.getName();
		}
		base += "].[";
		base += ctx;
		base += "]";
		logger.debug("silence base: {}", base);
	}

}

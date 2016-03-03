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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Constants;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat.DefaultWebXmlListener;
import org.apache.catalina.startup.Tomcat.ExistingStandardWrapper;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.juli.ClassLoaderLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class CustomCatalina extends Catalina {

	private final Logger logger = LoggerFactory.getLogger(CustomCatalina.class);
	private static final String GLOBAL_WEB_XML = "/conf/web.xml";
	private static final String GLOBAL_LOGGER_PROPERTIES = "/conf/logging.properties";


	public CustomCatalina() throws SecurityException, IOException {
		super();
		String homebase = System.getProperty(Globals.CATALINA_HOME_PROP);
		String file = homebase + GLOBAL_LOGGER_PROPERTIES;
		FileInputStream ins = Files.newInputStreamSupplier(new File(file)).getInput();
		ClassLoaderLogManager.getLogManager().readConfiguration(ins);
	}

	public Context addContext(String contextPath, String baseDir) {
		return addContext(getHost(), contextPath, baseDir);
	}

	public Context addContext(Host host, String contextPath, String dir) {
		return addContext(host, contextPath, contextPath, dir);
	}

	public Context addContext(Host host, String contextPath, String contextName, String dir) {
		silence(host, contextPath);
		Context ctx = new StandardContext();
		ctx.setName(contextName);
		ctx.setPath(contextPath);
		ctx.setDocBase(dir);
		ctx.addLifecycleListener(new FixContextListener());

		if (host == null) {
			getHost().addChild(ctx);
		} else {
			host.addChild(ctx);
		}
		return ctx;
	}

	public Wrapper addServlet(String contextPath, String servletName, Servlet servlet) {
		Container ctx = getHost().findChild(contextPath);
		return addServlet((Context) ctx, servletName, servlet);
	}

	public static Wrapper addServlet(Context ctx, String servletName, Servlet servlet) {
		// will do class for name and set init params
		Wrapper sw = new ExistingStandardWrapper(servlet);
		sw.setName(servletName);
		ctx.addChild(sw);

		return sw;
	}

	public Context addWebapp(String contextPath, String baseDir) throws ServletException {
		return addWebapp(getHost(), contextPath, baseDir);
	}

	public Context addWebapp(Host host, String url, String path) {
		return addWebapp(host, url, url, path);
	}

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
		String homebase = System.getProperty(Globals.CATALINA_HOME_PROP);
		String file = homebase + GLOBAL_WEB_XML;
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

	private StandardHost getHost() {
		if (server == null) {
			load();
		}
		StandardService service = (StandardService) server.findServices()[0];
		StandardEngine engine = (StandardEngine) service.getContainer();
		return (StandardHost) engine.findChildren()[0];
	}

	public String noDefaultWebXmlPath() {
		return Constants.NoDefaultWebXml;
	}
}

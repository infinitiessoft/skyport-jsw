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

import org.glassfish.jersey.server.ResourceConfig;

import com.infinities.nova.FaultWrapper;
import com.infinities.nova.common.config.Config;
import com.infinities.nova.middleware.AuthTokenMiddleware;
import com.infinities.nova.middleware.ComputeReqIdMiddleware;
import com.infinities.nova.middleware.KeystonecontextMiddleware;
import com.infinities.nova.middleware.NoAuthMiddleware;
import com.infinities.nova.resource.NovaResource;
import com.infinities.nova.security.CheckProjectIdFilter;
import com.infinities.nova.util.jackson.JacksonFeature;

public class NovaApplication extends ResourceConfig {

	public NovaApplication() {
		super(NovaApplication.class);
		this.register(FaultWrapper.class);
		this.register(CheckProjectIdFilter.class);
		// this.register(EntityManagerFilter.class);
		this.register(ComputeReqIdMiddleware.class);
		String strategy = Config.Instance.getOpt("auth_strategy").asText();
		if ("noauth".equals(strategy)) {
			this.register(NoAuthMiddleware.class);
		} else if ("keystone".equals(strategy)) {
			this.register(AuthTokenMiddleware.class);
			this.register(KeystonecontextMiddleware.class);
		}
		this.register(NovaResource.class);
		this.register(JacksonFeature.class);
	}
}

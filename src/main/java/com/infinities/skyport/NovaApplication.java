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

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import com.infinities.nova.api.VersionsApi;
import com.infinities.nova.api.factory.VersionsApiFactory;
import com.infinities.nova.api.middleware.AuthTokenMiddleware;
import com.infinities.nova.api.middleware.ComputeReqIdMiddleware;
import com.infinities.nova.api.middleware.KeystonecontextMiddleware;
import com.infinities.nova.api.middleware.NoAuthMiddleware;
import com.infinities.nova.api.openstack.FaultWrapper;
import com.infinities.nova.api.openstack.compute.flavors.FlavorsController;
import com.infinities.nova.api.openstack.compute.flavors.FlavorsControllerFactory;
import com.infinities.nova.api.openstack.compute.flavors.api.DaseinFlavorsApi;
import com.infinities.nova.api.openstack.compute.flavors.api.FlavorsApi;
import com.infinities.nova.api.openstack.compute.images.ImageMetadataController;
import com.infinities.nova.api.openstack.compute.images.ImageMetadataControllerFactory;
import com.infinities.nova.api.openstack.compute.images.ImagesController;
import com.infinities.nova.api.openstack.compute.images.ImagesControllerFactory;
import com.infinities.nova.api.openstack.compute.images.api.DaseinImageMetadataApi;
import com.infinities.nova.api.openstack.compute.images.api.DaseinImagesApi;
import com.infinities.nova.api.openstack.compute.images.api.ImageMetadataApi;
import com.infinities.nova.api.openstack.compute.images.api.ImagesApi;
import com.infinities.nova.api.openstack.compute.limits.LimitsController;
import com.infinities.nova.api.openstack.compute.limits.LimitsControllerFactory;
import com.infinities.nova.api.openstack.compute.servers.ServersController;
import com.infinities.nova.api.openstack.compute.servers.ServersControllerFactory;
import com.infinities.nova.api.openstack.compute.servers.api.ComputeApi;
import com.infinities.nova.api.openstack.compute.servers.api.DaseinComputeApi;
import com.infinities.nova.api.openstack.compute.servers.api.DaseinComputeTaskApi;
import com.infinities.nova.api.openstack.compute.servers.ips.ServerIpsController;
import com.infinities.nova.api.openstack.compute.servers.ips.ServerIpsControllerFactory;
import com.infinities.nova.api.openstack.compute.servers.metadata.ServerMetadataController;
import com.infinities.nova.api.openstack.compute.servers.metadata.ServerMetadataControllerFactory;
import com.infinities.nova.api.openstack.compute.task.ComputeTaskApi;
import com.infinities.nova.api.v2.Version2Api;
import com.infinities.nova.api.v2.factory.Version2ApiFactory;
import com.infinities.nova.common.Config;
import com.infinities.nova.resource.NovaResource;
import com.infinities.nova.util.jackson.JacksonFeature;
import com.infinities.skyport.registrar.ConfigurationHomeH2Factory;
import com.infinities.skyport.service.ConfigurationHome;

public class NovaApplication extends ResourceConfig {

	public NovaApplication() {
		super(NovaApplication.class);

		this.register(new AbstractBinder() {

			@Override
			protected void configure() {
				bindFactory(VersionsApiFactory.class).to(VersionsApi.class).in(Singleton.class);
				bindFactory(Version2ApiFactory.class).to(Version2Api.class).in(Singleton.class);
				bind(DaseinFlavorsApi.class).to(FlavorsApi.class).in(Singleton.class);
				bindFactory(FlavorsControllerFactory.class).to(FlavorsController.class).in(Singleton.class);
				bindFactory(ConfigurationHomeH2Factory.class).to(ConfigurationHome.class).in(Singleton.class);

				bind(DaseinImagesApi.class).to(ImagesApi.class).in(Singleton.class);
				bind(DaseinImageMetadataApi.class).to(ImageMetadataApi.class).in(Singleton.class);
				bindFactory(ImagesControllerFactory.class).to(ImagesController.class).in(Singleton.class);
				bindFactory(ImageMetadataControllerFactory.class).to(ImageMetadataController.class).in(Singleton.class);

				bind(DaseinComputeApi.class).to(ComputeApi.class).in(Singleton.class);
				bind(DaseinComputeTaskApi.class).to(ComputeTaskApi.class).in(Singleton.class);
				bindFactory(ServersControllerFactory.class).to(ServersController.class).in(Singleton.class);
				bindFactory(ServerMetadataControllerFactory.class).to(ServerMetadataController.class).in(Singleton.class);
				bindFactory(ServerIpsControllerFactory.class).to(ServerIpsController.class).in(Singleton.class);

				bindFactory(LimitsControllerFactory.class).to(LimitsController.class).in(Singleton.class);
			}

		});
		this.register(FaultWrapper.class);
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

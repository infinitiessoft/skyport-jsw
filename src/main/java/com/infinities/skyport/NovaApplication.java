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

import com.infinities.nova.FaultWrapper;
import com.infinities.nova.availablityzone.api.AvailabilityZoneApi;
import com.infinities.nova.availablityzone.api.DaseinAvailabilityZoneApi;
import com.infinities.nova.availablityzone.controller.AvailabilityZoneController;
import com.infinities.nova.availablityzone.controller.AvailabilityZoneControllerFactory;
import com.infinities.nova.common.config.Config;
import com.infinities.nova.flavors.api.DaseinFlavorsApi;
import com.infinities.nova.flavors.api.FlavorsApi;
import com.infinities.nova.flavors.controller.FlavorsController;
import com.infinities.nova.flavors.controller.FlavorsControllerFactory;
import com.infinities.nova.images.api.DaseinImagesApi;
import com.infinities.nova.images.api.ImagesApi;
import com.infinities.nova.images.controller.ImagesController;
import com.infinities.nova.images.controller.ImagesControllerFactory;
import com.infinities.nova.images.metadata.api.DaseinImageMetadataApi;
import com.infinities.nova.images.metadata.api.ImageMetadataApi;
import com.infinities.nova.images.metadata.controller.ImageMetadataController;
import com.infinities.nova.images.metadata.controller.ImageMetadataControllerFactory;
import com.infinities.nova.keypairs.api.DaseinKeyPairsApi;
import com.infinities.nova.keypairs.api.KeyPairsApi;
import com.infinities.nova.keypairs.controller.KeyPairsController;
import com.infinities.nova.keypairs.controller.KeyPairsControllerFactory;
import com.infinities.nova.limits.controller.LimitsController;
import com.infinities.nova.limits.controller.LimitsControllerFactory;
import com.infinities.nova.middleware.AuthTokenMiddleware;
import com.infinities.nova.middleware.ComputeReqIdMiddleware;
import com.infinities.nova.middleware.KeystonecontextMiddleware;
import com.infinities.nova.middleware.NoAuthMiddleware;
import com.infinities.nova.networks.api.DaseinNetworksApi;
import com.infinities.nova.networks.api.NetworksApi;
import com.infinities.nova.networks.controller.NetworksController;
import com.infinities.nova.networks.controller.NetworksControllerFactory;
import com.infinities.nova.resource.NovaResource;
import com.infinities.nova.securitygroups.api.DaseinSecurityGroupsApi;
import com.infinities.nova.securitygroups.api.SecurityGroupsApi;
import com.infinities.nova.securitygroups.controller.SecurityGroupsController;
import com.infinities.nova.securitygroups.controller.SecurityGroupsControllerFactory;
import com.infinities.nova.securitygroups.rules.api.DaseinSecurityGroupRulesApi;
import com.infinities.nova.securitygroups.rules.api.SecurityGroupRulesApi;
import com.infinities.nova.securitygroups.rules.controller.SecurityGroupRulesController;
import com.infinities.nova.securitygroups.rules.controller.SecurityGroupRulesControllerFactory;
import com.infinities.nova.servers.api.ComputeApi;
import com.infinities.nova.servers.api.ComputeTaskApi;
import com.infinities.nova.servers.api.DaseinComputeApi;
import com.infinities.nova.servers.api.DaseinComputeTaskApi;
import com.infinities.nova.servers.controller.ServersController;
import com.infinities.nova.servers.controller.ServersControllerFactory;
import com.infinities.nova.servers.interfaces.api.DaseinInterfaceAttachmentsApi;
import com.infinities.nova.servers.interfaces.api.InterfaceAttachmentsApi;
import com.infinities.nova.servers.interfaces.controller.InterfaceAttachmentsController;
import com.infinities.nova.servers.interfaces.controller.InterfaceAttachmentsControllerFactory;
import com.infinities.nova.servers.ips.controller.ServerIpsController;
import com.infinities.nova.servers.ips.controller.ServerIpsControllerFactory;
import com.infinities.nova.servers.metadata.controller.ServerMetadataController;
import com.infinities.nova.servers.metadata.controller.ServerMetadataControllerFactory;
import com.infinities.nova.servers.volumes.api.DaseinVolumeAttachmentsApi;
import com.infinities.nova.servers.volumes.api.VolumeAttachmentsApi;
import com.infinities.nova.servers.volumes.controller.VolumeAttachmentsController;
import com.infinities.nova.servers.volumes.controller.VolumeAttachmentsControllerFactory;
import com.infinities.nova.snapshots.api.DaseinSnapshotsApi;
import com.infinities.nova.snapshots.api.SnapshotsApi;
import com.infinities.nova.snapshots.controller.SnapshotsController;
import com.infinities.nova.snapshots.controller.SnapshotsControllerFactory;
import com.infinities.nova.util.jackson.JacksonFeature;
import com.infinities.nova.versions.api.VersionsApi;
import com.infinities.nova.versions.api.VersionsApiFactory;
import com.infinities.nova.versions.v2.api.Version2Api;
import com.infinities.nova.versions.v2.api.Version2ApiFactory;
import com.infinities.nova.volumes.api.DaseinVolumesApi;
import com.infinities.nova.volumes.api.VolumesApi;
import com.infinities.nova.volumes.controller.VolumesController;
import com.infinities.nova.volumes.controller.VolumesControllerFactory;
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

				bind(DaseinKeyPairsApi.class).to(KeyPairsApi.class).in(Singleton.class);
				bindFactory(KeyPairsControllerFactory.class).to(KeyPairsController.class).in(Singleton.class);

				bind(DaseinAvailabilityZoneApi.class).to(AvailabilityZoneApi.class).in(Singleton.class);
				bindFactory(AvailabilityZoneControllerFactory.class).to(AvailabilityZoneController.class)
						.in(Singleton.class);

				bind(DaseinVolumeAttachmentsApi.class).to(VolumeAttachmentsApi.class).in(Singleton.class);
				bindFactory(VolumeAttachmentsControllerFactory.class).to(VolumeAttachmentsController.class).in(
						Singleton.class);

				bind(DaseinVolumesApi.class).to(VolumesApi.class).in(Singleton.class);
				bindFactory(VolumesControllerFactory.class).to(VolumesController.class).in(Singleton.class);

				bind(DaseinSnapshotsApi.class).to(SnapshotsApi.class).in(Singleton.class);
				bindFactory(SnapshotsControllerFactory.class).to(SnapshotsController.class).in(Singleton.class);

				bind(DaseinSecurityGroupsApi.class).to(SecurityGroupsApi.class).in(Singleton.class);
				bindFactory(SecurityGroupsControllerFactory.class).to(SecurityGroupsController.class).in(Singleton.class);

				bind(DaseinSecurityGroupRulesApi.class).to(SecurityGroupRulesApi.class).in(Singleton.class);
				bindFactory(SecurityGroupRulesControllerFactory.class).to(SecurityGroupRulesController.class).in(
						Singleton.class);

				bind(DaseinNetworksApi.class).to(NetworksApi.class).in(Singleton.class);
				bindFactory(NetworksControllerFactory.class).to(NetworksController.class).in(Singleton.class);

				bind(DaseinInterfaceAttachmentsApi.class).to(InterfaceAttachmentsApi.class).in(Singleton.class);
				bindFactory(InterfaceAttachmentsControllerFactory.class).to(InterfaceAttachmentsController.class).in(
						Singleton.class);

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

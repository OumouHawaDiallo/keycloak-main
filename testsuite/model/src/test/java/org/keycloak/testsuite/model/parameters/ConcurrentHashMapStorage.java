/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.model.parameters;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.exportimport.ExportSpi;
import org.keycloak.exportimport.ImportSpi;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.dir.DirImportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileImportProviderFactory;
import org.keycloak.keys.KeyProviderFactory;
import org.keycloak.keys.KeySpi;
import org.keycloak.models.ClientScopeSpi;
import org.keycloak.models.map.storage.MapStorageSpi;
import org.keycloak.services.clientpolicy.ClientPolicyManagerFactory;
import org.keycloak.services.clientpolicy.ClientPolicyManagerSpi;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicySpi;
import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.testsuite.model.Config;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorage extends KeycloakModelParameters {

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .add(ExportSpi.class)
      .add(ClientPolicyManagerSpi.class)
      .add(ImportSpi.class)
      .add(ClientRegistrationPolicySpi.class)
      .add(ClientScopeSpi.class)
      .add(KeySpi.class)
      .build();

    static {
        // CryptoIntegration needed for import of realms
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(ConcurrentHashMapStorageProviderFactory.class)
      // start providers needed for export
      .add(SingleFileExportProviderFactory.class)
      .add(DirExportProviderFactory.class)
      .add(ClientPolicyManagerFactory.class)
      // end providers needed for export
      // start providers needed for import
      .add(SingleFileImportProviderFactory.class)
      .add(DirImportProviderFactory.class)
      .add(ClientRegistrationPolicyFactory.class)
      .add(KeyProviderFactory.class)
      // end providers needed for import
      .build();

    @Override
    public void updateConfig(Config cf) {
        cf.spi(MapStorageSpi.NAME)
            .defaultProvider(ConcurrentHashMapStorageProviderFactory.PROVIDER_ID)
            .provider(ConcurrentHashMapStorageProviderFactory.PROVIDER_ID)
              .config("dir", "${project.build.directory:target}");
    }

    public ConcurrentHashMapStorage() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }

}

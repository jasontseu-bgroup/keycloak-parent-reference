package org.keycloak.storage.ldap.mappers;

import org.keycloak.models.RealmModel;
import org.keycloak.storage.user.SynchronizationResult;

public interface LDAPStorageSyncMapper {
    /**
     * Sync data from federated storage to Keycloak. It's useful just if mapper needs some data preloaded from federated storage (For example
     * load roles from federated provider and sync them to Keycloak database)
     * <p>
     * Applicable just if sync is supported
     */
    SynchronizationResult syncDataFromFederationProviderToKeycloak(RealmModel realm);

    /**
     * Sync data from Keycloak back to federated storage
     **/
    SynchronizationResult syncDataFromKeycloakToFederationProvider(RealmModel realm);
}

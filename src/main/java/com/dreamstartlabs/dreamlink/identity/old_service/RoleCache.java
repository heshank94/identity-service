package com.dreamstartlabs.dreamlink.identity.old_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for role mappings between OneLogin role IDs and Keycloak role names.
 *
 * This cache stores:
 * - OneLogin role ID -> OneLogin role name mapping
 * - OneLogin role name -> Keycloak role ID mapping
 *
 * The cache is thread-safe and uses ConcurrentHashMap for concurrent access.
 */
@Component
public class RoleCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleCache.class);

    // OneLogin role ID -> OneLogin role name
    private final ConcurrentHashMap<Long, String> oneLoginIdToName = new ConcurrentHashMap<>();

    // OneLogin role name -> Keycloak role ID
    private final ConcurrentHashMap<String, String> keycloakRoleMap = new ConcurrentHashMap<>();

    /**
     * Caches an OneLogin role ID to name mapping.
     */
    public void cacheOneLoginRole(Long roleId, String roleName) {
        oneLoginIdToName.put(roleId, roleName);
        LOGGER.debug("Cached OneLogin role: ID={}, Name={}", roleId, roleName);
    }

    /**
     * Retrieves the OneLogin role name for a given role ID from cache.
     *
     * @param roleId the OneLogin role ID
     * @return the role name, or null if not in cache
     */
    public String getOneLoginRoleName(Long roleId) {
        return oneLoginIdToName.get(roleId);
    }

    /**
     * Caches a Keycloak role mapping by OneLogin role name.
     *
     * @param oneLoginRoleName the OneLogin role name
     * @param keycloakRoleId the Keycloak role ID
     */
    public void cacheKeycloakRole(String oneLoginRoleName, String keycloakRoleId) {
        keycloakRoleMap.put(oneLoginRoleName, keycloakRoleId);
        LOGGER.debug("Cached Keycloak role mapping: OneLogin name={}, Keycloak ID={}", oneLoginRoleName, keycloakRoleId);
    }

    /**
     * Retrieves the Keycloak role ID for a given OneLogin role name from cache.
     *
     * @param oneLoginRoleName the OneLogin role name
     * @return the Keycloak role ID, or null if not in cache
     */
    public String getKeycloakRoleId(String oneLoginRoleName) {
        return keycloakRoleMap.get(oneLoginRoleName);
    }

    /**
     * Check if a Keycloak role exists in the cache.
     *
     * @param oneLoginRoleName the OneLogin role name
     * @return true if the role exists in cache, false otherwise
     */
    public boolean hasKeycloakRole(String oneLoginRoleName) {
        return keycloakRoleMap.containsKey(oneLoginRoleName);
    }

    /**
     * Clears all cached role mappings.
     * Use with caution - typically only for testing.
     */
    public void clear() {
        oneLoginIdToName.clear();
        keycloakRoleMap.clear();
        LOGGER.debug("Role cache cleared");
    }

    /**
     * Returns the current size of the OneLogin role cache.
     */
    public int getOneLoginCacheSize() {
        return oneLoginIdToName.size();
    }

    /**
     * Returns the current size of the Keycloak role cache.
     */
    public int getKeycloakCacheSize() {
        return keycloakRoleMap.size();
    }
}

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




}

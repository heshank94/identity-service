package com.dreamstartlabs.dreamlink.identity.utils;

import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginRole;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.service.onelogin.OneLoginClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Heshan Karunaratne
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleResolverUtil {
    private final OneLoginClientService oneLoginClientService;
    private final Map<Long, String> roleNameCache = new ConcurrentHashMap<>();

    public List<String> resolveRoleNames(OneLoginUser user) {
        if (user.getRoleIds() == null || user.getRoleIds().isEmpty()) {
            return List.of();
        }

        List<String> roleNames = new ArrayList<>();

        for (Long roleId : user.getRoleIds()) {
            try {
                String roleName = resolveRoleName(roleId);
                if (roleName != null) roleNames.add(roleName);
            } catch (Exception e) {
                log.warn("Could not resolve role ID {} for user {}: {}", roleId, user.getId(), e.getMessage());
            }
        }

        return roleNames;
    }

    private String resolveRoleName(Long roleId) {
        String cached = roleNameCache.get(roleId);
        if (cached != null) {
            log.info("Role ID {} resolved from cache: {}", roleId, cached);
            return cached;
        }

        OneLoginRole role = oneLoginClientService.getRoleById(roleId);
        if (role == null || role.getName() == null) {
            log.warn("Could not resolve name for role ID {}", roleId);
            return null;
        }

        roleNameCache.put(roleId, role.getName());
        log.info("Role ID {} fetched and cached: {}", roleId, role.getName());
        return role.getName();
    }

    public void clearCache() {
        roleNameCache.clear();
        log.debug("Role name cache cleared.");
    }
}

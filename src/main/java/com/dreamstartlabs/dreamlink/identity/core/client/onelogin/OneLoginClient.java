package com.dreamstartlabs.dreamlink.identity.core.client.onelogin;

import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginRole;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;

import java.util.List;

/**
 * @author Heshan Karunaratne
 */
public interface OneLoginClient {
    List<OneLoginUser> getUsers(String updatedSince);
    OneLoginUser getUserById(Long userId);
    List<OneLoginRole> getAllRoles();
    OneLoginRole getRoleById(Long roleId);
    List<OneLoginEvent> getDeletionEvents(String since);
}

package com.dreamstartlabs.dreamlink.identity.core.client.onelogin;

import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginRole;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginUser;

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
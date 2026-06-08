package com.dreamstartlabs.dreamlink.identity.utils;

import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;

/**
 * @author Heshan Karunaratne
 */
public final class CustomAttributeUtil {

    private CustomAttributeUtil() {
    }

    public static String extract(OneLoginUser user, String key) {
        if (user.getCustomAttributes() == null) return "";
        Object value = user.getCustomAttributes().get(key);
        return value != null ? value.toString() : "";
    }
}
package com.dreamstartlabs.dreamlink.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OneLogin Role representation used in the OneLogin API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OneLoginRole {

    private Long id;
    private String name;

    public OneLoginRole() {
    }

    public OneLoginRole(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "OneLoginRole{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

package com.dreamstartlabs.dreamlink.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Keycloak Role representation used in the Admin REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakRole {

    private String id;
    private String name;
    private String description;
    private boolean composite;
    private boolean clientRole;
    private String containerId;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isComposite() {
        return composite;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
    }

    public boolean isClientRole() {
        return clientRole;
    }

    public void setClientRole(boolean clientRole) {
        this.clientRole = clientRole;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    @Override
    public String toString() {
        return "KeycloakRole{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", composite=" + composite +
                ", clientRole=" + clientRole +
                ", containerId='" + containerId + '\'' +
                '}';
    }
}

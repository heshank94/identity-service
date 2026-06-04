package com.dreamstartlabs.dreamlink.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OneLoginUser {

    private Long id;
    private String username;
    private String email;

    @JsonProperty("firstname")
    private String firstName;

    @JsonProperty("lastname")
    private String lastName;

    private Integer status;
    private Integer state;

    @JsonProperty("activated_at")
    private String activatedAt;

    @JsonProperty("distinguished_name")
    private String distinguishedName;

    @JsonProperty("external_id")
    private String externalId;

    @JsonProperty("last_login")
    private String lastLogin;

    private String company;

    @JsonProperty("directory_id")
    private Long directoryId;

    @JsonProperty("invitation_sent_at")
    private String invitationSentAt;

    @JsonProperty("member_of")
    private String memberOf;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("preferred_locale_code")
    private String preferredLocaleCode;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("userprincipalname")
    private String userPrincipalName;

    @JsonProperty("trusted_idp_id")
    private Long trustedIdpId;

    private String comment;
    private String title;

    @JsonProperty("role_ids")
    private List<Long> roleIds;

    private String department;

    @JsonProperty("custom_attributes")
    private Map<String, Object> customAttributes;

    @JsonProperty("invalid_login_attempts")
    private Integer invalidLoginAttempts;

    @JsonProperty("manager_user_id")
    private Long managerUserId;

    @JsonProperty("locked_until")
    private String lockedUntil;

    @JsonProperty("manager_ad_id")
    private Long managerAdId;

    private String phone;

    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("password_changed_at")
    private String passwordChangedAt;

    @JsonProperty("samaccountname")
    private String samAccountName;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(String activatedAt) {
        this.activatedAt = activatedAt;
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public void setDistinguishedName(String distinguishedName) {
        this.distinguishedName = distinguishedName;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Long getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(Long directoryId) {
        this.directoryId = directoryId;
    }

    public String getInvitationSentAt() {
        return invitationSentAt;
    }

    public void setInvitationSentAt(String invitationSentAt) {
        this.invitationSentAt = invitationSentAt;
    }

    public String getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(String memberOf) {
        this.memberOf = memberOf;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPreferredLocaleCode() {
        return preferredLocaleCode;
    }

    public void setPreferredLocaleCode(String preferredLocaleCode) {
        this.preferredLocaleCode = preferredLocaleCode;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserPrincipalName() {
        return userPrincipalName;
    }

    public void setUserPrincipalName(String userPrincipalName) {
        this.userPrincipalName = userPrincipalName;
    }

    public Long getTrustedIdpId() {
        return trustedIdpId;
    }

    public void setTrustedIdpId(Long trustedIdpId) {
        this.trustedIdpId = trustedIdpId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public Integer getInvalidLoginAttempts() {
        return invalidLoginAttempts;
    }

    public void setInvalidLoginAttempts(Integer invalidLoginAttempts) {
        this.invalidLoginAttempts = invalidLoginAttempts;
    }

    public Long getManagerUserId() {
        return managerUserId;
    }

    public void setManagerUserId(Long managerUserId) {
        this.managerUserId = managerUserId;
    }

    public String getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(String lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Long getManagerAdId() {
        return managerAdId;
    }

    public void setManagerAdId(Long managerAdId) {
        this.managerAdId = managerAdId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(String passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public String getSamAccountName() {
        return samAccountName;
    }

    public void setSamAccountName(String samAccountName) {
        this.samAccountName = samAccountName;
    }
}

package com.w2w.api.manager.dto;

public class AddManagerRequest {
    private String firstName;
    private String lastName;
    private String email;
    private Integer companyId;
    private boolean emailInstructions;
    private ManagerPermissionsDto permissions;

    public AddManagerRequest() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public boolean isEmailInstructions() { return emailInstructions; }
    public void setEmailInstructions(boolean emailInstructions) { this.emailInstructions = emailInstructions; }

    public ManagerPermissionsDto getPermissions() { return permissions; }
    public void setPermissions(ManagerPermissionsDto permissions) { this.permissions = permissions; }
}

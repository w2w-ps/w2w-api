package com.w2w.api.login;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;

    @Column(name = "user_login_id", unique = true)
    private String loginId;

    @Column(name = "user_login_pw")
    private String password;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "emp_type_id")
    private Integer empTypeId;

    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "encryption_type")
    private Integer encryptionType;

    @Column(name = "login_failures")
    private Integer loginFailures;

    public User() {}

    public Integer getId() { return id; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public Integer getEmpTypeId() { return empTypeId; }
    public void setEmpTypeId(Integer empTypeId) { this.empTypeId = empTypeId; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public Integer getEncryptionType() { return encryptionType; }
    public void setEncryptionType(Integer encryptionType) { this.encryptionType = encryptionType; }

    public Integer getLoginFailures() { return loginFailures; }
    public void setLoginFailures(Integer loginFailures) { this.loginFailures = loginFailures; }
}

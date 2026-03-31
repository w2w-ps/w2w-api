package com.w2w.api.login;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "USERS")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USUSERID")
    private Integer id;

    @Column(name = "USUSERLOGINID", unique = true)
    private String loginId;

    @Column(name = "USUSERLOGINPW")
    private String password;

    @Column(name = "USCOMPANYID")
    private Integer companyId;

    @Column(name = "USWORKERTYPE")
    private String workerType;

    @Column(name = "USENCRYPTIONTYPE")
    private Integer encryptionType;

    @Column(name = "USLOGINFAILURES")
    private Integer loginFailures;

    public User() {}

    public Integer getId() { return id; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getWorkerType() { return workerType; }
    public void setWorkerType(String workerType) { this.workerType = workerType; }

    public Integer getEncryptionType() { return encryptionType; }
    public void setEncryptionType(Integer encryptionType) { this.encryptionType = encryptionType; }

    public Integer getLoginFailures() { return loginFailures; }
    public void setLoginFailures(Integer loginFailures) { this.loginFailures = loginFailures; }
}

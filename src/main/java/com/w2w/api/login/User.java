package com.w2w.api.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import com.w2w.api.employee.Employee;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;

    @Column(name = "user_login_id", unique = true)
    private String loginId;

    @JsonIgnore
    @Column(name = "user_login_pw")
    private String password;

    @Column(name = "company_id")
    private Integer companyId;

    @ManyToOne
    @JoinColumn(name = "emp_type_id")
    private EmpType empType;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private UserRole role;

    @Column(name = "encryption_type")
    private Integer encryptionType;

    @Column(name = "login_failures")
    private Integer loginFailures;

    @OneToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    public User() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public EmpType getEmpType() { return empType; }
    public void setEmpType(EmpType empType) { this.empType = empType; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Integer getEncryptionType() { return encryptionType; }
    public void setEncryptionType(Integer encryptionType) { this.encryptionType = encryptionType; }

    public Integer getLoginFailures() { return loginFailures; }
    public void setLoginFailures(Integer loginFailures) { this.loginFailures = loginFailures; }

    public Integer getEmployeeId() {
        return employee != null ? employee.getEmployeeId() : null;
    }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
}

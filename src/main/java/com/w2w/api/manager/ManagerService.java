package com.w2w.api.manager;

import com.w2w.api.employee.Employee;
import com.w2w.api.employee.EmployeeRepository;
import com.w2w.api.login.EmpType;
import com.w2w.api.login.EmpTypeRepository;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.login.UserRoleRepository;
import com.w2w.api.manager.dto.AddManagerRequest;
import com.w2w.api.manager.dto.ManagerPermissionsDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ManagerService {

    private final EmployeeRepository employeeRepository;
    private final LoginRepository loginRepository;
    private final ManagerPermissionsRepository permissionsRepository;
    private final UserRoleRepository roleRepository;
    private final EmpTypeRepository empTypeRepository;
    private final PasswordEncoder passwordEncoder;

    public ManagerService(EmployeeRepository employeeRepository, 
                          LoginRepository loginRepository, 
                          ManagerPermissionsRepository permissionsRepository,
                          UserRoleRepository roleRepository,
                          EmpTypeRepository empTypeRepository,
                          PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.loginRepository = loginRepository;
        this.permissionsRepository = permissionsRepository;
        this.roleRepository = roleRepository;
        this.empTypeRepository = empTypeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User addManager(AddManagerRequest request) {
        // 1. Create Employee
        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setCompanyId(request.getCompanyId());
        employee.setStatus("Active");
        
        employee = employeeRepository.save(employee);

        // 2. Create User
        User user = new User();
        user.setLoginId(request.getEmail());
        user.setPassword(passwordEncoder.encode("Welcome123!")); // In real usage, this might be temporary
        user.setCompanyId(request.getCompanyId());
        user.setEmployee(employee);
        
        // Find and assign Manager Role
        UserRole managerRole = roleRepository.findByName("Manager")
            .orElseThrow(() -> new RuntimeException("Manager role not found in database."));
        user.setRole(managerRole);
        
        // Use default/first EmpType (e.g. Full Time)
        EmpType defaultType = empTypeRepository.findByName("Full Time")
            .orElseGet(() -> empTypeRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No employee types found in database.")));
        user.setEmpType(defaultType);

        user = loginRepository.save(user);

        // 3. Create Manager Permissions
        ManagerPermissions permissions = new ManagerPermissions();
        permissions.setUser(user);
        
        ManagerPermissionsDto pDto = request.getPermissions();
        if (pDto != null) {
            permissions.setCanAddShifts(pDto.isCanAddShifts());
            permissions.setCanImportTemplates(pDto.isCanImportTemplates());
            permissions.setCanUploadShifts(pDto.isCanUploadShifts());
            permissions.setCanAutofillShifts(pDto.isCanAutofillShifts());
            permissions.setCanClearSchedules(pDto.isCanClearSchedules());
            permissions.setCanEditShifts(pDto.isCanEditShifts());
            permissions.setCanSaveTemplates(pDto.isCanSaveTemplates());
            permissions.setCanPublishSchedules(pDto.isCanPublishSchedules());
            permissions.setCanUnpublishSchedules(pDto.isCanUnpublishSchedules());
            permissions.setCanManageCategories(pDto.isCanManageCategories());
            
            permissions.setCanAddEmployees(pDto.isCanAddEmployees());
            permissions.setCanViewPayRates(pDto.isCanViewPayRates());
            permissions.setCanEditEmployees(pDto.isCanEditEmployees());
            
            permissions.setCanApproveTrades(pDto.isCanApproveTrades());
            permissions.setCanApproveTimeOff(pDto.isCanApproveTimeOff());
            
            permissions.setCanChangeCompanySettings(pDto.isCanChangeCompanySettings());
            permissions.setCanManagePositions(pDto.isCanManagePositions());
            permissions.setCanManageTeamMembers(pDto.isCanManageTeamMembers());
            
            permissions.setCanReceiveManagerNotifications(pDto.isCanReceiveManagerNotifications());
        }
        
        permissionsRepository.save(permissions);

        if (request.isEmailInstructions()) {
            // Logic to send email would go here
            System.out.println("Emailing instructions to " + request.getEmail());
        }

        return user;
    }
}

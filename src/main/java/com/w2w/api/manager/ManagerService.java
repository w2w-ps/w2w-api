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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
        // Enforce Main Manager Security Constraints
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("User must be authenticated to add a manager.");
        }

        String currentUsername = auth.getName();
        User currentUser = loginRepository.findByLoginId(currentUsername)
                .orElseThrow(() -> new AccessDeniedException("Current user not found."));

        // Determine if user is a Main Manager
        boolean isMainManager = false;

        Optional<ManagerPermissions> permsOpt = permissionsRepository.findByUserId(currentUser.getId());
        if (permsOpt.isPresent() && permsOpt.get().isMainManager()) {
            isMainManager = true;
        } else if (currentUser.getRole() != null && "Manager".equalsIgnoreCase(currentUser.getRole().getName())) {
            isMainManager = true;
        }

        if (!isMainManager) {
            throw new AccessDeniedException("Only Main Managers can add additional managers.");
        }

        // 1. Create Employee
        Employee employee = new Employee();
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setCompanyId(request.companyId());
        employee.setStatus("Active");

        employee = employeeRepository.save(employee);

        // 2. Create User
        User user = new User();
        user.setLoginId(request.email());
        user.setPassword(passwordEncoder.encode("Welcome123!")); // In real usage, this might be temporary
        user.setCompanyId(request.companyId());
        user.setEmployee(employee);

        // Find and assign Additonal Manager Role
        UserRole managerRole = roleRepository.findByName("AddManager")
                .orElseThrow(() -> new RuntimeException("AddManager role not found in database."));
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

        // Always force the newly created manager to be an Additional Manager
        permissions.setMainManager(false);

        ManagerPermissionsDto pDto = request.permissions();
        if (pDto != null) {
            permissions.setCanAddShifts(Boolean.TRUE.equals(pDto.canAddShifts()));
            permissions.setCanImportTemplates(Boolean.TRUE.equals(pDto.canImportTemplates()));
            permissions.setCanUploadShifts(Boolean.TRUE.equals(pDto.canUploadShifts()));
            permissions.setCanAutofillShifts(Boolean.TRUE.equals(pDto.canAutofillShifts()));
            permissions.setCanClearSchedules(Boolean.TRUE.equals(pDto.canClearSchedules()));
            permissions.setCanEditShifts(Boolean.TRUE.equals(pDto.canEditShifts()));
            permissions.setCanSaveTemplates(Boolean.TRUE.equals(pDto.canSaveTemplates()));
            permissions.setCanPublishSchedules(Boolean.TRUE.equals(pDto.canPublishSchedules()));
            permissions.setCanUnpublishSchedules(Boolean.TRUE.equals(pDto.canUnpublishSchedules()));
            permissions.setCanManageCategories(Boolean.TRUE.equals(pDto.canManageCategories()));

            permissions.setCanAddEmployees(Boolean.TRUE.equals(pDto.canAddEmployees()));
            permissions.setCanViewPayRates(Boolean.TRUE.equals(pDto.canViewPayRates()));
            permissions.setCanEditEmployees(Boolean.TRUE.equals(pDto.canEditEmployees()));

            permissions.setCanApproveTrades(Boolean.TRUE.equals(pDto.canApproveTrades()));
            permissions.setCanApproveTimeOff(Boolean.TRUE.equals(pDto.canApproveTimeOff()));

            permissions.setCanChangeCompanySettings(Boolean.TRUE.equals(pDto.canChangeCompanySettings()));
            permissions.setCanManagePositions(Boolean.TRUE.equals(pDto.canManagePositions()));
            permissions.setCanManageTeamMembers(Boolean.TRUE.equals(pDto.canManageTeamMembers()));

            permissions.setCanReceiveManagerNotifications(Boolean.TRUE.equals(pDto.canReceiveManagerNotifications()));
        }

        permissionsRepository.save(permissions);

        if (Boolean.TRUE.equals(request.emailInstructions())) {
            // Logic to send email would go here
            System.out.println("Emailing instructions to " + request.email());
        }

        return user;
    }
}

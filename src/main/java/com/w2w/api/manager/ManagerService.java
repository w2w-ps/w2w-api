package com.w2w.api.manager;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.login.EmpType;
import com.w2w.api.login.EmpTypeRepository;
import com.w2w.api.login.InitialAccountPasswordGenerator;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.login.UserRoleRepository;
import com.w2w.api.manager.dto.AddManagerRequest;
import com.w2w.api.manager.dto.ManagerPermissionsDto;
import com.w2w.api.manager.dto.UpdateManagerRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import com.w2w.api.manager.dto.ManagerResponse;

@Service
public class ManagerService {

    private final EmployeeRepository employeeRepository;
    private final LoginRepository loginRepository;
    private final ManagerPermissionsRepository permissionsRepository;
    private final UserRoleRepository roleRepository;
    private final EmpTypeRepository empTypeRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialAccountPasswordGenerator initialAccountPasswordGenerator;

    public ManagerService(EmployeeRepository employeeRepository,
            LoginRepository loginRepository,
            ManagerPermissionsRepository permissionsRepository,
            UserRoleRepository roleRepository,
            EmpTypeRepository empTypeRepository,
            PasswordEncoder passwordEncoder,
            InitialAccountPasswordGenerator initialAccountPasswordGenerator) {
        this.employeeRepository = employeeRepository;
        this.loginRepository = loginRepository;
        this.permissionsRepository = permissionsRepository;
        this.roleRepository = roleRepository;
        this.empTypeRepository = empTypeRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialAccountPasswordGenerator = initialAccountPasswordGenerator;
    }

    private void enforceMainManagerCheck() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("User must be authenticated to manage managers.");
        }

        String currentUsername = auth.getName();
        User currentUser = loginRepository.findByLoginId(currentUsername)
                .orElseThrow(() -> new AccessDeniedException("Current user not found."));

        boolean isMainManager = false;
        Optional<ManagerPermissions> permsOpt = permissionsRepository.findByUserId(currentUser.getId());
        if (permsOpt.isPresent() && permsOpt.get().isMainManager()) {
            isMainManager = true;
        } else if (currentUser.getRole() != null && "Manager".equalsIgnoreCase(currentUser.getRole().getName())) {
            isMainManager = true;
        }

        if (!isMainManager) {
            throw new AccessDeniedException("Only Main Managers can perform this action.");
        }
    }

    @Transactional
    public User addManager(AddManagerRequest request) {
        enforceMainManagerCheck();

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
        user.setPassword(passwordEncoder.encode(initialAccountPasswordGenerator.generate()));
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

        ManagerPermissionsDto permissionsDto = request.permissions();
        if (permissionsDto != null) {
            permissions.setCanAddShifts(Boolean.TRUE.equals(permissionsDto.canAddShifts()));
            permissions.setCanImportTemplates(Boolean.TRUE.equals(permissionsDto.canImportTemplates()));
            permissions.setCanUploadShifts(Boolean.TRUE.equals(permissionsDto.canUploadShifts()));
            permissions.setCanAutofillShifts(Boolean.TRUE.equals(permissionsDto.canAutofillShifts()));
            permissions.setCanClearSchedules(Boolean.TRUE.equals(permissionsDto.canClearSchedules()));
            permissions.setCanEditShifts(Boolean.TRUE.equals(permissionsDto.canEditShifts()));
            permissions.setCanSaveTemplates(Boolean.TRUE.equals(permissionsDto.canSaveTemplates()));
            permissions.setCanPublishSchedules(Boolean.TRUE.equals(permissionsDto.canPublishSchedules()));
            permissions.setCanUnpublishSchedules(Boolean.TRUE.equals(permissionsDto.canUnpublishSchedules()));
            permissions.setCanManageCategories(Boolean.TRUE.equals(permissionsDto.canManageCategories()));

            permissions.setCanAddEmployees(Boolean.TRUE.equals(permissionsDto.canAddEmployees()));
            permissions.setCanViewPayRates(Boolean.TRUE.equals(permissionsDto.canViewPayRates()));
            permissions.setCanEditEmployees(Boolean.TRUE.equals(permissionsDto.canEditEmployees()));

            permissions.setCanApproveTrades(Boolean.TRUE.equals(permissionsDto.canApproveTrades()));
            permissions.setCanApproveTimeOff(Boolean.TRUE.equals(permissionsDto.canApproveTimeOff()));

            permissions.setCanChangeCompanySettings(Boolean.TRUE.equals(permissionsDto.canChangeCompanySettings()));
            permissions.setCanManagePositions(Boolean.TRUE.equals(permissionsDto.canManagePositions()));
            permissions.setCanManageTeamMembers(Boolean.TRUE.equals(permissionsDto.canManageTeamMembers()));

            permissions.setCanReceiveManagerNotifications(
                    Boolean.TRUE.equals(permissionsDto.canReceiveManagerNotifications()));
        }

        permissionsRepository.save(permissions);

        if (Boolean.TRUE.equals(request.emailInstructions())) {
            // Logic to send email would go here
            System.out.println("Emailing instructions to " + request.email());
        }

        return user;
    }

    @Transactional
    public User updateManager(Integer id, UpdateManagerRequest request) {
        enforceMainManagerCheck();

        User user = loginRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        Employee employee = user.getEmployee();
        if (employee != null) {
            employee.setFirstName(request.firstName());
            employee.setLastName(request.lastName());
            employee.setEmail(request.email());
            employeeRepository.save(employee);
        }

        user.setLoginId(request.email());
        user = loginRepository.save(user);

        ManagerPermissions permissions = permissionsRepository.findByUserId(user.getId())
                .orElse(new ManagerPermissions());
        permissions.setUser(user);

        ManagerPermissionsDto permissionsDto = request.permissions();
        if (permissionsDto != null) {
            permissions.setCanAddShifts(Boolean.TRUE.equals(permissionsDto.canAddShifts()));
            permissions.setCanImportTemplates(Boolean.TRUE.equals(permissionsDto.canImportTemplates()));
            permissions.setCanUploadShifts(Boolean.TRUE.equals(permissionsDto.canUploadShifts()));
            permissions.setCanAutofillShifts(Boolean.TRUE.equals(permissionsDto.canAutofillShifts()));
            permissions.setCanClearSchedules(Boolean.TRUE.equals(permissionsDto.canClearSchedules()));
            permissions.setCanEditShifts(Boolean.TRUE.equals(permissionsDto.canEditShifts()));
            permissions.setCanSaveTemplates(Boolean.TRUE.equals(permissionsDto.canSaveTemplates()));
            permissions.setCanPublishSchedules(Boolean.TRUE.equals(permissionsDto.canPublishSchedules()));
            permissions.setCanUnpublishSchedules(Boolean.TRUE.equals(permissionsDto.canUnpublishSchedules()));
            permissions.setCanManageCategories(Boolean.TRUE.equals(permissionsDto.canManageCategories()));

            permissions.setCanAddEmployees(Boolean.TRUE.equals(permissionsDto.canAddEmployees()));
            permissions.setCanViewPayRates(Boolean.TRUE.equals(permissionsDto.canViewPayRates()));
            permissions.setCanEditEmployees(Boolean.TRUE.equals(permissionsDto.canEditEmployees()));

            permissions.setCanApproveTrades(Boolean.TRUE.equals(permissionsDto.canApproveTrades()));
            permissions.setCanApproveTimeOff(Boolean.TRUE.equals(permissionsDto.canApproveTimeOff()));

            permissions.setCanChangeCompanySettings(Boolean.TRUE.equals(permissionsDto.canChangeCompanySettings()));
            permissions.setCanManagePositions(Boolean.TRUE.equals(permissionsDto.canManagePositions()));
            permissions.setCanManageTeamMembers(Boolean.TRUE.equals(permissionsDto.canManageTeamMembers()));

            permissions.setCanReceiveManagerNotifications(
                    Boolean.TRUE.equals(permissionsDto.canReceiveManagerNotifications()));
        }

        permissionsRepository.save(permissions);

        return user;
    }

    @Transactional
    public void deleteManager(Integer id) {
        enforceMainManagerCheck();

        User user = loginRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        // 1. Delete associated manager permissions
        permissionsRepository.findByUserId(id).ifPresent(permissionsRepository::delete);

        // 2. Soft-delete: Reassign to standard Employee role instead of deleting
        UserRole employeeRole = roleRepository.findByName("Employee")
                .orElseThrow(() -> new RuntimeException("Employee role not found in database."));
        user.setRole(employeeRole);

        loginRepository.save(user);
    }

    public List<ManagerResponse> getAdditionalManagersByCompany() {
        enforceMainManagerCheck();
        Integer companyId = CurrentTenant.requireCurrentTenant();
        List<User> addManagers = loginRepository.findByCompanyIdAndRoleName(companyId, "AddManager");

        return addManagers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ManagerResponse mapToResponse(User user) {
        Employee employee = user.getEmployee();
        ManagerPermissions permissions = permissionsRepository.findByUserId(user.getId())
                .orElse(new ManagerPermissions());

        ManagerPermissionsDto permissionsDto = new ManagerPermissionsDto(
                permissions.isMainManager(),
                permissions.isCanAddShifts(),
                permissions.isCanImportTemplates(),
                permissions.isCanUploadShifts(),
                permissions.isCanAutofillShifts(),
                permissions.isCanClearSchedules(),
                permissions.isCanEditShifts(),
                permissions.isCanSaveTemplates(),
                permissions.isCanPublishSchedules(),
                permissions.isCanUnpublishSchedules(),
                permissions.isCanManageCategories(),
                permissions.isCanAddEmployees(),
                permissions.isCanViewPayRates(),
                permissions.isCanEditEmployees(),
                permissions.isCanApproveTrades(),
                permissions.isCanApproveTimeOff(),
                permissions.isCanChangeCompanySettings(),
                permissions.isCanManagePositions(),
                permissions.isCanManageTeamMembers(),
                permissions.isCanReceiveManagerNotifications());

        return new ManagerResponse(
                user.getId(),
                employee != null ? employee.getFirstName() : null,
                employee != null ? employee.getLastName() : null,
                employee != null ? employee.getEmail() : user.getLoginId(),
                employee != null ? employee.getLastLogon() : null,
                user.getRole() != null ? user.getRole().getName() : null,
                permissionsDto);
    }
}

package com.w2w.api.employee;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.dto.EmployeeDetailResponse;
import com.w2w.api.employee.dto.EmployeeRequest;
import com.w2w.api.employee.dto.EmployeeResponse;
import com.w2w.api.config.exception.ResourceNotFoundException;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.model.EmployeeAddress;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.login.EmpType;
import com.w2w.api.login.EmpTypeRepository;
import com.w2w.api.login.InitialAccountPasswordGenerator;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.login.UserRoleRepository;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class EmployeeService {
    private static final String EMPLOYEE_NOT_FOUND_MESSAGE = "Employee not found";

    private final EmployeeRepository employeeRepository;
    private final EmpTypeRepository empTypeRepository;
    private final PositionRepository positionRepository;
    private final LoginRepository loginRepository;
    private final ManagerPermissionsRepository permissionsRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialAccountPasswordGenerator initialAccountPasswordGenerator;

    public EmployeeService(EmployeeRepository employeeRepository, EmpTypeRepository empTypeRepository,
            PositionRepository positionRepository, LoginRepository loginRepository,
            ManagerPermissionsRepository permissionsRepository, UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder, InitialAccountPasswordGenerator initialAccountPasswordGenerator) {
        this.employeeRepository = employeeRepository;
        this.empTypeRepository = empTypeRepository;
        this.positionRepository = positionRepository;
        this.loginRepository = loginRepository;
        this.permissionsRepository = permissionsRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialAccountPasswordGenerator = initialAccountPasswordGenerator;
    }

    public List<EmployeeResponse> getEmployeesByCompany() {
        return employeeRepository.findByCompanyIdAndIsDeletedFalse(TenantContext.getCurrentTenant())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeDetailResponse> getEmployeeDetail(Integer id) {
        return employeeRepository
                .findByEmployeeIdAndCompanyIdAndIsDeletedFalse(id, TenantContext.getCurrentTenant())
                .map(this::mapToDetailResponse);
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findActiveEmployeeForCurrentTenant(Integer employeeId) {
        return employeeRepository.findByEmployeeIdAndCompanyIdAndIsDeletedFalse(
                employeeId,
                CurrentTenant.requireCurrentTenant()
        );
    }

    @Transactional
    public EmployeeResponse saveEmployee(EmployeeRequest request) {
        enforceMainManagerCheck();
        if (request.email() != null && !request.email().isBlank()) {
            validateUniqueEmail(TenantContext.getCurrentTenant(), request.email(), null);
        }

        Employee employee = new Employee();
        mapRequestToEntity(request, employee);
        employee.setCompanyId(CurrentTenant.requireCurrentTenant());
        employee.setStatus("Active");

        Employee saved = employeeRepository.save(employee);

        // Auto-create User account
        User user = new User();
        user.setEmployee(saved);
        user.setCompanyId(saved.getCompanyId());
        user.setEmpType(saved.getEmpType());

        // Use email as loginId if available, otherwise employee.id
        String loginId = (saved.getEmail() != null && !saved.getEmail().isBlank())
                ? saved.getEmail()
                : "employee." + saved.getEmployeeId();
        user.setLoginId(loginId);

        user.setPassword(passwordEncoder.encode(initialAccountPasswordGenerator.generate()));

        // Assign "Employee" role
        UserRole employeeRole = userRoleRepository.findByName("Employee")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Target role 'Employee' not found"));
        user.setRole(employeeRole);

        user.setEncryptionType(0);
        user.setLoginFailures(0);

        loginRepository.save(user);

        return mapToResponse(saved);
    }

    public EmployeeResponse updateEmployee(Integer id, EmployeeRequest request) {
        Employee employee = employeeRepository
                .findByEmployeeIdAndCompanyIdAndIsDeletedFalse(id, CurrentTenant.requireCurrentTenant())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND_MESSAGE));

        if (request.email() != null && !request.email().isBlank()) {
            validateUniqueEmail(TenantContext.getCurrentTenant(), request.email(), id);
        }

        mapRequestToEntity(request, employee);

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    @Transactional
    public EmployeeResponse patchEmployee(Integer id, EmployeeRequest request) {
        enforceMainManagerCheck();
        Employee employee = employeeRepository
                .findByEmployeeIdAndCompanyIdAndIsDeletedFalse(id, CurrentTenant.requireCurrentTenant())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND_MESSAGE));

        if (request.email() != null) {
            validateUniqueEmail(TenantContext.getCurrentTenant(), request.email(), id);
        }

        applyPatchToEntity(request, employee);

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    private void applyPatchToEntity(EmployeeRequest request, Employee employee) {
        applyNamePatch(request, employee);
        applyContactPatch(request, employee);
        applyPhonePatch(request, employee);
        applySchedulingPatch(request, employee);
        applyProfilePatch(request, employee);
        applyRelationshipPatch(request, employee);

        if (request.address() != null) {
            updateAddressPartially(request.address(), employee);
        }
    }

    private void applyNamePatch(EmployeeRequest request, Employee employee) {
        applyIfPresent(request.firstName(), employee::setFirstName);
        applyIfPresent(request.lastName(), employee::setLastName);

        if (request.username() != null) {
            employee.setUsername(request.username());
            return;
        }

        if (request.firstName() != null || request.lastName() != null) {
            employee.setUsername((employee.getFirstName() + employee.getLastName()).toLowerCase());
        }
    }

    private void applyContactPatch(EmployeeRequest request, Employee employee) {
        applyIfPresent(request.email(), employee::setEmail);
        applyIfPresent(request.employeeNumber(), employee::setEmployeeNumber);
    }

    private void applyPhonePatch(EmployeeRequest request, Employee employee) {
        if (request.phone() != null || request.phone2() != null || request.cell() != null) {
            List<String> currentPhones = new ArrayList<>(employee.getPhones());
            while (currentPhones.size() < 3)
                currentPhones.add(null);

            if (request.phone() != null)
                currentPhones.set(0, request.phone());
            if (request.phone2() != null)
                currentPhones.set(1, request.phone2());
            if (request.cell() != null)
                currentPhones.set(2, request.cell());

            employee.setPhones(currentPhones);
        }
    }

    private void applySchedulingPatch(EmployeeRequest request, Employee employee) {
        applyIfPresent(request.hireDate(), employee::setHireDate);
        applyIfPresent(request.maxScheduledHours(), employee::setMaxScheduledHours);
        applyIfPresent(request.maxDailyHours(), employee::setMaxDailyHours);
        applyIfPresent(request.payRate(), employee::setPayRate);
        applyIfPresent(request.maxWeeklyDays(), employee::setMaxWeeklyDays);
        applyIfPresent(request.maxDailyShifts(), employee::setMaxDailyShifts);
        applyIfPresent(request.nextAlertDate(), employee::setNextAlertDate);
    }

    private void applyProfilePatch(EmployeeRequest request, Employee employee) {
        applyIfPresent(request.comments(), employee::setComments);
        applyIfPresent(request.priorityGroup(), employee::setPriorityGroup);
        applyIfPresent(request.googleCalExport(), employee::setGoogleCalExport);
        applyIfPresent(request.customField1(), employee::setCustomField1);
        applyIfPresent(request.customField2(), employee::setCustomField2);
        applyIfPresent(request.employeePhoto(), employee::setEmployeePhoto);
        applyIfPresent(request.accessibilityMode(), employee::setAccessibilityMode);
    }

    private void applyRelationshipPatch(EmployeeRequest request, Employee employee) {
        if (request.positionIds() != null) {
            List<Position> positions = positionRepository.findByPositionIdInAndCompanyId(
                    request.positionIds(), CurrentTenant.requireCurrentTenant());
            employee.setPositions(positions);
        }

        if (request.empTypeId() != null) {
            EmpType empType = empTypeRepository.findById(request.empTypeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid EmpType ID"));
            employee.setEmpType(empType);
        }
    }

    private <T> void applyIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void updateAddressPartially(EmployeeRequest.AddressRequest addressReq, Employee employee) {
        EmployeeAddress address = employee.getAddress();
        if (address == null) {
            address = new EmployeeAddress();
            address.setEmployee(employee);
            employee.setAddress(address);
        }
        if (addressReq.address() != null)
            address.setAddress(addressReq.address());
        if (addressReq.address2() != null)
            address.setAddress2(addressReq.address2());
        if (addressReq.city() != null)
            address.setCity(addressReq.city());
        if (addressReq.state() != null)
            address.setState(addressReq.state());
        if (addressReq.zip() != null)
            address.setZip(addressReq.zip());
    }

    private void validateUniqueEmail(Integer companyId, String email, Integer employeeId) {
        employeeRepository.findByCompanyIdAndEmailAndIsDeletedFalse(companyId, email)
                .ifPresent(existing -> {
                    if (employeeId == null || !existing.getEmployeeId().equals(employeeId)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Email address already in use for this company");
                    }
                });
    }

    private void mapRequestToEntity(EmployeeRequest request, Employee employee) {
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());

        String username = (request.username() != null && !request.username().isBlank())
                ? request.username()
                : (request.firstName() + request.lastName()).toLowerCase();
        employee.setUsername(username);

        employee.setEmail(request.email());
        employee.setEmployeeNumber(request.employeeNumber());
        employee.setPhones(Arrays.asList(request.phone(), request.phone2(), request.cell()));
        employee.setHireDate(request.hireDate());
        employee.setMaxScheduledHours(request.maxScheduledHours());
        employee.setMaxDailyHours(request.maxDailyHours());
        employee.setPayRate(request.payRate());
        employee.setMaxWeeklyDays(request.maxWeeklyDays());
        employee.setMaxDailyShifts(request.maxDailyShifts());
        employee.setComments(request.comments());
        employee.setPriorityGroup(request.priorityGroup());
        employee.setGoogleCalExport(request.googleCalExport() != null ? request.googleCalExport() : false);
        employee.setNextAlertDate(request.nextAlertDate());
        employee.setCustomField1(request.customField1());
        employee.setCustomField2(request.customField2());
        employee.setEmployeePhoto(request.employeePhoto());
        employee.setAccessibilityMode(request.accessibilityMode() != null ? request.accessibilityMode() : false);

        if (request.positionIds() != null) {
            List<Position> positions = positionRepository.findByPositionIdInAndCompanyId(
                    request.positionIds(), CurrentTenant.requireCurrentTenant());
            employee.setPositions(positions);
        }

        if (request.empTypeId() != null) {
            EmpType empType = empTypeRepository.findById(request.empTypeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid EmpType ID"));
            employee.setEmpType(empType);
        }

        if (request.address() != null) {
            EmployeeAddress address = employee.getAddress();
            if (address == null) {
                address = new EmployeeAddress();
                address.setEmployee(employee);
                employee.setAddress(address);
            }
            address.setAddress(request.address().address());
            address.setAddress2(request.address().address2());
            address.setCity(request.address().city());
            address.setState(request.address().state());
            address.setZip(request.address().zip());
        } else {
            employee.setAddress(null);
        }
    }

    public void deleteEmployee(Integer id) {
        Employee employee = employeeRepository
                .findByEmployeeIdAndCompanyIdAndIsDeletedFalse(id, CurrentTenant.requireCurrentTenant())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND_MESSAGE));

        employee.setIsDeleted(true);
        employeeRepository.save(employee);
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        EmployeeResponse.AddressSummary addressSummary = null;
        if (employee.getAddress() != null) {
            addressSummary = new EmployeeResponse.AddressSummary(
                    employee.getAddress().getAddress(),
                    employee.getAddress().getAddress2(),
                    employee.getAddress().getCity(),
                    employee.getAddress().getState(),
                    employee.getAddress().getZip());
        }

        EmployeeResponse.EmpTypeSummary empTypeSummary = null;
        if (employee.getEmpType() != null) {
            empTypeSummary = new EmployeeResponse.EmpTypeSummary(
                    employee.getEmpType().getId(),
                    employee.getEmpType().getName());
        }

        return new EmployeeResponse(
                employee.getEmployeeId(),
                employee.getCompanyId(),
                employee.getStatus(),
                employee.getLastLogon(),
                employee.getLogonCount(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getUsername(),
                employee.getEmail(),
                employee.getEmployeeNumber(),
                getPhone(employee.getPhones(), 0),
                getPhone(employee.getPhones(), 1),
                getPhone(employee.getPhones(), 2),
                employee.getHireDate(),
                employee.getMaxScheduledHours(),
                employee.getMaxDailyHours(),
                employee.getPayRate(),
                empTypeSummary,
                addressSummary,
                employee.getMaxWeeklyDays(),
                employee.getMaxDailyShifts(),
                employee.getComments(),
                employee.getPriorityGroup(),
                employee.getGoogleCalExport(),
                employee.getNextAlertDate(),
                employee.getCustomField1(),
                employee.getCustomField2(),
                employee.getEmployeePhoto(),
                employee.getAccessibilityMode());
    }

    private EmployeeDetailResponse mapToDetailResponse(Employee employee) {
        EmployeeDetailResponse.AddressSummary addressSummary = null;
        if (employee.getAddress() != null) {
            addressSummary = new EmployeeDetailResponse.AddressSummary(
                    employee.getAddress().getAddress(),
                    employee.getAddress().getAddress2(),
                    employee.getAddress().getCity(),
                    employee.getAddress().getState(),
                    employee.getAddress().getZip());
        }

        EmployeeDetailResponse.EmpTypeSummary empTypeSummary = null;
        if (employee.getEmpType() != null) {
            empTypeSummary = new EmployeeDetailResponse.EmpTypeSummary(
                    employee.getEmpType().getId(),
                    employee.getEmpType().getName());
        }

        List<PositionSummary> positions = Optional.ofNullable(employee.getPositions())
                .orElse(List.of())
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(p -> new PositionSummary(p.getPositionId(), p.getDescription()))
                .sorted(Comparator.comparing(PositionSummary::description,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return new EmployeeDetailResponse(
                employee.getEmployeeId(),
                employee.getCompanyId(),
                employee.getStatus(),
                employee.getLastLogon(),
                employee.getLogonCount(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getUsername(),
                employee.getEmail(),
                employee.getEmployeeNumber(),
                getPhone(employee.getPhones(), 0),
                getPhone(employee.getPhones(), 1),
                getPhone(employee.getPhones(), 2),
                employee.getHireDate(),
                employee.getPayRate() != null ? BigDecimal.valueOf(employee.getPayRate()) : null,
                empTypeSummary,
                addressSummary,
                employee.getNextAlertDate(),
                employee.getCustomField1(),
                employee.getCustomField2(),
                employee.getEmployeePhoto(),
                positions,
                employee.getMaxScheduledHours(),
                employee.getMaxDailyHours(),
                employee.getMaxWeeklyDays(),
                employee.getMaxDailyShifts(),
                employee.getPriorityGroup(),
                employee.getComments(),
                employee.getGoogleCalExport(),
                employee.getAccessibilityMode());
    }

    private String getPhone(List<String> phones, int index) {
        if (phones == null || index < 0 || index >= phones.size()) {
            return null;
        }
        return phones.get(index);
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
}

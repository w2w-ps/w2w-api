package com.w2w.api.employee;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.dto.EmployeeDetailResponse;
import com.w2w.api.employee.dto.EmployeeRequest;
import com.w2w.api.employee.dto.EmployeeResponse;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.model.EmployeeAddress;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.login.EmpType;
import com.w2w.api.login.EmpTypeRepository;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmpTypeRepository empTypeRepository;
    private final PositionRepository positionRepository;

    public EmployeeService(EmployeeRepository employeeRepository, EmpTypeRepository empTypeRepository, PositionRepository positionRepository) {
        this.employeeRepository = employeeRepository;
        this.empTypeRepository = empTypeRepository;
        this.positionRepository = positionRepository;
    }

    public List<EmployeeResponse> getEmployeesByCompany() {
        return employeeRepository.findByCompanyIdAndIsDeletedFalse(TenantContext.getCurrentTenant())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeDetailResponse> getEmployeeDetail(Integer id) {
        return employeeRepository
                .findByEmployeeIdAndCompanyIdAndIsDeletedFalse(id, TenantContext.getCurrentTenant())
                .map(this::mapToDetailResponse);
    }

    public EmployeeResponse saveEmployee(EmployeeRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            validateUniqueEmail(TenantContext.getCurrentTenant(), request.email(), null);
        }

        Employee employee = new Employee();
        mapRequestToEntity(request, employee);
        employee.setCompanyId(CurrentTenant.requireCurrentTenant());
        employee.setStatus("Active");

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    public EmployeeResponse updateEmployee(Integer id, EmployeeRequest request) {
        Employee employee = employeeRepository
                .findByEmployeeIdAndCompanyIdAndIsDeletedFalse(id, CurrentTenant.requireCurrentTenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (request.email() != null && !request.email().isBlank()) {
            validateUniqueEmail(TenantContext.getCurrentTenant(), request.email(), id);
        }

        mapRequestToEntity(request, employee);

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    private void validateUniqueEmail(Integer companyId, String email, Integer employeeId) {
        employeeRepository.findByCompanyIdAndEmailAndStatusNot(companyId, email, "Deleted")
                .ifPresent(existing -> {
                    if (employeeId == null || !existing.getEmployeeId().equals(employeeId)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address already in use for this company");
                    }
                });
    }

    private void mapRequestToEntity(EmployeeRequest request, Employee employee) {
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
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
                .collect(Collectors.toList());

        return new EmployeeDetailResponse(
                employee.getEmployeeId(),
                employee.getCompanyId(),
                employee.getStatus(),
                employee.getLastLogon(),
                employee.getLogonCount(),
                employee.getFirstName(),
                employee.getLastName(),
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
}

package com.w2w.api.employee;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.dto.EmployeeRequest;
import com.w2w.api.employee.dto.EmployeeResponse;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.model.EmployeeAddress;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.login.EmpType;
import com.w2w.api.login.EmpTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmpTypeRepository empTypeRepository;

    public EmployeeService(EmployeeRepository employeeRepository, EmpTypeRepository empTypeRepository) {
        this.employeeRepository = employeeRepository;
        this.empTypeRepository = empTypeRepository;
    }

    public List<EmployeeResponse> getEmployeesByCompany() {
        return employeeRepository.findByCompanyIdAndStatusNot(TenantContext.getCurrentTenant(), "Deleted")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<EmployeeResponse> getEmployeeById(Integer id) {
        return employeeRepository.findByEmployeeIdAndCompanyIdAndStatusNot(id, TenantContext.getCurrentTenant(), "Deleted")
                .map(this::mapToResponse);
    }

    public EmployeeResponse saveEmployee(EmployeeRequest request) {
        Employee employee = new Employee();
        mapRequestToEntity(request, employee);
        employee.setCompanyId(CurrentTenant.requireCurrentTenant());
        employee.setStatus("Active");

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    public EmployeeResponse updateEmployee(Integer id, EmployeeRequest request) {
        Employee employee = employeeRepository.findByEmployeeIdAndCompanyIdAndStatusNot(id, CurrentTenant.requireCurrentTenant(), "Deleted")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        mapRequestToEntity(request, employee);

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    private void mapRequestToEntity(EmployeeRequest request, Employee employee) {
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setEmployeeNumber(request.employeeNumber());
        employee.setPhones(request.phones());
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
        Employee employee = employeeRepository.findByEmployeeIdAndCompanyIdAndStatusNot(id, CurrentTenant.requireCurrentTenant(), "Deleted")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        
        employee.setStatus("Deleted");
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
                employee.getAddress().getZip()
            );
        }

        EmployeeResponse.EmpTypeSummary empTypeSummary = null;
        if (employee.getEmpType() != null) {
            empTypeSummary = new EmployeeResponse.EmpTypeSummary(
                employee.getEmpType().getId(),
                employee.getEmpType().getName()
            );
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
            employee.getPhones(),
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
            employee.getEmployeePhoto()
        );
    }
}

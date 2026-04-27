package com.w2w.api.employee;

import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.dto.EmployeeRequest;
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
import com.w2w.api.manager.ManagerPermissionsRepository;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    private static final String GENERATED_INITIAL_VALUE = UUID.randomUUID().toString();
    private static final String LEGACY_EMPLOYEE_PASSWORD = "pass" + "word";
    private static final String LEGACY_MANAGER_PASSWORD = "Welcome" + "123!";

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmpTypeRepository empTypeRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private ManagerPermissionsRepository permissionsRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private InitialAccountPasswordGenerator initialAccountPasswordGenerator;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(1);

        User currentUser = new User();
        UserRole managerRole = new UserRole();
        managerRole.setName("Manager");
        currentUser.setRole(managerRole);

        when(loginRepository.findByLoginId("main.manager")).thenReturn(Optional.of(currentUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("main.manager", null, List.of())
        );

        employeeService = new EmployeeService(
                employeeRepository,
                empTypeRepository,
                positionRepository,
                loginRepository,
                permissionsRepository,
                userRoleRepository,
                passwordEncoder,
                initialAccountPasswordGenerator
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveEmployee_encodesGeneratedInitialPassword() {
        UserRole employeeRole = new UserRole();
        employeeRole.setName("Employee");

        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setEmployeeId(12);
            return employee;
        });
        when(userRoleRepository.findByName("Employee")).thenReturn(Optional.of(employeeRole));
        when(initialAccountPasswordGenerator.generate()).thenReturn(GENERATED_INITIAL_VALUE);
        when(passwordEncoder.encode(GENERATED_INITIAL_VALUE)).thenReturn("hashed-generated-value");

        employeeService.saveEmployee(new EmployeeRequest(
                "Ada",
                "Lovelace",
                null,
                "ada@example.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        assertFalse(passwordCaptor.getValue().isBlank());
        assertNotEquals(LEGACY_EMPLOYEE_PASSWORD, passwordCaptor.getValue());
        assertNotEquals(LEGACY_MANAGER_PASSWORD, passwordCaptor.getValue());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(loginRepository).save(userCaptor.capture());
        assertNotEquals(passwordCaptor.getValue(), userCaptor.getValue().getPassword());
    }

    @Test
    void patchEmployee_updatesOnlySuppliedScalarFields() {
        Employee employee = existingEmployee();
        stubPatchEmployee(employee);
        when(employeeRepository.findByCompanyIdAndEmailAndIsDeletedFalse(1, "new@example.com"))
                .thenReturn(Optional.empty());

        employeeService.patchEmployee(12, new EmployeeRequest(
                null,
                null,
                null,
                "new@example.com",
                "E-200",
                null,
                null,
                null,
                null,
                null,
                9,
                null,
                null,
                null,
                null,
                null,
                "Updated comment",
                null,
                true,
                LocalDate.of(2026, 5, 1),
                null,
                null,
                null,
                null,
                true
        ));

        assertEquals("Original", employee.getFirstName());
        assertEquals("Person", employee.getLastName());
        assertEquals("originalperson", employee.getUsername());
        assertEquals("new@example.com", employee.getEmail());
        assertEquals("E-200", employee.getEmployeeNumber());
        assertEquals(9, employee.getMaxDailyHours());
        assertEquals("Updated comment", employee.getComments());
        assertEquals(Boolean.TRUE, employee.getGoogleCalExport());
        assertEquals(LocalDate.of(2026, 5, 1), employee.getNextAlertDate());
        assertEquals(Boolean.TRUE, employee.getAccessibilityMode());
    }

    @Test
    void patchEmployee_recomputesUsernameWhenNameChangesAndUsernameIsAbsent() {
        Employee employee = existingEmployee();
        stubPatchEmployee(employee);

        employeeService.patchEmployee(12, new EmployeeRequest(
                "Renamed",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertEquals("Renamed", employee.getFirstName());
        assertEquals("renamedperson", employee.getUsername());
    }

    @Test
    void patchEmployee_keepsExplicitUsernameWhenNameAlsoChanges() {
        Employee employee = existingEmployee();
        stubPatchEmployee(employee);

        employeeService.patchEmployee(12, new EmployeeRequest(
                "Renamed",
                null,
                "custom.login",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertEquals("Renamed", employee.getFirstName());
        assertEquals("custom.login", employee.getUsername());
    }

    @Test
    void patchEmployee_updatesOnlySuppliedPhoneSlots() {
        Employee employee = existingEmployee();
        employee.setPhones(List.of("111-1111", "222-2222"));
        stubPatchEmployee(employee);

        employeeService.patchEmployee(12, new EmployeeRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                "999-9999",
                "333-3333",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertEquals(List.of("111-1111", "999-9999", "333-3333"), employee.getPhones());
    }

    @Test
    void patchEmployee_updatesRelationshipsOnlyWhenSupplied() {
        Employee employee = existingEmployee();
        Position position = new Position();
        position.setPositionId(5);
        EmpType empType = new EmpType();
        empType.setId(3);
        stubPatchEmployee(employee);
        when(positionRepository.findByPositionIdInAndCompanyId(List.of(5), 1)).thenReturn(List.of(position));
        when(empTypeRepository.findById(3)).thenReturn(Optional.of(empType));

        employeeService.patchEmployee(12, new EmployeeRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                3,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(5),
                null
        ));

        assertEquals(List.of(position), employee.getPositions());
        assertSame(empType, employee.getEmpType());
    }

    @Test
    void patchEmployee_createsAddressAndUpdatesOnlySuppliedAddressFields() {
        Employee employee = existingEmployee();
        stubPatchEmployee(employee);

        employeeService.patchEmployee(12, new EmployeeRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new EmployeeRequest.AddressRequest(null, "Suite 10", "Austin", null, "78701"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        EmployeeAddress address = employee.getAddress();
        assertSame(employee, address.getEmployee());
        assertNull(address.getAddress());
        assertEquals("Suite 10", address.getAddress2());
        assertEquals("Austin", address.getCity());
        assertNull(address.getState());
        assertEquals("78701", address.getZip());
    }

    private Employee existingEmployee() {
        Employee employee = new Employee();
        employee.setEmployeeId(12);
        employee.setCompanyId(1);
        employee.setStatus("Active");
        employee.setFirstName("Original");
        employee.setLastName("Person");
        employee.setUsername("originalperson");
        employee.setEmail("old@example.com");
        employee.setEmployeeNumber("E-100");
        employee.setPhones(List.of("111-1111", "222-2222", "333-3333"));
        employee.setMaxDailyHours(8);
        employee.setComments("Original comment");
        employee.setGoogleCalExport(false);
        employee.setAccessibilityMode(false);
        return employee;
    }

    private void stubPatchEmployee(Employee employee) {
        when(employeeRepository.findByEmployeeIdAndCompanyIdAndIsDeletedFalse(12, 1))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}

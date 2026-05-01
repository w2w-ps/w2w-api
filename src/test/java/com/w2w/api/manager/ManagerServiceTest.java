package com.w2w.api.manager;

import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.login.InitialAccountPasswordGenerator;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.login.UserRoleRepository;
import com.w2w.api.manager.dto.AddManagerRequest;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {
    private static final String GENERATED_INITIAL_VALUE = UUID.randomUUID().toString();
    private static final String LEGACY_EMPLOYEE_PASSWORD = "pass" + "word";
    private static final String LEGACY_MANAGER_PASSWORD = "Welcome" + "123!";

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private ManagerPermissionsRepository permissionsRepository;

    @Mock
    private UserRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private InitialAccountPasswordGenerator initialAccountPasswordGenerator;

    private ManagerService managerService;

    @BeforeEach
    void setUp() {
        User currentUser = new User();
        UserRole managerRole = new UserRole();
        managerRole.setName("Manager");
        currentUser.setRole(managerRole);

        when(loginRepository.findByLoginId("main.manager")).thenReturn(Optional.of(currentUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("main.manager", null, List.of())
        );

        managerService = new ManagerService(
                employeeRepository,
                loginRepository,
                permissionsRepository,
                roleRepository,
                passwordEncoder,
                initialAccountPasswordGenerator
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addManager_encodesGeneratedInitialPassword() {
        UserRole addManagerRole = new UserRole();
        addManagerRole.setName("AddManager");

        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("AddManager")).thenReturn(Optional.of(addManagerRole));
        when(initialAccountPasswordGenerator.generate()).thenReturn(GENERATED_INITIAL_VALUE);
        when(passwordEncoder.encode(GENERATED_INITIAL_VALUE)).thenReturn("hashed-generated-value");
        when(loginRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        managerService.addManager(new AddManagerRequest(
                "Grace",
                "Hopper",
                "grace@example.com",
                1,
                false,
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
}

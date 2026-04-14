package com.w2w.api.notification;

import com.w2w.api.employee.Employee;
import com.w2w.api.employee.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;

    public NotificationService(EmailService emailService, EmployeeRepository employeeRepository) {
        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
    }

    public void processNotification(NotificationRequest request) {
        String task = request.task();
        if (task == null)
            return;

        if (task.startsWith("leave_")) {
            handleLeaveNotification(request);
        } else if (task.equals("publish") || task.equals("unpublish")) {
            handleScheduleNotification(request);
        }
    }

    private void handleLeaveNotification(NotificationRequest request) {
        // TODO: Get employee details from leave request id
        // Placeholder values for now
        String employeeName = "Employee";
        String intendedEmail = "actual_employee@example.com";
        String leaveDate = "2026-11-11";

        String action = request.task().replace("leave_", "");
        String subject = "W2W: Leave Request " + action.substring(0, 1).toUpperCase() + action.substring(1);

        String body = String.format("Dear %s,\n\nYour leave request for %s has been %s.\n\nBest regards,\nW2W Team",
                employeeName, leaveDate, action);

        try {
            // Print intended recipient to console
            System.out.println("DEBUG: Intended recipient: " + intendedEmail + " [" + employeeName + "]");

            // Redirect to debug email
            String debugEmail = "96mbsb@gmail.com";
            emailService.sendSimpleEmail(debugEmail, subject, body);

            // emailService.sendSimpleEmail(intendedEmail, subject, body);

            log.info("Leave notification [{}] sent for leaveRequestId: {}", request.task(), request.leaveRequestId());
        } catch (Exception e) {
            log.error("Failed to send leave notification: {}", e.getMessage());
        }
    }

    private void handleScheduleNotification(NotificationRequest request) {
        // TODO: Get schedule details (start date, end date, AND target roles) from
        // schedule table using request.scheduleId()
        String startDate = "2026-11-11";
        String endDate = "2026-11-17";

        // Placeholder: Simulating fetching roles from the schedule table
        // List<String> rolesFromTable =
        // scheduleRepository.findRolesForSchedule(request.scheduleId());
        List<String> rolesFromTable = new ArrayList<>();

        List<Employee> targets;
        if (rolesFromTable.isEmpty()) {
            // todo remove and replace this with needed logic while implementing with table
            targets = employeeRepository.findByCompanyId(1);
        } else {
            // todo remove and replace this with needed logic while implementing with table
            targets = employeeRepository.findByCompanyId(1);
        }

        String action = request.task().equals("publish") ? "published" : "unpublished";
        String subject = "W2W: Schedule " + (request.task().equals("publish") ? "Published" : "Unpublished");

        for (Employee employee : targets) {
            String body = String.format(
                    "Dear %s,\n\nThe schedule from %s to %s has been %s.\nPlease log in to the portal to view the details.\n\nBest regards,\nW2W Team",
                    employee.getFirstName(), startDate, endDate, action);

            try {
                // System.out.println("DEBUG: Intended recipient: " + employee.getEmail());

                // Redirect to debug email
                emailService.sendSimpleEmail("96mbsb@gmail.com", subject, body);

                // emailService.sendSimpleEmail(employee.getEmail(), subject, body);
            } catch (Exception e) {
                log.error("Failed to send schedule notification to {}: {}", employee.getEmail(), e.getMessage());
            }
        }
        log.info("Schedule notification [{}] processed for scheduleId: {}, targets: {}", request.task(),
                request.scheduleId(), targets.size());
    }
}

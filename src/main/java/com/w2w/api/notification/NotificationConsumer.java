package com.w2w.api.notification;

import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.Employee;
import com.w2w.api.employee.EmployeeRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;

    public NotificationConsumer(EmailService emailService, EmployeeRepository employeeRepository) {
        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
    }

    @KafkaListener(topics = "notifications-topic", groupId = "notification-group")
    public void consumeNotification(NotificationRequest request) {
        System.out.println("Kafka Consumer received request: " + request);

        Integer tenantId = request.companyId() != null ? request.companyId() : -1;
        TenantContext.setCurrentTenant(tenantId);
        try {
            List<Employee> targets = new ArrayList<>();
            if (request.userIds().contains("ALL_EMPLOYEES")) {
                targets = employeeRepository.findByCompanyId(tenantId);
                log.info("Sending broadcast notification to all {} employees in company {}", targets.size(), tenantId);
            } else {
                for (String identifier : request.userIds()) {
                    Employee e = resolveEmployee(identifier);
                    // Double check companyId if we found the employee bypassingly
                    if (e != null && (e.getCompanyId().equals(tenantId) || tenantId == 0)) {
                        targets.add(e);
                    } else {
                        log.warn("Could not resolve employee for identifier: {} in company {}", identifier, tenantId);
                    }
                }
            }

            for (Employee employee : targets) {
                if (employee.getEmail() != null) {
                    processEmployeeNotification(employee, request);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    private Employee resolveEmployee(String identifier) {
        try {
            // 1. Try numeric ID
            try {
                Integer id = Integer.parseInt(identifier);
                return employeeRepository.findById(id).orElseGet(() ->
                // If numeric ID doesn't exist, it might actually be an Employee Number
                employeeRepository.findByEmployeeNumber(identifier).orElse(null));
            } catch (NumberFormatException nfe) {
                // 2. Try Employee Number directly
                return employeeRepository.findByEmployeeNumber(identifier).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error resolving employee {}: {}", identifier, e.getMessage());
            return null;
        }
    }

    private void processEmployeeNotification(Employee employee, NotificationRequest request) {
        String subject = getSubject(request.type());
        String body = getEmailTemplate(request.type(), employee.getFirstName(), request.message());

        try {
            // Print the actual intended recipient to console
            System.out.println("DEBUG: Intended recipient: " + employee.getEmail() + " [" + employee.getFirstName() + "]");
            
            // Temporarily redirect all emails to this debug address
            String debugEmail = "96mbsb@gmail.com";
            emailService.sendSimpleEmail(debugEmail, subject, body);
            
            // Original code commented out per user request
            // emailService.sendSimpleEmail(employee.getEmail(), subject, body);
            
            log.info("Email redirected to: {} for employee: {}", debugEmail, employee.getEmail());
        } catch (Exception e) {
            log.error("Failed to send redirected email to {}: {}", "96mbsb@gmail.com", e.getMessage());
        }
    }

    private String getSubject(String type) {
        return switch (type) {
            case "TIMEOFF_APPROVAL" -> "W2W: Leave Request Approved";
            case "SCHEDULE_PUBLISHED" -> "W2W: New Schedule Published";
            case "SCHEDULE_UNPUBLISHED" -> "W2W: Schedule Change Notification";
            default -> "W2W: Notification";
        };
    }

    private String getEmailTemplate(String type, String name, String message) {
        List<String> dates = extractDates(message);

        return switch (type) {
            case "TIMEOFF_APPROVAL" -> {
                String dateStr = dates.isEmpty() ? "your requested date" : dates.get(0);
                yield String.format(
                        "Dear %s,\n\nYour leave request for %s has been approved.\n\nBest regards,\nW2W Team",
                        name, dateStr);
            }
            case "SCHEDULE_PUBLISHED" -> {
                String range = dates.size() >= 2
                        ? String.format("from %s to %s", dates.get(0), dates.get(1))
                        : "for the upcoming period";
                yield String.format(
                        "Dear %s,\n\nThis is a formal notification that the schedule %s has been published.\nPlease log in to the portal to view your shifts.\n\nBest regards,\nW2W Team",
                        name, range);
            }
            case "SCHEDULE_UNPUBLISHED" -> String.format(
                    "Dear %s,\n\nThe schedule has been unpublished. Please log in to the portal to check for updates.\n\nBest regards,\nW2W Team",
                    name);
            default -> String.format("Dear %s,\n\nNotification: %s\n\nBest regards,\nW2W Team", name, message);
        };
    }

    private List<String> extractDates(String text) {
        List<String> dates = new ArrayList<>();
        // Matches common date formats like 11/11/2026 or 2026-11-11
        Pattern p = Pattern.compile("(\\d{1,4}[/-]\\d{1,2}[/-]\\d{1,4})");
        Matcher m = p.matcher(text);
        while (m.find()) {
            dates.add(m.group(1));
        }
        return dates;
    }
}

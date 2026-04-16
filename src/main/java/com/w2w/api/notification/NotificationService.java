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

        String heading = "Leave Request Update";
        String message = String.format(
                "Dear %s,<br/><br/>Your leave request for <strong>%s</strong> has been <strong>%s</strong>.<br/><br/>Best regards,<br/>W2W Team",
                employeeName, leaveDate, action);

        String htmlBody = buildHtmlTemplate(heading, message);

        try {
            // Print intended recipient to console
            System.out.println("DEBUG: Intended recipient: " + intendedEmail + " [" + employeeName + "]");

            // Redirect to debug email
            String debugEmail = "96mbsb@gmail.com";
            emailService.sendEmail(debugEmail, subject, htmlBody);

            // emailService.sendEmail(intendedEmail, subject, htmlBody);

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

        String heading = "Schedule Update";

        for (Employee employee : targets) {
            String message = String.format(
                    "Dear %s,<br/><br/>The schedule from <strong>%s</strong> to <strong>%s</strong> has been <strong>%s</strong>.<br/>Please log in to the portal to view the details.<br/><br/>Best regards,<br/>W2W Team",
                    employee.getFirstName(), startDate, endDate, action);

            String htmlBody = buildHtmlTemplate(heading, message);

            try {
                // System.out.println("DEBUG: Intended recipient: " + employee.getEmail());

                // Redirect to debug email
                emailService.sendEmail("96mbsb@gmail.com", subject, htmlBody);

                // emailService.sendEmail(employee.getEmail(), subject, htmlBody);
            } catch (Exception e) {
                log.error("Failed to send schedule notification to {}: {}", employee.getEmail(), e.getMessage());
            }
        }
        log.info("Schedule notification [{}] processed for scheduleId: {}, targets: {}", request.task(),
                request.scheduleId(), targets.size());
    }

    private String buildHtmlTemplate(String heading, String message) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center; color: white; }
                        .header h1 { margin: 0; font-size: 24px; letter-spacing: 1px; color: white; }
                        .content { padding: 40px; color: #333333; line-height: 1.6; }
                        .content h2 { color: #764ba2; margin-top: 0; }
                        .footer { background-color: #f4f7f6; padding: 20px; text-align: center; color: #777777; font-size: 12px; }
                        .btn { display: inline-block; padding: 12px 24px; background-color: #764ba2; color: white !important; text-decoration: none; border-radius: 4px; margin-top: 20px; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>When2Work</h1>
                        </div>
                        <div class="content">
                            <h2>%s</h2>
                            <p>%s</p>
                            <a href="#" class="btn">View in Portal</a>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 When2Work Team. All rights reserved.</p>
                            <p>This is an automated message, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(heading, message);
    }
}

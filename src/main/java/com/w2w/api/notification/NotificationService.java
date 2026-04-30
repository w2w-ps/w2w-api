package com.w2w.api.notification;

import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.scheduling.ScheduleRepository;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.ShiftRepository;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.timeoff.TimeOffRequest;
import com.w2w.api.timeoff.TimeOffRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public static final String TASK_LEAVE_PREFIX = "LEAVE_";
    public static final String TASK_PUBLISH = "PUBLISH";
    public static final String TASK_UNPUBLISH = "UNPUBLISH";
    public static final String TASK_SHIFT_CREATE = "SHIFT_CREATE";

    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;
    private final ScheduleRepository scheduleRepository;
    private final TimeOffRequestRepository timeOffRequestRepository;
    private final ShiftRepository shiftRepository;

    public NotificationService(EmailService emailService,
            EmployeeRepository employeeRepository,
            ScheduleRepository scheduleRepository,
            TimeOffRequestRepository timeOffRequestRepository,
            ShiftRepository shiftRepository) {
        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
        this.scheduleRepository = scheduleRepository;
        this.timeOffRequestRepository = timeOffRequestRepository;
        this.shiftRepository = shiftRepository;
    }

    public void processNotification(NotificationRequest request) {
        String task = request.task();
        if (task == null)
            return;

        if (task.startsWith(TASK_LEAVE_PREFIX)) {
            handleLeaveNotification(request);
        } else if (task.equals(TASK_PUBLISH) || task.equals(TASK_UNPUBLISH)) {
            handleScheduleNotification(request);
        } else if (task.equals(TASK_SHIFT_CREATE)) {
            handleShiftNotification(request);
        }
    }

    private void handleShiftNotification(NotificationRequest request) {
        if (request.shiftId() == null || request.companyId() == null) {
            log.warn("Notification failed - shiftId or companyId is null");
            return;
        }

        ShiftDetailsProjection shift = shiftRepository.findShiftDetailsByShiftIdAndCompanyId(request.shiftId(), request.companyId()).orElse(null);
        if (shift == null) {
            log.warn("Notification failed - Shift not found for id: {}", request.shiftId());
            return;
        }

        Employee employee = employeeRepository.findById(shift.getEmployeeId()).orElse(null);
        if (employee == null) {
            log.warn("Notification failed - Employee not found for id: {}", shift.getEmployeeId());
            return;
        }

        String employeeName = employee.getFirstName();
        String intendedEmail = employee.getEmail();
        
        String shiftDate = shift.getDate() != null ? shift.getDate().toString() : "TBD";
        String startTime = shift.getStartTime() != null ? shift.getStartTime().toString() : "TBD";
        String endTime = shift.getEndTime() != null ? shift.getEndTime().toString() : "TBD";

        String subject = "When2Work: New Shift Assigned";
        String heading = "Shift Update";
        String message = String.format(
                "Dear %s,<br/><br/>You have been assigned a new shift on <strong>%s</strong> from <strong>%s</strong> to <strong>%s</strong>.<br/>Please log in to the portal to view the details.<br/><br/>Best regards,<br/>When2Work Team",
                employeeName, shiftDate, startTime, endTime);

        String htmlBody = buildHtmlTemplate(heading, message);

        try {
            log.debug("Intended recipient: {} [{}]", intendedEmail, employeeName);
            String debugEmail = "96mbsb@gmail.com";
            emailService.sendEmail(debugEmail, subject, htmlBody);
            // emailService.sendEmail(intendedEmail, subject, htmlBody);
            log.info("Shift notification sent for shiftId: {}", request.shiftId());
        } catch (Exception e) {
            log.error("Failed to send shift notification: {}", e.getMessage());
        }
    }

    private void handleLeaveNotification(NotificationRequest request) {
        if (request.leaveRequestId() == null || request.companyId() == null) {
            log.warn("Notification failed - leaveRequestId or companyId is null");
            return;
        }

        TimeOffRequest timeOffRequest = timeOffRequestRepository.findByRequestIdAndCompanyId(request.leaveRequestId(), request.companyId()).orElse(null);
        if (timeOffRequest == null) {
            log.warn("Notification failed - TimeOffRequest not found for id: {}", request.leaveRequestId());
            return;
        }

        Employee employee = employeeRepository.findById(timeOffRequest.getEmployeeId()).orElse(null);
        if (employee == null) {
            log.warn("Notification failed - Employee not found for id: {}", timeOffRequest.getEmployeeId());
            return;
        }

        String employeeName = employee.getFirstName();
        String intendedEmail = employee.getEmail();
        
        String leaveDate = timeOffRequest.getStartDate() != null ? timeOffRequest.getStartDate().toString() : "TBD";
        if (timeOffRequest.getEndDate() != null && !timeOffRequest.getStartDate().equals(timeOffRequest.getEndDate())) {
            leaveDate += " to " + timeOffRequest.getEndDate().toString();
        }

        String actionRaw = request.task().replace(TASK_LEAVE_PREFIX, "");
        String action;
        if ("CREATE".equals(actionRaw)) {
            action = "created";
        } else if ("APPROVE".equals(actionRaw)) {
            action = "approved";
        } else if ("DECLINE".equals(actionRaw)) {
            action = "declined";
        } else if ("CANCEL".equals(actionRaw)) {
            action = "cancelled";
        } else {
            action = actionRaw.toLowerCase();
        }

        String subject = "When2Work: Leave Request " + action.substring(0, 1).toUpperCase() + action.substring(1);

        String heading = "Leave Request Update";
        String message = String.format(
                "Dear %s,<br/><br/>Your leave request for <strong>%s</strong> has been <strong>%s</strong>.<br/><br/>Best regards,<br/>When2Work Team",
                employeeName, leaveDate, action);

        String htmlBody = buildHtmlTemplate(heading, message);

        try {
            // Print intended recipient to console
            log.debug("Intended recipient: {} [{}]", intendedEmail, employeeName);

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
        if (request.scheduleId() == null) {
            log.warn("Notification failed - scheduleId is null");
            return;
        }

        Schedule schedule = scheduleRepository.findById(request.scheduleId()).orElse(null);
        if (schedule == null) {
            log.warn("Notification failed - Schedule not found for id: {}", request.scheduleId());
            return;
        }

        String startDate = schedule.getStartDate() != null ? schedule.getStartDate().toString() : "TBD";
        String endDate = schedule.getStartDate() != null ? schedule.getStartDate().plusDays(6).toString() : "TBD";
        Integer companyId = schedule.getCompanyId();
        log.debug("Processing notification for scheduleId: {}, companyId: {}", request.scheduleId(), companyId);

        List<Employee> targets;
        if (request.positionIds() != null && !request.positionIds().isEmpty()) {
            // Partial publish: Notify only employees with matching positions
            log.debug("Searching for employees with positionIds: {}", request.positionIds());
            targets = employeeRepository.findByPositionIdsAndCompanyId(request.positionIds(), companyId);
        } else {
            // Full publish/unpublish: Notify all employees of the company
            log.debug("Searching for all employees in companyId: {}", companyId);
            targets = employeeRepository.findByCompanyId(companyId);
        }

        log.debug("Found {} target employees.", targets != null ? targets.size() : 0);

        if (targets == null || targets.isEmpty()) {
            log.debug("No emails sent - empty target list.");
            return;
        }

        String action = request.task().equals(TASK_PUBLISH) ? "published" : "unpublished";
        String subject = "When2Work: Schedule " + (request.task().equals(TASK_PUBLISH) ? "Published" : "Unpublished");

        String heading = "Schedule Update";

        // Log and process each intended recipient
        for (Employee employee : targets) {
            log.debug("Sending notification for intended recipient: {} [{}]", employee.getEmail(), employee.getFirstName());

            String message = String.format(
                    "Dear %s,<br/><br/>The schedule from <strong>%s</strong> to <strong>%s</strong> has been <strong>%s</strong>.<br/>Please log in to the portal to view the details.<br/><br/>Best regards,<br/>When2Work Team",
                    employee.getFirstName(), startDate, endDate, action);

            String htmlBody = buildHtmlTemplate(heading, message);

            try {
                // Send separately to debug email for each employee
                String debugEmail = "96mbsb@gmail.com";
                emailService.sendEmail(debugEmail, subject, htmlBody);

                // Original logic to send to the actual employee (commented out per request)
                // emailService.sendEmail(employee.getEmail(), subject, htmlBody);
            } catch (Exception e) {
                log.error("Failed to send schedule notification for {}: {}", employee.getEmail(), e.getMessage());
            }
        }

        log.info("Schedule notification [{}] processed for scheduleId: {}, total targets: {}", request.task(),
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
                        .header { background-color: #2C467C; padding: 30px; text-align: center; color: white; }
                        .header h1 { margin: 0; font-size: 24px; letter-spacing: 1px; color: white; }
                        .content { padding: 40px; color: #333333; line-height: 1.6; }
                        .content h2 { color: #2C467C; margin-top: 0; }
                        .footer { background-color: #f4f7f6; padding: 20px; text-align: center; color: #777777; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <img src="https://whentowork.com/images_sales/w2w_logo_circle_und.png" alt="When2Work Logo" style="height: 60px; margin-bottom: 10px;"/>
                            <h1>When2Work</h1>
                        </div>
                        <div class="content">
                            <h2>%s</h2>
                            <p>%s</p>
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

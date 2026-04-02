package com.w2w.api.manager;

import com.w2w.api.login.User;
import jakarta.persistence.*;

@Entity
@Table(name = "manager_permissions")
public class ManagerPermissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Schedules
    @Column(name = "can_add_shifts")
    private boolean canAddShifts;

    @Column(name = "can_import_templates")
    private boolean canImportTemplates;

    @Column(name = "can_upload_shifts")
    private boolean canUploadShifts;

    @Column(name = "can_autofill_shifts")
    private boolean canAutofillShifts;

    @Column(name = "can_clear_schedules")
    private boolean canClearSchedules;

    @Column(name = "can_edit_shifts")
    private boolean canEditShifts;

    @Column(name = "can_save_templates")
    private boolean canSaveTemplates;

    @Column(name = "can_publish_schedules")
    private boolean canPublishSchedules;

    @Column(name = "can_unpublish_schedules")
    private boolean canUnpublishSchedules;

    @Column(name = "can_manage_categories")
    private boolean canManageCategories;

    // Employees
    @Column(name = "can_add_employees")
    private boolean canAddEmployees;

    @Column(name = "can_view_pay_rates")
    private boolean canViewPayRates;

    @Column(name = "can_edit_employees")
    private boolean canEditEmployees;

    // Trades
    @Column(name = "can_approve_trades")
    private boolean canApproveTrades;

    // Time Off
    @Column(name = "can_approve_time_off")
    private boolean canApproveTimeOff;

    // Company Settings
    @Column(name = "can_change_company_settings")
    private boolean canChangeCompanySettings;

    @Column(name = "can_manage_positions")
    private boolean canManagePositions;

    @Column(name = "can_manage_team_members")
    private boolean canManageTeamMembers;

    // Notifications
    @Column(name = "can_receive_manager_notifications")
    private boolean canReceiveManagerNotifications;

    public ManagerPermissions() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isCanAddShifts() { return canAddShifts; }
    public void setCanAddShifts(boolean canAddShifts) { this.canAddShifts = canAddShifts; }

    public boolean isCanImportTemplates() { return canImportTemplates; }
    public void setCanImportTemplates(boolean canImportTemplates) { this.canImportTemplates = canImportTemplates; }

    public boolean isCanUploadShifts() { return canUploadShifts; }
    public void setCanUploadShifts(boolean canUploadShifts) { this.canUploadShifts = canUploadShifts; }

    public boolean isCanAutofillShifts() { return canAutofillShifts; }
    public void setCanAutofillShifts(boolean canAutofillShifts) { this.canAutofillShifts = canAutofillShifts; }

    public boolean isCanClearSchedules() { return canClearSchedules; }
    public void setCanClearSchedules(boolean canClearSchedules) { this.canClearSchedules = canClearSchedules; }

    public boolean isCanEditShifts() { return canEditShifts; }
    public void setCanEditShifts(boolean canEditShifts) { this.canEditShifts = canEditShifts; }

    public boolean isCanSaveTemplates() { return canSaveTemplates; }
    public void setCanSaveTemplates(boolean canSaveTemplates) { this.canSaveTemplates = canSaveTemplates; }

    public boolean isCanPublishSchedules() { return canPublishSchedules; }
    public void setCanPublishSchedules(boolean canPublishSchedules) { this.canPublishSchedules = canPublishSchedules; }

    public boolean isCanUnpublishSchedules() { return canUnpublishSchedules; }
    public void setCanUnpublishSchedules(boolean canUnpublishSchedules) { this.canUnpublishSchedules = canUnpublishSchedules; }

    public boolean isCanManageCategories() { return canManageCategories; }
    public void setCanManageCategories(boolean canManageCategories) { this.canManageCategories = canManageCategories; }

    public boolean isCanAddEmployees() { return canAddEmployees; }
    public void setCanAddEmployees(boolean canAddEmployees) { this.canAddEmployees = canAddEmployees; }

    public boolean isCanViewPayRates() { return canViewPayRates; }
    public void setCanViewPayRates(boolean canViewPayRates) { this.canViewPayRates = canViewPayRates; }

    public boolean isCanEditEmployees() { return canEditEmployees; }
    public void setCanEditEmployees(boolean canEditEmployees) { this.canEditEmployees = canEditEmployees; }

    public boolean isCanApproveTrades() { return canApproveTrades; }
    public void setCanApproveTrades(boolean canApproveTrades) { this.canApproveTrades = canApproveTrades; }

    public boolean isCanApproveTimeOff() { return canApproveTimeOff; }
    public void setCanApproveTimeOff(boolean canApproveTimeOff) { this.canApproveTimeOff = canApproveTimeOff; }

    public boolean isCanChangeCompanySettings() { return canChangeCompanySettings; }
    public void setCanChangeCompanySettings(boolean canChangeCompanySettings) { this.canChangeCompanySettings = canChangeCompanySettings; }

    public boolean isCanManagePositions() { return canManagePositions; }
    public void setCanManagePositions(boolean canManagePositions) { this.canManagePositions = canManagePositions; }

    public boolean isCanManageTeamMembers() { return canManageTeamMembers; }
    public void setCanManageTeamMembers(boolean canManageTeamMembers) { this.canManageTeamMembers = canManageTeamMembers; }

    public boolean isCanReceiveManagerNotifications() { return canReceiveManagerNotifications; }
    public void setCanReceiveManagerNotifications(boolean canReceiveManagerNotifications) { this.canReceiveManagerNotifications = canReceiveManagerNotifications; }
}

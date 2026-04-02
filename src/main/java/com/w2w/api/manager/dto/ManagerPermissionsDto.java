package com.w2w.api.manager.dto;

public class ManagerPermissionsDto {
    // Schedules
    private boolean canAddShifts;
    private boolean canImportTemplates;
    private boolean canUploadShifts;
    private boolean canAutofillShifts;
    private boolean canClearSchedules;
    private boolean canEditShifts;
    private boolean canSaveTemplates;
    private boolean canPublishSchedules;
    private boolean canUnpublishSchedules;
    private boolean canManageCategories;
    
    // Employees
    private boolean canAddEmployees;
    private boolean canViewPayRates;
    private boolean canEditEmployees;
    
    // Trades
    private boolean canApproveTrades;
    
    // Time Off
    private boolean canApproveTimeOff;
    
    // Company Settings
    private boolean canChangeCompanySettings;
    private boolean canManagePositions;
    private boolean canManageTeamMembers;
    
    // Notifications
    private boolean canReceiveManagerNotifications;

    public ManagerPermissionsDto() {}

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

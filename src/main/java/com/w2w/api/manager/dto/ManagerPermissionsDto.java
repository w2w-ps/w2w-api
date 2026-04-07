package com.w2w.api.manager.dto;

public record ManagerPermissionsDto(
    Boolean isMainManager,
    Boolean canAddShifts,
    Boolean canImportTemplates,
    Boolean canUploadShifts,
    Boolean canAutofillShifts,
    Boolean canClearSchedules,
    Boolean canEditShifts,
    Boolean canSaveTemplates,
    Boolean canPublishSchedules,
    Boolean canUnpublishSchedules,
    Boolean canManageCategories,
    Boolean canAddEmployees,
    Boolean canViewPayRates,
    Boolean canEditEmployees,
    Boolean canApproveTrades,
    Boolean canApproveTimeOff,
    Boolean canChangeCompanySettings,
    Boolean canManagePositions,
    Boolean canManageTeamMembers,
    Boolean canReceiveManagerNotifications
) {}

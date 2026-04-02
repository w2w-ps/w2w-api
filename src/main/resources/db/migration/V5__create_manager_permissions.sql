CREATE TABLE manager_permissions (
    permission_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Schedules
    can_add_shifts BOOLEAN DEFAULT FALSE,
    can_import_templates BOOLEAN DEFAULT FALSE,
    can_upload_shifts BOOLEAN DEFAULT FALSE,
    can_autofill_shifts BOOLEAN DEFAULT FALSE,
    can_clear_schedules BOOLEAN DEFAULT FALSE,
    can_edit_shifts BOOLEAN DEFAULT FALSE,
    can_save_templates BOOLEAN DEFAULT FALSE,
    can_publish_schedules BOOLEAN DEFAULT FALSE,
    can_unpublish_schedules BOOLEAN DEFAULT FALSE,
    can_manage_categories BOOLEAN DEFAULT FALSE,
    
    -- Employees
    can_add_employees BOOLEAN DEFAULT FALSE,
    can_view_pay_rates BOOLEAN DEFAULT FALSE,
    can_edit_employees BOOLEAN DEFAULT FALSE,
    
    -- Trades
    can_approve_trades BOOLEAN DEFAULT FALSE,
    
    -- Time Off
    can_approve_time_off BOOLEAN DEFAULT FALSE,
    
    -- Company Settings
    can_change_company_settings BOOLEAN DEFAULT FALSE,
    can_manage_positions BOOLEAN DEFAULT FALSE,
    can_manage_team_members BOOLEAN DEFAULT FALSE,
    
    -- Notifications
    can_receive_manager_notifications BOOLEAN DEFAULT FALSE
);

-- Add is_main_manager flag to manager_permissions table
ALTER TABLE manager_permissions ADD COLUMN is_main_manager BOOLEAN DEFAULT FALSE;

-- Make the default seeded admin a main manager
UPDATE manager_permissions SET is_main_manager = TRUE WHERE user_id = (SELECT user_id FROM users WHERE user_login_id = 'admin');

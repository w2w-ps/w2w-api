DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'w2wuser_app') THEN
        CREATE ROLE w2wuser_app
            LOGIN
            PASSWORD 'w2wpass_pass'
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            INHERIT
            NOREPLICATION;
    ELSE
        ALTER ROLE w2wuser_app
            WITH LOGIN
            PASSWORD 'w2wpass_pass'
            NOSUPERUSER
            NOCREATEDB
            NOCREATEROLE
            INHERIT
            NOREPLICATION;
    END IF;
END $$;

GRANT CONNECT ON DATABASE w2wdb TO w2wuser_app;
GRANT USAGE ON SCHEMA public TO w2wuser_app;

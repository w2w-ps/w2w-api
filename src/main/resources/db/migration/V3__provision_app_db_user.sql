DO $$
BEGIN
  IF '${app.db.user}' <> '${flyway.db.user}' THEN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${app.db.user}') THEN
      EXECUTE format(
        'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOREPLICATION',
        '${app.db.user}',
        '${app.db.password}'
      );
    ELSE
      EXECUTE format(
        'ALTER ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOREPLICATION',
        '${app.db.user}',
        '${app.db.password}'
      );
    END IF;

    EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), '${app.db.user}');
    EXECUTE format('REVOKE CREATE ON SCHEMA public FROM %I', '${app.db.user}');
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM %I', '${app.db.user}');
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM %I', '${app.db.user}');
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM %I', '${app.db.user}');

    EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', '${app.db.user}');
    EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I', '${app.db.user}');
    EXECUTE format('GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO %I', '${app.db.user}');
    EXECUTE format('GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO %I', '${app.db.user}');

    EXECUTE format(
      'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
      '${flyway.db.user}',
      '${app.db.user}'
    );
    EXECUTE format(
      'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO %I',
      '${flyway.db.user}',
      '${app.db.user}'
    );
    EXECUTE format(
      'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT EXECUTE ON FUNCTIONS TO %I',
      '${flyway.db.user}',
      '${app.db.user}'
    );
  END IF;
END $$;

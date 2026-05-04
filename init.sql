-- Create the restricted read-only user
CREATE USER readonly_user WITH PASSWORD 'readonly_secret';

-- Grant connection access to the database
GRANT CONNECT ON DATABASE mydatabase TO readonly_user;

-- Grant usage on the public schema
GRANT USAGE ON SCHEMA public TO readonly_user;

-- Grant SELECT permission on all EXISTING tables
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;

-- Ensure SELECT permission is automatically applied to FUTURE tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readonly_user;
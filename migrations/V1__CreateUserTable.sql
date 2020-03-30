CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

CREATE TABLE "user" (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text NOT NULL UNIQUE,
  auth0_id text NOT NULL UNIQUE,
  created_at timestamp with time zone DEFAULT now() NOT NULL
);

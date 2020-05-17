CREATE TABLE gift_list (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_by_id uuid NOT NULL REFERENCES "user" (id),
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  name text NOT NULL
);

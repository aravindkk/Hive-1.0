CREATE TABLE public.fcm_tokens (
  id         uuid        NOT NULL DEFAULT gen_random_uuid(),
  user_id    uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  token      text        NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fcm_tokens_pkey PRIMARY KEY (id),
  CONSTRAINT fcm_tokens_user_token_unique UNIQUE (user_id, token)
);

CREATE INDEX fcm_tokens_user_id_idx ON public.fcm_tokens(user_id);

ALTER TABLE public.fcm_tokens ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users manage own tokens"
  ON public.fcm_tokens FOR ALL TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

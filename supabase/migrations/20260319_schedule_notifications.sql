-- Weekly reflection: Sunday 09:00 IST = Sunday 03:30 UTC
SELECT cron.schedule(
  'weekly-reflection',
  '30 3 * * 0',
  $$ SELECT net.http_post(
       url := current_setting('app.supabase_url') || '/functions/v1/send-weekly-reflection',
       headers := jsonb_build_object('Authorization', 'Bearer ' || current_setting('app.service_role_key')),
       body := '{}'::jsonb
     ); $$
);

-- Re-engagement: Wednesday 10:00 AM IST = Wednesday 04:30 UTC
SELECT cron.schedule(
  'reengagement-wednesday',
  '30 4 * * 3',
  $$ SELECT net.http_post(
       url := current_setting('app.supabase_url') || '/functions/v1/send-reengagement-notification',
       headers := jsonb_build_object('Authorization', 'Bearer ' || current_setting('app.service_role_key')),
       body := '{}'::jsonb
     ); $$
);

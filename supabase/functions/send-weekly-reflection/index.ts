import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient, SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.21.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_KEY  = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

async function getFcmAccessToken(): Promise<{ token: string; projectId: string }> {
  const sa = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT") ?? "{}");
  const now = Math.floor(Date.now() / 1000);

  const encodeB64Url = (data: string) =>
    btoa(data).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");

  const header  = encodeB64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = encodeB64Url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));

  const unsigned = `${header}.${payload}`;

  const pemContents = sa.private_key
    .replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\n/g, "");
  const binaryDer = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0));

  const key = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const sig = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned)
  );
  const sigB64 = btoa(String.fromCharCode(...new Uint8Array(sig)))
    .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");

  const jwt = `${unsigned}.${sigB64}`;

  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const { access_token } = await tokenRes.json();
  return { token: access_token, projectId: sa.project_id };
}

async function sendFcmToUser(
  supabase: SupabaseClient,
  userId: string,
  payload: Record<string, string>,
  accessToken: string,
  projectId: string
) {
  const { data: tokens } = await supabase
    .from("fcm_tokens")
    .select("token")
    .eq("user_id", userId);
  if (!tokens?.length) return;

  await Promise.allSettled(
    tokens.map((row: { token: string }) =>
      fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${accessToken}`,
        },
        body: JSON.stringify({
          message: {
            token: row.token,
            data: payload,
            android: { priority: "high" },
          },
        }),
      })
    )
  );
}

serve(async (_req) => {
  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);
  const { data: tokenRows } = await supabase.from("fcm_tokens").select("user_id");
  const userIds = [...new Set(tokenRows?.map((r: { user_id: string }) => r.user_id) ?? [])];

  const payload = {
    type: "weekly_reflection",
    title: "Your week in Hive",
    body: "Your week in review is ready — see what your neighbourhood was talking about.",
    deep_link: "hive://timeline",
  };

  const { token: accessToken, projectId } = await getFcmAccessToken();
  for (const userId of userIds) {
    await sendFcmToUser(supabase, userId as string, payload, accessToken, projectId);
  }
  return new Response("ok");
});

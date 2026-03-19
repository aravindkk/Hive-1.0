# Account Persistence on Reinstall — Analysis & Fix

## Why It Breaks

The anonymous auth system derives credentials entirely from the username:

```
email    = "{username}@hive.anonymous"
password = "hive_password_{username}"
```

The username is generated once, then stored in `SharedPreferences` via `PreferenceManager.saveLastGeneratedId()`. On the next launch, `loadStoredId()` reads it back and auto-logs in.

**The problem:** `SharedPreferences` lives at `/data/data/com.example.tester2/shared_prefs/` — Android wipes this directory on uninstall. On reinstall, `getLastGeneratedId()` returns `null`, `generateNewId()` runs, and the user gets a completely new identity with no link to their old account or history.

Version upgrades via the Play Store do NOT trigger this — app data is preserved. The issue is specific to:
- Uninstall + reinstall
- Installing a new build by sideloading after wiping the previous one ("Install anyway" replacing an existing install does not wipe data, but a manual uninstall first does)

---

## Fix Options

### Option A — Android Auto Backup (passive, covers most cases)

Android's Auto Backup silently backs up `SharedPreferences` to the user's Google Drive and restores it on reinstall. It requires zero UI work and no backend changes.

**How to enable:** uncomment one line in `res/xml/backup_rules.xml`:
```xml
<full-backup-content>
    <include domain="sharedpref" path="hive_prefs.xml" />
</full-backup-content>
```

This backs up the entire `hive_prefs` file, including the username and last login timestamp.

**Limitations:**
- Requires the user to have Google backup enabled in Android settings (most users do)
- Backup is not instantaneous — Google syncs it periodically (usually within 24h of the last backup)
- Does not help if the user switches to a new phone without restoring from backup, or if they have backup disabled

**Verdict:** Simple to implement, solves the most common case (accidental uninstall/reinstall on same device), but not a complete solution.

---

### Option B — Manual Username Recovery UI (active fallback)

Add a "Recover existing account" entry point on the auth screen. Since the password is deterministically derived from the username, if the user remembers their handle they can sign back in immediately.

**Flow:**
1. Auth screen → "Had an account? Enter your username"
2. User types e.g. `sunkite`
3. App constructs `sunkite@hive.anonymous` / `hive_password_sunkite` and calls `repository.signIn()`
4. On success, saves the username back to `SharedPreferences` and navigates home

**Limitations:**
- Requires the user to remember their 2-syllable handle (many won't)
- No way to look up a forgotten username (no email, no phone number on the account)

**Verdict:** Good as an explicit escape hatch, poor as the primary recovery mechanism.

---

### Option C — Google Sign-In (already planned, Phase 3)

Link the anonymous Supabase account to a Google identity. On reinstall, sign in with Google → the same Supabase user ID is recovered. This is the only option that is fully reliable across devices and backup states.

**Verdict:** The right long-term solution. Already in the roadmap as item 30. Blocks on the Credential Manager API + Supabase GoTrue OAuth integration.

---

## Recommended Fix

**Immediate (low effort, high coverage):** Implement Option A.
Enable Auto Backup for `hive_prefs.xml`. This silently handles the most common case — someone uninstalls and reinstalls on the same phone — with no UI changes and no backend work.

**Also add Option B** as a visible escape hatch on the auth screen so power users who know their username can self-recover without contacting support.

**Long-term:** Option C (Google Sign-In) supersedes both once implemented. At that point, the anonymous account can be upgraded to a linked account, making the username the display name rather than the authentication credential.

---

## Implementation Effort

| Option | Code changes | Backend changes | Effort |
|---|---|---|---|
| A — Auto Backup | 1 line in `backup_rules.xml` | None | ~5 min |
| B — Recovery UI | New composable + `AuthViewModel.recoverAccount()` | None | ~2 hours |
| C — Google Sign-In | Credential Manager, Supabase OAuth | Google Cloud OAuth config | Phase 3 |

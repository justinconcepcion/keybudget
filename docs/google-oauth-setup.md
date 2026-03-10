# Google OAuth 2.0 Setup for Local Development

How to configure Google Sign-In for KeyBudget running locally.
Total time: ~10 minutes.

---

## 1. Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Click the project dropdown (top-left, next to "Google Cloud") and select **New Project**.
3. Name it `keybudget-dev` (or anything you like). Leave organization as "No organization."
4. Click **Create**, then select the new project from the dropdown.

## 2. Configure the OAuth Consent Screen

1. In the left sidebar, go to **APIs & Services > OAuth consent screen**.
2. Select **External** as the user type and click **Create**.
   - For a personal dev project, External is the only option for free Gmail accounts.
3. Fill in the required fields:
   - **App name**: `KeyBudget (Dev)`
   - **User support email**: your Gmail address
   - **Developer contact email**: your Gmail address
4. Click **Save and Continue** through the remaining steps (Scopes, Test Users, Summary).
   - You do not need to add scopes on this screen; they are requested by the app at runtime.
5. On the **Test Users** page, click **+ Add Users** and add your own Gmail address.
   - While in "Testing" mode, only listed test users can log in. This is fine for personal dev.

> **Shortcut for personal projects**: You never need to submit for Google verification.
> Leave the app in "Testing" publishing status permanently.

## 3. Create OAuth 2.0 Client ID Credentials

1. Go to **APIs & Services > Credentials**.
2. Click **+ Create Credentials > OAuth client ID**.
3. Set **Application type** to **Web application**.
4. **Name**: `KeyBudget Local Dev` (cosmetic only).
5. Under **Authorized JavaScript origins**, add:
   ```
   http://localhost:8080
   ```
6. Under **Authorized redirect URIs**, add:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
   This is Spring Security's default redirect URI for the Google OAuth2 provider.
7. Click **Create**.

## 4. Copy Client ID and Client Secret

After creation, a dialog shows your **Client ID** and **Client Secret**.
Copy both values. You can also retrieve them later from the Credentials page.

## 5. Configure Spring Boot

Edit `backend/src/main/resources/application-dev.properties` and replace the placeholders:

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID_HERE
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET_HERE
spring.security.oauth2.client.registration.google.scope=openid,email,profile
```

The three scopes provide:
- `openid` — required for OpenID Connect (enables ID token)
- `email` — user's email address
- `profile` — user's name and profile picture

> **Do not commit real credentials.** The `application-dev.properties` file has `REPLACE_ME`
> placeholders in version control. If you want to avoid accidental commits, you can use
> environment variables instead:
> ```properties
> spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
> spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
> ```

## 6. Verify It Works

1. Start the backend: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
2. Open `http://localhost:8080/oauth2/authorization/google` in your browser.
3. You should be redirected to Google's sign-in page.
4. After signing in, you should be redirected back to the app.

## Common Gotchas

| Issue | Fix |
|---|---|
| **`redirect_uri_mismatch` error** | The redirect URI in Google Console must exactly match `http://localhost:8080/login/oauth2/code/google` — no trailing slash, correct port, `http` not `https`. |
| **`localhost` vs `127.0.0.1`** | Google treats these as different origins. Use `localhost` consistently (both in browser URL and in Google Console). |
| **Port mismatch** | KeyBudget backend runs on port 8080 by default. If you changed `server.port`, update the redirect URI to match. |
| **"Access blocked: This app's request is invalid"** | You likely forgot to add the redirect URI, or the consent screen is not configured. |
| **"Google hasn't verified this app" warning** | Expected in Testing mode. Click **Continue** to proceed. Only test users you added can see this. |
| **403 after sign-in** | Make sure your Gmail is listed as a test user on the OAuth consent screen. |
| **Vue frontend on port 5173** | The OAuth flow goes through the Spring Boot backend (8080), not the Vue dev server. The frontend calls the backend's OAuth endpoint. No Google Console changes needed for port 5173. |
| **`redirect_uri` override in properties** | Do NOT set `redirect-uri` in application properties — Spring Security's default template `{baseUrl}/login/oauth2/code/{registrationId}` auto-resolves to the correct backend port (8080). Overriding it to port 5173 causes a `redirect_uri_mismatch` error. |

## Reference

- Google Cloud Console: https://console.cloud.google.com/
- Spring Security OAuth2 Client docs: https://docs.spring.io/spring-security/reference/servlet/oauth2/login.html

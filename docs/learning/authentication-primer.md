# Authentication primer for Biddly

What to learn before Challenge 11, in the order you'll need it. Each step lists the questions you should be able to answer and one or two good sources. Steps 1-3 matter before 11a, 4-5 before 11b and 11c, and 6 before 11d.

## 1. The vocabulary (about 1 hour)

- **Authentication** is "who are you?" **Authorization** is "are you allowed to do this?"
- **`401 Unauthorized`** really means "not authenticated: I don't know who you are". **`403 Forbidden`** means "I know who you are, and you're not allowed".
- **Sessions vs tokens:** a server-side session keeps state on the server and gives the browser a session ID cookie. A token (like a JWT) carries the facts itself, so the server can check it without looking anything up.

Be able to answer: why does Biddly use tokens, given that the roadmap runs several backend copies behind a load balancer?

## 2. Cookies and browser security (about 2 hours)

- Cookie flags: **`HttpOnly`** (JavaScript can't read it), **`Secure`** (HTTPS only), **`SameSite`** (`Strict`, `Lax`, `None`), `Path`, `Max-Age`.
- **XSS** (an attacker's script runs in your page) vs **CSRF** (another site makes your browser send a request, with your cookies attached).
- **CORS:** why the Vite proxy (`/api` → `localhost:8081`) means Biddly has no CORS problem in development.

Be able to answer: why keep the access token in memory and the refresh token in an `HttpOnly` cookie, rather than both in `localStorage`?

Source: [MDN: Using HTTP cookies](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Cookies).

## 3. Password storage (about 1 hour)

- **Hashing vs encryption:** encryption can be reversed with the key; a hash can't be reversed at all.
- **Why not SHA-256:** it's built to be fast, so attackers can try billions of guesses per second. **BCrypt** is deliberately slow, and its *work factor* makes it slower as computers get faster.
- **Salt:** a random value stored inside each hash, so two users with the same password get different hashes and precomputed tables are useless.
- BCrypt only uses the first **72 bytes** of a password (rule R28).
- **Account enumeration:** why "Wrong email or password" is safer than "No account with that email".

Source: [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).

## 4. JSON Web Tokens (about 2 hours)

- A JWT has three base64url parts: `header.payload.signature`. Paste one into [jwt.io](https://jwt.io) and read it: anyone can, because it's **signed, not encrypted**. Never put secrets in it.
- Standard claims: `sub` (who), `iat` (issued at), `exp` (expires), `iss` (issuer), `aud` (audience: who the token is for).
- **HS256** signs with one shared secret (Biddly signs and checks its own tokens). **RS256** signs with a private key and is checked with a public key (Google signs; everyone can check).
- A JWT can't be taken back before it expires. Hence **short access tokens** (15 minutes) plus a **refresh token** stored in the database that *can* be revoked, and **rotation**: every refresh swaps the old refresh token for a new one.

Be able to answer: a user signs out. Their access token is still valid for 10 more minutes. Is that acceptable, and what limits the damage?

Sources: [jwt.io introduction](https://jwt.io/introduction), [OWASP JWT Cheat Sheet for Java](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html).

## 5. Spring Security (about 3 hours, the biggest step)

- **The filter chain:** every request passes through a list of filters *before* reaching your controller. One of them reads the JWT and decides who the caller is.
- **`SecurityFilterChain`** bean: which URLs are public (`permitAll`) and which need sign-in (`authenticated`).
- **`SecurityContext`** and **`Authentication`:** where "the current user" lives during a request. `@AuthenticationPrincipal` hands it to a controller method.
- **`PasswordEncoder`** (`BCryptPasswordEncoder`), and the **resource server** support, which checks JWTs for you (`oauth2ResourceServer().jwt()`).
- Stateless setup: no server sessions, and why CSRF protection can be turned off for a pure token API, but needs a second look for the refresh cookie endpoint.

Be able to answer: when a request without a token hits a protected endpoint, which component answers `401`, and does your controller run at all?

Sources: [Spring Security architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html), [Spring Security: OAuth 2.0 resource server with JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## 6. OAuth 2.0, OpenID Connect and Sign in with Google (about 2-3 hours)

- **OAuth 2.0** is about *access*: "let this app read my Google Drive". **OpenID Connect (OIDC)** adds *identity* on top: "tell this app who I am". Sign in with Google is OIDC.
- An **ID token** is a JWT from Google, meant for **your app** (`aud` = your client ID), that says who the user is. An **access token** is for calling Google's APIs. Biddly only needs the ID token.
- **Verifying an ID token:** Google's signature (public keys from Google's JWKS URL, which rotate), `iss`, `aud`, `exp`, and `email_verified`. Skip the `aud` check, and a token issued to *any other* website would let its users into Biddly.
- **`sub` vs email:** `sub` is Google's permanent ID for the account. Emails can change and can be reused, so they're not a safe identifier.
- **Account pre-hijacking:** someone registers your email with their password before you ever use Biddly. If Google sign-in later links to that account automatically, you share an account with them. This is Product decision 7.
- The **authorization code flow** (redirects, `client_secret`) is how server-side apps do it. Biddly uses the simpler **ID token flow** with the Google button, which needs no client secret. Know both for interviews.

Sources: [Google: Sign in with Google overview](https://developers.google.com/identity/gsi/web/guides/overview), [Google: Verify the Google ID token on your server side](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token), [OAuth 2.0 Simplified](https://www.oauth.com/).

## Interview one-liners to be able to say by the end

- "Passwords are BCrypt-hashed with a per-user salt; we never store or log them."
- "The access token is a short-lived JWT kept in memory; the refresh token is an HttpOnly cookie stored hashed in the database and rotated on every use."
- "The server takes the user from the token, never from the request body, and checks ownership on every write."
- "For Google sign-in we verify the ID token's signature, issuer, audience and expiry, match the account on `sub`, and then issue our own session."

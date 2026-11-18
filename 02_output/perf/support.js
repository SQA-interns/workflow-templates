// Shared setup of the k6 scripts (NFR-01 … NFR-03): emulator accounts, ID tokens and Members of
// "Konferenca 2025". Local stack only: the Firebase Auth emulator accepts any API key and the
// password of scripts/seed-local.sh.
import http from "k6/http";

export const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8090";
export const EMULATOR_URL = __ENV.EMULATOR_URL || "http://127.0.0.1:9199";
export const GROUP_NAME = "Konferenca 2025";
const PASSWORD = "Kontakti#2026";
const IDENTITY = `${EMULATOR_URL}/identitytoolkit.googleapis.com/v1`;
const JSON_HEADERS = { "Content-Type": "application/json" };

/** POST to the emulator; fails the run on an unexpected answer. */
function emulator(path, body, headers = {}) {
  const res = http.post(`${IDENTITY}${path}`, JSON.stringify(body), {
    headers: { ...JSON_HEADERS, ...headers },
  });
  if (res.status !== 200) {
    throw new Error(`emulator ${path}: ${res.status}`);
  }
  return res.json();
}

/** An ID token of the e-mail/password account; creates it (e-mail verified) when missing. */
export function signIn(email, displayName) {
  const credentials = { email, password: PASSWORD, returnSecureToken: true };
  let res = http.post(
    `${IDENTITY}/accounts:signInWithPassword?key=fake-api-key`,
    JSON.stringify(credentials),
    { headers: JSON_HEADERS },
  );
  if (res.status !== 200) {
    const created = emulator("/accounts:signUp?key=fake-api-key", credentials);
    emulator(
      "/projects/demo-contacts/accounts:update",
      { localId: created.localId, emailVerified: true, displayName },
      { Authorization: "Bearer owner" },
    );
    res = http.post(
      `${IDENTITY}/accounts:signInWithPassword?key=fake-api-key`,
      JSON.stringify(credentials),
      { headers: JSON_HEADERS },
    );
  }
  return res.json().idToken;
}

/** A request to the Contacts API as the token's User. */
export function api(method, path, token, body, params = {}) {
  return http.request(
    method,
    `${BASE_URL}/api/v1${path}`,
    body ? JSON.stringify(body) : null,
    {
      ...params,
      headers: {
        ...JSON_HEADERS,
        Authorization: `Bearer ${token}`,
        ...(params.headers || {}),
      },
    },
  );
}

/** The id of "Konferenca 2025" as Ana sees it (seed/seed.sql). */
export function groupIdOf(token) {
  const res = api("GET", "/groups", token);
  const group = res.json().find((g) => g.name === GROUP_NAME);
  if (!group) {
    throw new Error(
      `no Group ${GROUP_NAME}: load seed/seed.sql and perf/seed-10k.sql first`,
    );
  }
  return group.id;
}

/**
 * Tokens of `count` Viewers of the Group, one per virtual User, so that each stays under the rate
 * limit of 100 requests a minute (SR-03). They join through Invitations (R-004-01), as every
 * Member does; a User who is already a Member keeps the Membership.
 */
export function viewers(count, groupId, ownerToken) {
  const tokens = [];
  for (let i = 1; i <= count; i++) {
    const email = `obremenitev${String(i).padStart(2, "0")}@example.si`;
    const token = signIn(email, `Obremenitev ${i}`);
    api("GET", "/me", token);
    const invited = api("POST", `/groups/${groupId}/invitations`, ownerToken, {
      email,
      role: "VIEWER",
    });
    if (invited.status === 201) {
      const link = invited.json().link;
      api(
        "POST",
        `/invitations/${link.substring(link.lastIndexOf("/") + 1)}/accept`,
        token,
      );
    }
    tokens.push(token);
  }
  return tokens;
}

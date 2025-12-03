import assert from "node:assert/strict";
import test from "node:test";

import {
  STEP_UP_SECONDS,
  createStepUpMarker,
  decodeStepUpToken,
  isStepUpTokenUsable,
  verifyStepUpMarker
} from "../lib/server/step-up";

const SECRET = "test-session-secret";
const NOW = 1_800_000_000;

function jwt(payload: Record<string, unknown>): string {
  const header = Buffer.from(JSON.stringify({ alg: "HS256", typ: "JWT" })).toString("base64url");
  const body = Buffer.from(JSON.stringify(payload)).toString("base64url");
  return `${header}.${body}.signature`;
}

test("a marker signed with the session secret validates for the same user", () => {
  const marker = createStepUpMarker("42", NOW, SECRET);

  assert.equal(verifyStepUpMarker(marker, "42", NOW, SECRET), true);
});

test("a marker expires and cannot be reused after the five-minute window", () => {
  const marker = createStepUpMarker("42", NOW, SECRET);

  assert.equal(verifyStepUpMarker(marker, "42", NOW + STEP_UP_SECONDS + 1, SECRET), false);
});

test("a marker is bound to the user and to the session secret", () => {
  const marker = createStepUpMarker("42", NOW, SECRET);

  assert.equal(verifyStepUpMarker(marker, "7", NOW, SECRET), false);
  assert.equal(verifyStepUpMarker(marker, "42", NOW, "another-secret"), false);
  assert.equal(verifyStepUpMarker(`${marker}x`, "42", NOW, SECRET), false);
});

test("a valid step-up JWT is usable for the same user", () => {
  const token = jwt({ sub: "42", scope: "step-up", iat: NOW, exp: NOW + STEP_UP_SECONDS });

  assert.equal(isStepUpTokenUsable(token, "42", NOW), true);
});

test("an expired, mis-scoped, or mis-addressed step-up JWT is not usable", () => {
  const expired = jwt({ sub: "42", scope: "step-up", iat: NOW - 600, exp: NOW - 300 });
  const access = jwt({ sub: "42", scope: "access", iat: NOW, exp: NOW + 120 });
  const otherUser = jwt({ sub: "7", scope: "step-up", iat: NOW, exp: NOW + 120 });
  const tooLong = jwt({ sub: "42", scope: "step-up", iat: NOW, exp: NOW + 600 });

  assert.equal(isStepUpTokenUsable(expired, "42", NOW), false);
  assert.equal(isStepUpTokenUsable(access, "42", NOW), false);
  assert.equal(isStepUpTokenUsable(otherUser, "42", NOW), false);
  assert.equal(isStepUpTokenUsable(tooLong, "42", NOW), false);
});

test("malformed token payloads decode to null", () => {
  assert.equal(decodeStepUpToken(undefined), null);
  assert.equal(decodeStepUpToken("not-a-jwt"), null);
  assert.equal(decodeStepUpToken("a.@@@.c"), null);
});

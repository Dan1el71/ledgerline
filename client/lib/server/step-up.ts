import { createHmac, timingSafeEqual } from "node:crypto";

/**
 * Pure helpers for the BFF step-up marker. Kept free of Next.js imports so they can be unit
 * tested; the cryptographic verification of the step-up JWT itself belongs to transaction-service.
 */
export const STEP_UP_SECONDS = 5 * 60;

export interface StepUpTokenPayload {
  sub?: string;
  scope?: string;
  iat?: number;
  exp?: number;
}

export function signStepUpMarker(payload: string, secret: string): string {
  return createHmac("sha256", secret).update(payload).digest("base64url");
}

export function createStepUpMarker(
  userId: string,
  nowSeconds: number,
  secret: string,
  ttlSeconds = STEP_UP_SECONDS
): string {
  const expires = nowSeconds + ttlSeconds;
  const payload = `${userId}.${expires}`;
  return `${payload}.${signStepUpMarker(payload, secret)}`;
}

export function verifyStepUpMarker(
  raw: string | undefined,
  userId: string | undefined,
  nowSeconds: number,
  secret: string
): boolean {
  if (!raw || !userId) {
    return false;
  }
  const [id, expires, signature] = raw.split(".");
  if (!id || !expires || !signature || id !== userId) {
    return false;
  }
  const expiresAt = Number(expires);
  if (!Number.isFinite(expiresAt) || expiresAt < nowSeconds) {
    return false;
  }
  const expected = Buffer.from(signStepUpMarker(`${id}.${expires}`, secret));
  const actual = Buffer.from(signature);
  return expected.length === actual.length && timingSafeEqual(expected, actual);
}

export function decodeStepUpToken(token: string | undefined): StepUpTokenPayload | null {
  if (!token) {
    return null;
  }
  const parts = token.split(".");
  if (parts.length !== 3) {
    return null;
  }
  try {
    const payload = JSON.parse(Buffer.from(parts[1], "base64url").toString("utf8")) as unknown;
    return payload && typeof payload === "object" ? (payload as StepUpTokenPayload) : null;
  } catch {
    return null;
  }
}

/**
 * Local gate only. transaction-service re-verifies the JWT signature, audience, scope and user;
 * this check keeps the UI from sending a request that would be rejected anyway.
 */
export function isStepUpTokenUsable(
  token: string | undefined,
  userId: string | undefined,
  nowSeconds: number
): boolean {
  if (!token || !userId) {
    return false;
  }
  const payload = decodeStepUpToken(token);
  if (!payload || payload.sub !== userId || payload.scope !== "step-up") {
    return false;
  }
  if (typeof payload.exp !== "number" || payload.exp <= nowSeconds) {
    return false;
  }
  return (
    typeof payload.iat !== "number" ||
    (payload.exp - payload.iat > 0 && payload.exp - payload.iat <= STEP_UP_SECONDS)
  );
}

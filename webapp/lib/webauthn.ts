"use client";

import {
  create,
  get,
  supported,
  type CredentialCreationOptionsJSON,
  type CredentialRequestOptionsJSON,
} from "@github/webauthn-json";

/**
 * Thin wrapper around the browser WebAuthn API for our passkey flows.
 *
 * The backend ceremony endpoints speak the standard WebAuthn JSON convention
 * (base64url-encoded fields). `@github/webauthn-json` handles the
 * ArrayBuffer <-> base64url conversion in exactly that format, so here we only
 * deal with the JSON strings that our SDK returns / expects:
 *  - the "options" endpoints return the creation/request options as a JSON string
 *  - the finish endpoints (register, register-new, login) take the
 *    resulting credential as a raw JSON string
 */

/** Thrown when the user cancels or the authenticator aborts the ceremony. */
export class PasskeyCeremonyCancelledError extends Error {
  constructor(message = "Passkey ceremony was cancelled") {
    super(message);
    this.name = "PasskeyCeremonyCancelledError";
  }
}

/** True when the current browser exposes the WebAuthn platform API. */
export function isPasskeySupported(): boolean {
  return typeof globalThis.window !== "undefined" && supported();
}

function isCancellation(error: unknown): boolean {
  return (
    error instanceof DOMException &&
    (error.name === "NotAllowedError" || error.name === "AbortError")
  );
}

/**
 * The options endpoints are declared to return a JSON `string`, but the backend
 * serves them as `application/json`, so the SDK client parses the body into an
 * object before we receive it. Accept either form and hand back a plain object.
 */
function parseOptions<T>(options: unknown): T {
  return (typeof options === "string" ? JSON.parse(options) : options) as T;
}

/**
 * Runs the registration (attestation) ceremony.
 * @param options the creation options returned by a register options endpoint
 *     (a parsed object, or a JSON string)
 * @returns the credential as a JSON string ready to POST to the finish endpoint
 */
export async function createCredential(options: unknown): Promise<string> {
  const publicKey =
    parseOptions<CredentialCreationOptionsJSON["publicKey"]>(options);
  try {
    const credential = await create({ publicKey });
    return JSON.stringify(credential);
  } catch (error) {
    if (isCancellation(error)) {
      throw new PasskeyCeremonyCancelledError();
    }
    throw error;
  }
}

/**
 * Runs the authentication (assertion) ceremony.
 * @param options the request options returned by the `login/options` endpoint
 *     (a parsed object, or a JSON string)
 * @returns the assertion as a JSON string ready to POST to the login endpoint
 */
export async function getAssertion(options: unknown): Promise<string> {
  const publicKey =
    parseOptions<CredentialRequestOptionsJSON["publicKey"]>(options);
  try {
    const assertion = await get({ publicKey });
    return JSON.stringify(assertion);
  } catch (error) {
    if (isCancellation(error)) {
      throw new PasskeyCeremonyCancelledError();
    }
    throw error;
  }
}

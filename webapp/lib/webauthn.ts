"use client";

/**
 * Thin wrapper around the browser WebAuthn API for our passkey flows.
 *
 * The backend ceremony endpoints speak the standard WebAuthn JSON convention
 * (base64url-encoded fields). `PublicKeyCredential`'s native JSON methods
 * handle the ArrayBuffer <-> base64url conversion in exactly that format, so
 * here we only deal with the JSON strings that our SDK returns / expects:
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
  return (
    typeof globalThis.window !== "undefined" &&
    !!globalThis.PublicKeyCredential?.parseCreationOptionsFromJSON
  );
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
  const optionsJSON =
    parseOptions<PublicKeyCredentialCreationOptionsJSON>(options);
  const publicKey =
    PublicKeyCredential.parseCreationOptionsFromJSON(optionsJSON);
  try {
    const credential = (await navigator.credentials.create({
      publicKey,
    })) as PublicKeyCredential;
    return JSON.stringify(credential.toJSON());
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
  const optionsJSON =
    parseOptions<PublicKeyCredentialRequestOptionsJSON>(options);
  const publicKey =
    PublicKeyCredential.parseRequestOptionsFromJSON(optionsJSON);
  try {
    const assertion = (await navigator.credentials.get({
      publicKey,
    })) as PublicKeyCredential;
    return JSON.stringify(assertion.toJSON());
  } catch (error) {
    if (isCancellation(error)) {
      throw new PasskeyCeremonyCancelledError();
    }
    throw error;
  }
}

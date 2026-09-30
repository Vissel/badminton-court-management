import api, { responseMessage } from "./index";
import { emitApiError } from "./errorBus";

// The server's RSA-OAEP/SHA-256 public key (X.509/SPKI), served as PEM text by
// GET {context}/public-key. Cached as a CryptoKey; invalidated + refetched on
// 400/401 credential failures in case the server rotated keys (withKeyRetry).
let publicKeyPromise = null;

const pemToDer = (pem) => {
  const base64 = String(pem ?? "")
    .replace(/-----BEGIN PUBLIC KEY-----/g, "")
    .replace(/-----END PUBLIC KEY-----/g, "")
    .replace(/\s/g, "");
  const binary = atob(base64);
  const der = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    der[i] = binary.charCodeAt(i);
  }
  return der.buffer;
};

const fetchPublicKey = async () => {
  const res = await api.get("/public-key", {
    transformResponse: [(data) => data],
  });
  return crypto.subtle.importKey(
    "spki",
    pemToDer(res?.data),
    { name: "RSA-OAEP", hash: "SHA-256" },
    false,
    ["encrypt"]
  );
};

const getPublicKey = () => {
  if (!publicKeyPromise) {
    publicKeyPromise = fetchPublicKey().catch((err) => {
      publicKeyPromise = null;
      throw err;
    });
  }
  return publicKeyPromise;
};

// Returns the base64 RSA-OAEP ciphertext of `plain`. OAEP is non-deterministic,
// so call this once per field — never reuse a ciphertext for two fields.
// Requires a secure context (HTTPS or localhost) for crypto.subtle.
export const encryptPassword = async (plain) => {
  if (!crypto?.subtle) {
    throw new Error("crypto.subtle is unavailable (requires HTTPS or localhost)");
  }
  const key = await getPublicKey();
  const ciphertext = await crypto.subtle.encrypt(
    { name: "RSA-OAEP" },
    key,
    new TextEncoder().encode(plain)
  );
  const bytes = new Uint8Array(ciphertext);
  let binary = "";
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
};

// Runs `request(cfg)`; the callback must forward `cfg` to axios's config arg so
// the first attempt can suppress the global error toast — a stale cached key
// producing a 400/401 is retried silently (key refetched, request re-encrypted)
// exactly once, and only a retry failure reaches the user's screen.
export const withKeyRetry = async (request) => {
  try {
    return await request({ skipErrorToast: true });
  } catch (err) {
    const status = err?.response?.status;
    if (status === 400 || status === 401) {
      publicKeyPromise = null;
      return request();
    }
    // Non-retryable failure on the suppressed attempt — surface it ourselves.
    emitApiError(
      err?.response
        ? responseMessage(err.response.data, "Yêu cầu không hợp lệ.")
        : "Không thể kết nối tới máy chủ. Vui lòng thử lại."
    );
    throw err;
  }
};

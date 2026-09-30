import { TextEncoder } from "util";

jest.mock("./index", () => ({
  __esModule: true,
  default: { get: jest.fn() },
  responseMessage: (data, fallback) =>
    typeof data === "string" && data ? data : fallback,
}));

const PEM = [
  "-----BEGIN PUBLIC KEY-----",
  btoa("fake-der-bytes"),
  "-----END PUBLIC KEY-----",
].join("\n");

describe("rsaCrypto", () => {
  let api;
  let importKey;
  let encrypt;
  let encryptPassword;
  let withKeyRetry;

  beforeEach(() => {
    // Fresh module graph per test: the key cache lives at module level, and
    // the mocked api must be re-required after resetModules to stay in sync.
    jest.resetModules();
    api = require("./index").default;
    ({ encryptPassword, withKeyRetry } = require("./rsaCrypto"));

    // jsdom has no WebCrypto — mock subtle; "encrypt" reverses bytes.
    importKey = jest.fn().mockResolvedValue({ fake: "key" });
    encrypt = jest.fn((_alg, _key, data) =>
      Promise.resolve(new Uint8Array(data).reverse().buffer)
    );
    global.crypto = { subtle: { importKey, encrypt } };
    global.TextEncoder = TextEncoder;

    api.get.mockResolvedValue({ status: 200, data: PEM });
  });

  test("encryptPassword fetches /public-key once and caches the key", async () => {
    await encryptPassword("first");
    await encryptPassword("second");

    expect(api.get).toHaveBeenCalledTimes(1);
    expect(api.get).toHaveBeenCalledWith("/public-key", expect.anything());
    expect(importKey).toHaveBeenCalledTimes(1);
    expect(importKey).toHaveBeenCalledWith(
      "spki",
      expect.any(ArrayBuffer),
      { name: "RSA-OAEP", hash: "SHA-256" },
      false,
      ["encrypt"]
    );
  });

  test("encryptPassword returns base64 ciphertext of the plaintext", async () => {
    const ct = await encryptPassword("secret");
    expect(atob(ct).split("").reverse().join("")).toBe("secret");
    expect(encrypt).toHaveBeenCalledWith(
      { name: "RSA-OAEP" },
      expect.anything(),
      expect.anything()
    );
  });

  test("a failed key fetch clears the cache so the next call refetches", async () => {
    api.get.mockRejectedValueOnce(new Error("network"));
    await expect(encryptPassword("x")).rejects.toThrow();

    const ct = await encryptPassword("x");
    expect(api.get).toHaveBeenCalledTimes(2);
    expect(ct).toBeTruthy();
  });

  test("withKeyRetry refetches the key and retries once on 400", async () => {
    const apiCall = jest
      .fn()
      .mockRejectedValueOnce({ response: { status: 400 } })
      .mockResolvedValueOnce("ok");

    const res = await withKeyRetry(async () => {
      await encryptPassword("pw");
      return apiCall();
    });

    expect(res).toBe("ok");
    expect(apiCall).toHaveBeenCalledTimes(2);
    expect(api.get).toHaveBeenCalledTimes(2); // refetched after invalidation
  });

  test("withKeyRetry does not retry on non-400/401 errors", async () => {
    const apiCall = jest.fn().mockRejectedValue({ response: { status: 500 } });

    await expect(withKeyRetry(apiCall)).rejects.toEqual({
      response: { status: 500 },
    });
    expect(apiCall).toHaveBeenCalledTimes(1);
  });
});

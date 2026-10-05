import {
  clearAuth,
  getAccessToken,
  getRefreshToken,
  getRoles,
  getUsername,
  isAccessTokenValid,
  storeAuth,
} from "./tokenStore";

const token = (expiresAt) => {
  const payload = btoa(JSON.stringify({ exp: expiresAt })).replace(/=/g, "");
  return `header.${payload}.signature`;
};

describe("tokenStore", () => {
  afterEach(() => sessionStorage.clear());

  test("stores and clears an authentication response", () => {
    storeAuth({
      accessToken: "access",
      refreshToken: "refresh",
      username: "admin",
      roles: ["ADMINISTRATOR"],
    });

    expect(getAccessToken()).toBe("access");
    expect(getRefreshToken()).toBe("refresh");
    expect(getUsername()).toBe("admin");
    expect(getRoles()).toEqual(["ADMINISTRATOR"]);

    clearAuth();
    expect(getAccessToken()).toBeNull();
    expect(getRoles()).toEqual([]);
  });

  test("checks JWT expiry", () => {
    sessionStorage.setItem("accessToken", token(Math.floor(Date.now() / 1000) + 60));
    expect(isAccessTokenValid()).toBe(true);

    sessionStorage.setItem("accessToken", token(Math.floor(Date.now() / 1000) - 1));
    expect(isAccessTokenValid()).toBe(false);
  });
});

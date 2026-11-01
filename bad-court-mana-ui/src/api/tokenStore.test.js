import {
  clearAuth,
  getAccessToken,
  getRefreshToken,
  getRoles,
  getUsername,
  getVnRoleMapping,
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
      roles: ["Administrator"],
      vnRoleMapping: { Administrator: "Quản trị viên" },
    });

    expect(getAccessToken()).toBe("access");
    expect(getRefreshToken()).toBe("refresh");
    expect(getUsername()).toBe("admin");
    expect(getRoles()).toEqual(["Administrator"]);
    expect(getVnRoleMapping()).toEqual({ Administrator: "Quản trị viên" });

    clearAuth();
    expect(getAccessToken()).toBeNull();
    expect(getRoles()).toEqual([]);
    expect(getVnRoleMapping()).toEqual({});
  });

  test("normalizes legacy role casing to PascalCase", () => {
    sessionStorage.setItem("roles", JSON.stringify(["ROOT", "administrator", "Coordinator"]));
    expect(getRoles()).toEqual(["Root", "Administrator", "Coordinator"]);
  });

  test("checks JWT expiry", () => {
    sessionStorage.setItem("accessToken", token(Math.floor(Date.now() / 1000) + 60));
    expect(isAccessTokenValid()).toBe(true);

    sessionStorage.setItem("accessToken", token(Math.floor(Date.now() / 1000) - 1));
    expect(isAccessTokenValid()).toBe(false);
  });
});

const KEYS = {
  accessToken: "accessToken",
  refreshToken: "refreshToken",
  username: "username",
  roles: "roles",
  vnRoleMapping: "vnRoleMapping",
};

export const getAccessToken = () => sessionStorage.getItem(KEYS.accessToken);
export const getRefreshToken = () => sessionStorage.getItem(KEYS.refreshToken);
export const getUsername = () => sessionStorage.getItem(KEYS.username);
// Roles are handled in PascalCase (Root, Administrator, Coordinator, Player).
// Older sessions may still cache camelCase or UPPER_CASE names, so normalize
// whatever is read or written.
export const normalizeRoles = (roles) =>
  Array.isArray(roles)
    ? roles
      .map((role) => String(role ?? "").trim().toLowerCase())
      .filter(Boolean)
      .map((role) => role.charAt(0).toUpperCase() + role.slice(1))
    : [];
export const getRoles = () => {
  try {
    return normalizeRoles(JSON.parse(sessionStorage.getItem(KEYS.roles) || "[]"));
  } catch {
    return [];
  }
};
export const getVnRoleMapping = () => {
  try {
    return JSON.parse(sessionStorage.getItem(KEYS.vnRoleMapping) || "{}");
  } catch {
    return {};
  }
};

export const storeAuth = ({ accessToken, refreshToken, username, roles = [], vnRoleMapping = {} }) => {
  sessionStorage.setItem(KEYS.accessToken, accessToken);
  sessionStorage.setItem(KEYS.refreshToken, refreshToken);
  sessionStorage.setItem(KEYS.username, username);
  sessionStorage.setItem(KEYS.roles, JSON.stringify(normalizeRoles(roles)));
  sessionStorage.setItem(KEYS.vnRoleMapping, JSON.stringify(vnRoleMapping));
};

export const clearAuth = () => Object.values(KEYS).forEach((key) => sessionStorage.removeItem(key));

export const isAccessTokenValid = () => {
  const token = getAccessToken();
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return payload.exp * 1000 > Date.now() + 5000;
  } catch {
    return false;
  }
};

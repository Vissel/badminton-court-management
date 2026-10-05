const KEYS = {
  accessToken: "accessToken",
  refreshToken: "refreshToken",
  username: "username",
  roles: "roles",
};

export const getAccessToken = () => sessionStorage.getItem(KEYS.accessToken);
export const getRefreshToken = () => sessionStorage.getItem(KEYS.refreshToken);
export const getUsername = () => sessionStorage.getItem(KEYS.username);
export const getRoles = () => {
  try {
    return JSON.parse(sessionStorage.getItem(KEYS.roles) || "[]");
  } catch {
    return [];
  }
};

export const storeAuth = ({ accessToken, refreshToken, username, roles = [] }) => {
  sessionStorage.setItem(KEYS.accessToken, accessToken);
  sessionStorage.setItem(KEYS.refreshToken, refreshToken);
  sessionStorage.setItem(KEYS.username, username);
  sessionStorage.setItem(KEYS.roles, JSON.stringify(roles));
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

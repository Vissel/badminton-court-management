import React, { createContext, useEffect, useState } from "react";
import api from "../api";
import { emitApiError } from "../api/errorBus";
import { authRef } from "./authRef";
import {
  clearAuth,
  getRefreshToken,
  getRoles,
  getUsername,
  getVnRoleMapping,
  isAccessTokenValid,
  normalizeRoles,
  storeAuth,
} from "../api/tokenStore";

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [authenticated, setAuthenticated] = useState(false);
  const [username, setUsername] = useState(getUsername());
  const [roles, setRoles] = useState(getRoles());
  const [vnRoleMapping, setVnRoleMapping] = useState(getVnRoleMapping());
  const [loading, setLoading] = useState(true);

  const applyAuth = (data) => {
    storeAuth(data);
    // Functionality checks compare PascalCase role names; tolerate any legacy casing.
    setUsername(data.username || getUsername());
    setRoles(normalizeRoles(data.roles));
    setVnRoleMapping(data.vnRoleMapping || {});
    setAuthenticated(true);
  };

  useEffect(() => {
    const restore = async () => {
      if (isAccessTokenValid()) {
        setAuthenticated(true);
        setLoading(false);
        return;
      }
      const refreshToken = getRefreshToken();
      if (!refreshToken) {
        clearAuth();
        setLoading(false);
        return;
      }
      try {
        const response = await api.post("/auth/refresh", { refreshToken });
        applyAuth(response.data);
      } catch {
        clearAuth();
      } finally {
        setLoading(false);
      }
    };
    restore();
  }, []);

  const logout = async () => {
    const refreshToken = getRefreshToken();
    // Logging out must never depend on the network: drop the local session first,
    // then let the backend revoke the refresh token in the background. The request
    // is silent so a stale backend never shows an error while the user leaves.
    clearAuth();
    setAuthenticated(false);
    setUsername(null);
    setRoles([]);
    setVnRoleMapping({});
    await api
      .post("/logout", { refreshToken }, { skipErrorToast: true })
      .catch(() => null);
  };

  const forceLogout = () => {
    clearAuth();
    setAuthenticated(false);
    setUsername(null);
    setRoles([]);
    setVnRoleMapping({});
    emitApiError("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!");
    // Keep the current path prefix (e.g. /caulong-tc-qa/) instead of an absolute
    // root URL, which would leave the deployed sub-path and 404.
    window.location.hash = "#/login";
  };

  useEffect(() => {
    authRef.logout = forceLogout;
  });

  const hasRole = (...allowedRoles) => {
    const allowed = allowedRoles.map((role) => String(role).toLowerCase());
    return roles.some((role) => allowed.includes(String(role).toLowerCase()));
  };

  return (
    <AuthContext.Provider
      value={{
        authenticated,
        setAuthenticated,
        username,
        roles,
        setRoles,
        vnRoleMapping,
        hasRole,
        applyAuth,
        logout,
        loading,
        setLoading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

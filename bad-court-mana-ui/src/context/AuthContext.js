import React, { createContext, useEffect, useState } from "react";
import api from "../api";
import { emitApiError } from "../api/errorBus";
import { authRef } from "./authRef";
import {
  clearAuth,
  getRefreshToken,
  getRoles,
  isAccessTokenValid,
  storeAuth,
} from "../api/tokenStore";

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [authenticated, setAuthenticated] = useState(false);
  const [roles, setRoles] = useState(getRoles());
  const [loading, setLoading] = useState(true);

  const applyAuth = (data) => {
    storeAuth(data);
    setRoles(data.roles || []);
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
    await api.post("/logout", { refreshToken }).catch(() => null);
    clearAuth();
    setAuthenticated(false);
    setRoles([]);
  };

  const forceLogout = () => {
    clearAuth();
    setAuthenticated(false);
    setRoles([]);
    emitApiError("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!");
    window.location.replace("/#/login");
  };

  useEffect(() => {
    authRef.logout = forceLogout;
  });

  const hasRole = (...allowedRoles) => roles.some((role) => allowedRoles.includes(role));

  return (
    <AuthContext.Provider
      value={{
        authenticated,
        setAuthenticated,
        roles,
        setRoles,
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

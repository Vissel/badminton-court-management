// src/context/AuthContext.js
import React, { createContext, useState, useEffect } from "react";

import Cookies from "js-cookie";
import api from "../api";

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [authenticated, setAuthenticated] = useState(false);
  const [csrfToken, setCsrfToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // Check session on mount
  const checkSession = async () => {
    try {
      // Step 1: fetch CSRF token and set it
      const res = await api.get(`/csrf`);
      if (res.status === 200) {
        const tokenFromCookie = res.data.csrfToken;
        setCsrfToken(tokenFromCookie);
        sessionStorage.setItem('csrfToken',tokenFromCookie)

        setAuthenticated(res.data.valid);
      }
    } catch (err) {
      setAuthenticated(false);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };
  const checkValidSession = () => {
    const token = Cookies.get("XSRF-TOKEN");
    if (token) {
      console.log("Valid token");
    } else {
      setAuthenticated(false);
      setCsrfToken(null);
    }
    setLoading(false);
  };
  useEffect(() => {
    setLoading(true);
    checkSession();
    // checkValidSession();
    // Refresh CSRF token every 10 minutes (optional)
    // const interval = setInterval(checkValidSession, 10 * 60 * 1000);
    // return () => clearInterval(interval);
  }, []);
  const logout = async () => {
    console.log("Calling logout.");
    const res = await api.post(
      `/logout`,
      {}
    );

    if (res.status === 200) {
      setAuthenticated(false);
      setCsrfToken(null);
      sessionStorage.clear();
      alert("Đăng xuất thành công.");
    }
  };
  return (
    <AuthContext.Provider
      value={{
        authenticated,
        setAuthenticated,
        csrfToken,
        setCsrfToken,
        logout,
        loading,
        setLoading
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// src/context/AuthContext.js
import React, { createContext, useState, useEffect } from "react";

import axios from "axios";
import Cookies from "js-cookie";
import { setCsrfTokenGetter } from "../api";

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [authenticated, setAuthenticated] = useState(false);
  const [csrfToken, setCsrfToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const localHost = "http://localhost:9080";
  const context = "bad-court-management-dev";

  // Check session on mount
  const checkSession = async () => {
    try {
      // Step 1: fetch CSRF token and set it
      const res = await axios.get(`${localHost}/${context}/csrf`, {
        withCredentials: true,
      });
      if (res.status === 200) {
        const tokenFromCookie = res.data.token;
        setCsrfToken(tokenFromCookie || Cookies.get("XSRF-TOKEN"));

        // Step 2: check session (if already logged in)
        //   await axios.get('http://localhost:8080/api/user', {
        //     withCredentials: true,
        //   });
        setAuthenticated(true);
      }
    } catch (err) {
      setAuthenticated(false);
      console.err(err);
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
      setCsrfTokenGetter(null);
    }
    setLoading(false);
  };
  useEffect(() => {
    // checkSession();
    checkValidSession();
    // Refresh CSRF token every 10 minutes (optional)
    // const interval = setInterval(checkValidSession, 10 * 60 * 1000);
    // return () => clearInterval(interval);
  }, []);
  const logout = async () => {
    console.log("Calling logout.");
    const res = await axios.post(
      `${localHost}/${context}/logout`,
      {},
      {
        headers: {
          "X-XSRF-TOKEN": csrfToken || Cookies.get("XSRF-TOKEN"), // REQUIRED
        },
        withCredentials: true,
      }
    );

    if (res.status === 200) {
      setAuthenticated(false);
      setCsrfToken(null);
      setCsrfTokenGetter(null);
      alert("logout is successful.");
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
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

import axios from "axios";
import config from './config'
import { authRef } from '../context/authRef';
import { emitApiError } from './errorBus';
import { clearAuth, getAccessToken, getRefreshToken, storeAuth } from './tokenStore';

// import { useNavigate } from 'react-router';

export const currentHost = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/#/`;

export const backendHost = `${config.baseURL}`;

const api = axios.create({
  baseURL: config.baseURL, // Update with your backend base URL
  withCredentials: true,
  timeout: config.timeout,
});
api.interceptors.request.use((config) => {
  const accessToken = getAccessToken();
  const anonymousPath = ["/login", "/auth/refresh", "/logout", "/public-key"].includes(config.url);
  if (accessToken && !anonymousPath) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  // FormData uploads must let the browser set the multipart boundary.
  if (config.data instanceof FormData) {
    delete config.headers["Content-Type"];
  } else {
    config.headers["Content-Type"] = "application/json";
  }
  return config;
});

// Text that belongs in the console, not on screen: SQL statements, Hibernate
// and Java stack fragments the backend may still return on an unexpected 500.
const TECHNICAL_MESSAGE =
  /could not execute statement|SQL \[|deadlock found|lock wait timeout|nested exception|hibernate|java\.[a-z]|javax\.|org\.springframework|constraint \[/i;

// Extracts a human-readable message from an error body. Handles JSON error
// payloads ({errorMessage|message}) and plain-string bodies (e.g. SuperAdmin
// endpoints); HTML error pages and technical text fall back to the generic
// message, so the user is never shown code.
export const responseMessage = (data, fallback) => {
  let message = null;
  if (data && typeof data === "object") {
    message = data.errorMessage || data.message || null;
  } else if (
    typeof data === "string" &&
    data.trim() &&
    data.length <= 300 &&
    !data.trimStart().startsWith("<")
  ) {
    message = data;
  }
  if (!message || TECHNICAL_MESSAGE.test(message)) {
    return fallback;
  }
  return message;
};

let refreshPromise = null;

const refreshAccessToken = () => {
  if (!refreshPromise) {
    refreshPromise = axios.post(`${config.baseURL}/auth/refresh`, {
      refreshToken: getRefreshToken(),
    }).then((response) => {
      storeAuth(response.data);
      return response.data.accessToken;
    }).finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
};

api.interceptors.response.use(
  (response) => response,

  async (error) => {
    // Opt-out flag for callers that handle failures themselves (e.g. per-player
    // fan-out requests where a business error is an expected outcome).
    const silent = error.config?.skipErrorToast;

    // Network error (server down, CORS, timeout)
    if (!error.response) {
      console.error("Network error:", error);
      if (!silent) emitApiError("Không thể kết nối tới máy chủ. Vui lòng thử lại.");
      return Promise.reject(error);
    }

    const { status, config, data } = error.response;
    const currentPath = window.location.pathname;

    /* ===============================
       401 / 403 – Unauthorized
    ================================ */
    if (status === 401 && !config?._retry && config?.url !== "/auth/refresh" && getRefreshToken()) {
      try {
        config._retry = true;
        config.headers.Authorization = `Bearer ${await refreshAccessToken()}`;
        return api(config);
      } catch {
        clearAuth();
        authRef.logout?.();
        return Promise.reject(error);
      }
    }
    if (status === 401 && !currentPath.includes("/login")) {
      clearAuth();
      authRef.logout?.();
      return Promise.reject(error);
    }
    if (status === 403 && !silent) {
      emitApiError("Bạn không có quyền thực hiện thao tác này.");
      return Promise.reject(error);
    }
    /* ===============================
       Handle BLOB error (export)
    ================================ */
    if (
      config?.responseType === "blob" &&
      data instanceof Blob &&
      data.type?.includes("application/json")
    ) {
      try {
        const text = await data.text();
        const json = JSON.parse(text);

        console.error("Export error:", json);
        emitApiError(json.errorMessage || json.message || "Xuất báo cáo thất bại");
      } catch (e) {
        console.error("Failed to parse blob error", e);
        emitApiError("Xuất báo cáo thất bại");
      }

      return Promise.reject(error);
    }

    /* ===============================
       500 – Internal Server Error
    ================================ */
    if (status >= 500) {
      console.error("Server error:", error.response);
      if (!silent)
        emitApiError(
          responseMessage(data, "Lỗi hệ thống. Vui lòng thử lại sau vài giây.")
        );
      return Promise.resolve(null);
    }

    /* ===============================
       Other client errors (400, 404…)
    ================================ */
    if (status === 400 || status > 403) {
      console.warn("Client error:", error.response);
      if (!silent) emitApiError(responseMessage(data, "Yêu cầu không hợp lệ."));
    }

    return Promise.reject(error);
  }
);

export default api;
// export const addNewServiceAPI = (payload) =>
//   api.post("/api/addSetupService", payload);
// export const getSettings = () => api.get("/api/getSetupServices");

// Rent by time API calls
export const applyRentByTime = (payload) =>
  api.post("/court-mana/applyRentByTime", payload);
export const payRentByTime = (rentId, customFee) =>
  api.post(`/court-mana/payRentByTime?rentId=${rentId}&customFee=${customFee}`);
export const cancelRentByTime = (rentId) =>
  api.post(`/court-mana/cancelRentByTime?rentId=${rentId}`);
export const updateRentByTime = (rentId, payload) =>
  api.post(`/court-mana/updateRentByTime?rentId=${rentId}`, payload);

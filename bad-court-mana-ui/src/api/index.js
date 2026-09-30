import axios from "axios";
import config from './config'
import { authRef } from '../context/authRef';
import { emitApiError } from './errorBus';

// import { useNavigate } from 'react-router';

export const currentHost = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/#/`;

export const backendHost = `${config.baseURL}`;

const api = axios.create({
  baseURL: config.baseURL, // Update with your backend base URL
  withCredentials: true,
  timeout: config.timeout,
});
api.interceptors.request.use((config) => {
  const csrfToken = sessionStorage.getItem("csrfToken");
  if (csrfToken) {
    config.headers["X-XSRF-TOKEN"] = csrfToken;
  }
  // FormData uploads must let the browser set the multipart boundary.
  if (config.data instanceof FormData) {
    delete config.headers["Content-Type"];
  } else {
    config.headers["Content-Type"] = "application/json";
  }
  return config;
});

// Extracts a human-readable message from an error body. Handles JSON error
// payloads ({errorMessage|message}) and plain-string bodies (e.g. SuperAdmin
// endpoints); HTML error pages fall back to the generic message.
export const responseMessage = (data, fallback) => {
  if (data && typeof data === "object") {
    return data.errorMessage || data.message || fallback;
  }
  if (
    typeof data === "string" &&
    data.trim() &&
    data.length <= 300 &&
    !data.trimStart().startsWith("<")
  ) {
    return data;
  }
  return fallback;
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
    if (status === 401 || status === 403) {
      const excludePaths = ["/login", "*", "/"];
      const isExcluded = excludePaths.some((p) => currentPath.includes(p));

      if (!isExcluded) {
        console.warn("Unauthorized / Forbidden – forcing logout");
        authRef.logout?.();
        return new Promise(() => { });
      }
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
      if (!silent) emitApiError(responseMessage(data, "Lỗi hệ thống. Vui lòng thử lại sau."));
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

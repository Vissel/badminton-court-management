import axios from "axios";
import Cookies from "js-cookie";

// import { useNavigate } from 'react-router';

export const currentHost = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/#/`;

const localHost = "http://localhost:9080";
const context = "bad-court-management-dev";
export const backendHost = `${localHost}/${context}`

const api = axios.create({
  baseURL: `${localHost}/${context}`, // Update with your backend base URL
  withCredentials: true,
  // timeout: 3000,
});
api.interceptors.request.use((config) => {
  const csrfToken = sessionStorage.getItem("csrfToken");
  if (csrfToken) {
    config.headers["X-XSRF-TOKEN"] = csrfToken;
  }
  config.headers["Content-Type"] = "application/json";
  return config;
});

api.interceptors.response.use(
  (response) => response,

  async (error) => {
    // Network error (server down, CORS, timeout)
    if (!error.response) {
      console.error("Network error:", error);
      alert("Không thể kết nối tới máy chủ. Vui lòng thử lại.");
      return Promise.reject(error);
    }

    const { status, config, data, headers } = error.response;
    const currentPath = window.location.pathname;

    /* ===============================
       401 / 403 – Unauthorized
    ================================ */
    if ((status === 401 || status === 403) && !currentPath.includes("/login")) {
      console.warn("Unauthorized / Forbidden – redirecting to login");

      localStorage.clear();
      sessionStorage.clear();

      alert("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!");
      window.location.replace("/#/login");

      // Stop promise chain cleanly
      return new Promise(() => {});
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
        alert(json.message || "Xuất báo cáo thất bại");

      } catch (e) {
        console.error("Failed to parse blob error", e);
        alert("Xuất báo cáo thất bại");
      }

      return Promise.reject(error);
    }

    /* ===============================
       500 – Internal Server Error
    ================================ */
    if (status >= 500) {
      console.error("Server error:", error.response);
      alert("Lỗi hệ thống. Vui lòng thử lại sau.");
      return Promise.resolve(null);
    }

    /* ===============================
       Other client errors (400, 404…)
    ================================ */
    if (status >= 400) {
      console.warn("Client error:", error.response);
      alert("Yêu cầu không hợp lệ.");
    }

    return Promise.reject(error);
  }
);


export default api;
// export const addNewServiceAPI = (payload) =>
//   api.post("/api/addSetupService", payload);
// export const getSettings = () => api.get("/api/getSetupServices");

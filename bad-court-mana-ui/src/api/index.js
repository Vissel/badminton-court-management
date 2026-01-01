import axios from "axios";
import Cookies from "js-cookie";

// import { useNavigate } from 'react-router';

const currentHost = `${window.location.protocol}//${window.location.hostname}`;
const localHost = "http://localhost:9080";
const context = "bad-court-management-dev";

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

// 🔁 Response interceptor to handle expired CSRF/session
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;

    if (status === 401 || status === 403) {
      if (
        !window.location.pathname.includes("/login") ||
        !window.location.pathname.includes("/")
      ) {
        console.warn("Unauthorized or Forbidden – redirecting to /login");

        // Clear any stored auth info if you have
        localStorage.clear();
        sessionStorage.clear();

        alert("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!");
        window.location.replace("/#/login");
        return new Promise(() => {});
      }
    }
    return Promise.reject(error);
  }
);

export default api;
// export const addNewServiceAPI = (payload) =>
//   api.post("/api/addSetupService", payload);
// export const getSettings = () => api.get("/api/getSetupServices");

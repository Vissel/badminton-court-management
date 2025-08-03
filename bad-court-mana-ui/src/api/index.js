import axios from "axios";
import Cookies from "js-cookie";
import { useNavigate } from 'react-router';

const currentHost = `${window.location.protocol}//${window.location.hostname}`;
const localHost = "http://localhost:9080";
const context = "bad-court-management-dev";

const api = axios.create({
  baseURL: `${localHost}/${context}`, // Update with your backend base URL
  withCredentials: true,
});
api.interceptors.request.use((config) => {
  const csrfToken = sessionStorage.getItem('csrfToken');
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
    if (
      error.response && (error.response.status === 401) &&
      window.location.pathname !== "/login"
    ) {
      // Optional: clear tokens or local state
      console.warn(
        "CSRF token expired or session timeout, redirecting to login"
      );
      const navigate = useNavigate();
      navigate("/login"); // 🔁 force full reload
    }
    return Promise.reject(error);
  }
);

export default api;
// export const addNewServiceAPI = (payload) =>
//   api.post("/api/addSetupService", payload);
// export const getSettings = () => api.get("/api/getSetupServices");

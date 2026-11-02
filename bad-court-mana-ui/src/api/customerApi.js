import api from "./index";

// Buyer directory (G-9) — /api/v1/customers. Used by the checkout buyer block
// autocomplete and by the admin catalogue on the Setup page.
export const searchCustomers = (keyword) =>
    api.get("/api/v1/customers", { params: { keyword: keyword || undefined } });

// ROOT/ADMINISTRATOR only.
export const saveCustomer = (payload) => api.post("/api/v1/customers", payload);

// Soft delete: the entry stops appearing in the autocomplete.
export const deleteCustomer = (customerId) =>
    api.delete(`/api/v1/customers/${customerId}`);

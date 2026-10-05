import api from "./index";

// Bill (invoice) endpoints — see BillingController.
// List rows omit `items`; fetch detail for the full document.
export const listBills = (payload) => api.post("/api/v1/bills/list", payload);
export const getBill = (billId) => api.get(`/api/v1/bills/${billId}`);
// Bill + seller profile merged for the print view.
export const getReceipt = (billId) => api.get(`/api/v1/bills/${billId}/receipt`);
// Print through the configured channel: "NETWORK" pushes an ESC/POS raster
// job to the printer IP:port; "BROWSER"/omitted uses the configured mode and
// (in BROWSER mode) only counts the print after the FE ran window.print().
export const printBill = (billId, channel) =>
  api.post(`/api/v1/bills/${billId}/print`, null, {
    params: channel ? { channel } : {},
  });
// ROOT/ADMINISTRATOR only.
export const voidBill = (billId, reason) =>
  api.post(`/api/v1/bills/${billId}/void`, { reason });
export const getBillConfig = () => api.get("/api/v1/bills/config");
// ROOT/ADMINISTRATOR only.
export const updateBillConfig = (payload) =>
  api.put("/api/v1/bills/config", payload);
// Probe the configured network printer (ROOT/ADMINISTRATOR only).
export const testPrinter = () => api.post("/api/v1/bills/printer/test");

// E-invoice (MISA meInvoice): publish/retry is admin-only; status refresh is
// available to anyone who can view the bill.
export const issueEInvoice = (billId) =>
  api.post(`/api/v1/bills/${billId}/einvoice`);
export const refreshEInvoiceStatus = (billId) =>
  api.get(`/api/v1/bills/${billId}/einvoice/status`);

// Binary exports — blob responses (the axios interceptor parses JSON error
// bodies out of blobs already).
export const exportBillsExcel = (payload) =>
  api.post("/api/v1/bills/export", payload, { responseType: "blob" });
export const downloadBillPdf = (billId) =>
  api.get(`/api/v1/bills/${billId}/pdf`, { responseType: "blob" });
export const downloadEInvoicePdf = (billId) =>
  api.get(`/api/v1/bills/${billId}/einvoice/pdf`, { responseType: "blob" });

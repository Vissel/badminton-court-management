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
// Archived provider XML (retention copy of the issued e-invoice).
export const downloadEInvoiceXml = (billId) =>
  api.get(`/api/v1/bills/${billId}/einvoice/xml`, { responseType: "blob" });

// Buyer retrieval QR (G-11) as a PNG blob — an <img src> cannot send the JWT,
// so the receipt view fetches it here and renders an object URL. 404 means no
// retrieval base is configured: the receipt then prints without a QR.
export const fetchBillQr = (billId) =>
  api.get(`/api/v1/bills/${billId}/qr`, {
    responseType: "blob",
    skipErrorToast: true,
  });

// Resend the issued e-invoice to the buyer's email. `email` is optional: the
// backend falls back to the buyer email stored on the bill.
export const sendEInvoiceMail = (billId, email) =>
  api.post(`/api/v1/bills/${billId}/einvoice/send`, email ? { email } : {});

// Correction invoices (ROOT/ADMINISTRATOR only). Adjust = delta lines only,
// replace = a full new bill that voids the original.
export const adjustBill = (billId, payload) =>
  api.post(`/api/v1/bills/${billId}/adjust`, payload);
export const replaceBill = (billId, payload) =>
  api.post(`/api/v1/bills/${billId}/replace`, payload);

// Internal-control trail of one bill (issue/void/adjust/e-invoice/config).
export const getBillLogs = (billId) => api.get(`/api/v1/bills/${billId}/logs`);

// Revenue + output VAT summary. groupBy: DAY | MONTH | QUARTER.
export const getTaxReport = (payload) =>
  api.post("/api/v1/bills/tax-report", payload || {});
export const exportTaxReport = (payload) =>
  api.post("/api/v1/bills/tax-report/export", payload || {}, {
    responseType: "blob",
  });

// Per-service VAT overrides edited from Setup (write is admin-only).
export const listServiceVat = () => api.get("/api/v1/bills/service-vat");
export const updateServiceVat = (serviceId, vatRate) =>
  api.put(`/api/v1/bills/service-vat/${serviceId}`, { vatRate });

import api from "./index";

/**
 * Product catalog management API (shuttle balls + food/drink services).
 *
 * Backend contract expected:
 *
 * 1) GET /api/getSetupServices
 *    Returns the existing settings payload that already powers SetupPage:
 *    { totalCourt, costInPerson, rentByTime, shuttleBalls: [...], services: [...] }
 *    Used by ProductPage to read current products and preserve court/pricing
 *    settings when saving product changes.
 *
 * 2) POST /api/updateSetupService
 *    Existing setup mutation used for adding/deleting products.
 *    Preserves the settings above while accepting added/deleted arrays.
 *
 * 3) GET /api/products/export
 *    Returns a .xlsx blob (2 sheets: ShuttleBall, Service).
 *
 * 4) GET /api/products/template
 *    Returns an empty .xlsx template with headers only.
 *
 * 5) POST /api/products/import/preview
 *    Multipart: file=.xlsx
 *    Response: { success, data: { importToken, rows, counts } }
 *    rows: [{ row, sheet, name, cost, action, message }]
 *    action: ADD | UPDATE | REACTIVATE | SKIP | ERROR
 *
 * 6) POST /api/products/import/commit
 *    Body: { importToken }
 *    Applies the previewed import transactionally.
 */

const DEFAULT_FILE_NAME = "products.xlsx";

const downloadBlob = (response, defaultName) => {
  const blob = new Blob([response.data]);
  const url = window.URL.createObjectURL(blob);
  const disposition = response.headers["content-disposition"] || "";
  const match = disposition.match(/filename="?([^"]+)"?/);
  const fileName = match ? match[1] : defaultName;

  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const getSetupServices = () => api.get("/api/getSetupServices");

export const updateSetupService = (payload) =>
  api.post("/api/updateSetupService", payload);

export const exportProducts = async () => {
  const response = await api.get("/api/products/export", {
    responseType: "blob",
  });
  downloadBlob(response, `export_${DEFAULT_FILE_NAME}`);
  return response;
};

export const downloadProductTemplate = async () => {
  const response = await api.get("/api/products/template", {
    responseType: "blob",
  });
  downloadBlob(response, `template_${DEFAULT_FILE_NAME}`);
  return response;
};

export const previewImportProducts = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.post("/api/products/import/preview", formData, {
    headers: { Accept: "application/json" },
  });
};

export const commitImportProducts = (importToken) =>
  api.post("/api/products/import/commit", { importToken });

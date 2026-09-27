import api from "./index";

/**
 * Inventory management API — backend is already implemented and rootuser-gated
 * (InventoryController → /api/inventory/** returns 403 for non-root users).
 *
 * 1) GET /api/inventory/items?itemType=SHUTTLE_BALL|GOODS   (itemType optional)
 *    → Result<[{ itemId, itemName, itemType: SHUTTLE_BALL|GOODS, unit,
 *       packageUnit, unitsPerPackage, retailPrice, active, stockOnHand
 *       (base units), stockLabel, packageBreakdown, avgCost, margin,
 *       lowStock }]>
 *
 * 2) POST /api/inventory/items          { itemName, itemType, unit,
 *       packageUnit?, unitsPerPackage?, retailPrice }
 *    PUT  /api/inventory/items/{id}    same body (blank packageUnit clears
 *    package config)
 *
 * 3) POST /api/inventory/purchases
 *    { itemId } OR { itemName, itemType } for a new item,
 *    + { purchaseDate: "YYYY-MM-DD", quantity, unitCost, unit?, supplier?,
 *       note? }
 *    unit: "PACKAGE" | "BASE" (default PACKAGE when the item has a package
 *    unit); quantity/unitCost are expressed in the chosen unit — the ledger
 *    records quantity × unitsPerPackage in base units.
 *
 * 4) GET /api/inventory/movements?itemId=&from=&to=
 *    → Result<[{ movementId, itemId, itemName, movementType
 *       (PURCHASE_IN|GAME_CONSUMPTION|RETAIL_SALE|ADJUSTMENT|RETURN),
 *       quantityDelta, unitCost, refType, refId, note, createdDate }]>
 *
 * 5) POST /api/inventory/adjustments    { itemId, quantityDelta, reason }
 *
 * 6) GET /api/inventory/export          → .xlsx blob (stock/avgCost/retail/margin)
 *
 * 7) POST /api/inventory/import/preview (multipart file)
 *    → Result<{ importToken,
 *       rows: [{ rowNumber, name, quantity, unitCost, purchaseDate, itemType,
 *                action: ADD|UPDATE|SKIP|ERROR, message }],
 *       counts }>
 *
 * 8) POST /api/inventory/import/commit  { importToken } → Result<Boolean>
 */

export const listInventoryItems = (itemType) =>
  api.get(
    itemType
      ? `/api/inventory/items?itemType=${encodeURIComponent(itemType)}`
      : "/api/inventory/items"
  );

export const createInventoryItem = (request) =>
  api.post("/api/inventory/items", request);

export const updateInventoryItem = (itemId, request) =>
  api.put(`/api/inventory/items/${itemId}`, request);

/**
 * Lightweight stock lookup for pickers (not root-gated).
 * Params: { itemId } or { item: name, itemType: SHUTTLE_BALL|GOODS }.
 * → Result<{ itemId, itemName, itemType, stockOnHand, lowStock, outOfStock }>
 */
export const checkStockItem = ({ itemId, item, itemType } = {}) => {
  const params = new URLSearchParams();
  if (itemId != null) params.set("itemId", itemId);
  if (item) params.set("item", item);
  if (itemType) params.set("itemType", itemType);
  return api.get(`/api/inventory/checkStock?${params.toString()}`);
};

export const recordPurchase = (request) =>
  api.post("/api/inventory/purchases", request);

export const listStockMovements = (itemId, from, to) => {
  const params = new URLSearchParams({ itemId });
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  return api.get(`/api/inventory/movements?${params.toString()}`);
};

export const adjustStock = (request) =>
  api.post("/api/inventory/adjustments", request);

export const exportInventory = async () => {
  const response = await api.get("/api/inventory/export", {
    responseType: "blob",
  });
  const blob = new Blob([response.data]);
  const url = window.URL.createObjectURL(blob);
  const disposition = response.headers["content-disposition"] || "";
  const match = disposition.match(/filename="?([^"]+)"?/);
  const link = document.createElement("a");
  link.href = url;
  link.download = match ? match[1] : "inventory.xlsx";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
  return response;
};

export const previewStockIntake = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.post("/api/inventory/import/preview", formData, {
    headers: { Accept: "application/json" },
  });
};

export const commitStockIntake = (importToken) =>
  api.post("/api/inventory/import/commit", { importToken });

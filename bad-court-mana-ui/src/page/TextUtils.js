// Accent-insensitive search normalization for Vietnamese text:
// "cau" matches "Cầu", "do uong" matches "Đồ uống".
export const normalizeSearchText = (s) =>
  (s || "")
    .toString()
    .toLowerCase()
    .replace(/đ/g, "d")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "") // combining diacritical marks U+0300–U+036F
    .trim();

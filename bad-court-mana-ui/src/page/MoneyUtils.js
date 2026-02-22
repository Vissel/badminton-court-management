export const VN_CURRENCY = "VND";

export const formatVND = (value) => {
  if (value === null || value === undefined || value === "") return "";

  // 1. Convert to number (handles "15000.0", 15000.5, or 15000)
  // If value is a string with dots like "15.000", remove them first
  const cleanValue =
    typeof value === "string" ? value.replace(/\./g, "") : value;
  const number = parseFloat(cleanValue);

  // 2. Check if the result is a valid number
  if (isNaN(number)) return "";

  // 3. Format using Intl.NumberFormat
  return new Intl.NumberFormat("vi-VN").format(number);
};
export const rawNumber = (value) => {
  return value.toString().replace(/\D/g, "");
};

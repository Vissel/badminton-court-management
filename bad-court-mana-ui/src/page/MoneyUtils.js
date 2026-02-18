
export const VN_CURRENCY = "VND";

export const formatVND = (value) => {
    if (!value) return "";
    const number = value.toString().replace(/\D/g, "");
    return new Intl.NumberFormat("vi-VN").format(number);
  };
export const rawNumber = (value) =>{
   return value.toString().replace(/\D/g, "");
}
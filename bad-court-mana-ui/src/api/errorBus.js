// Tiny pub/sub channel that lets non-React code (axios interceptors, api
// helpers) surface error messages through the single global ErrorPopup.
const listeners = new Set();

export const subscribeApiError = (listener) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const emitApiError = (message) => {
  listeners.forEach((listener) => listener(message));
};

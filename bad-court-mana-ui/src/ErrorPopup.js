import React, { useEffect, useState } from "react";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import { subscribeApiError } from "./api/errorBus";

// Standard backend-error popup: top-center filled red alert, manually closed
// (errors must not auto-hide). Any api call or component can show it by
// calling emitApiError(message) from api/errorBus.
const ErrorPopup = () => {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    return subscribeApiError((msg) => {
      setMessage(msg || "Đã xảy ra lỗi. Vui lòng thử lại.");
      setOpen(true);
    });
  }, []);

  return (
    <Snackbar
      open={open}
      anchorOrigin={{ vertical: "top", horizontal: "center" }}
      onClose={() => setOpen(false)}
    >
      <Alert
        severity="error"
        variant="filled"
        onClose={() => setOpen(false)}
        sx={{ width: "100%" }}
      >
        {message}
      </Alert>
    </Snackbar>
  );
};

export default ErrorPopup;

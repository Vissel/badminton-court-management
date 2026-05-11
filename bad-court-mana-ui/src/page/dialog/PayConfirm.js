import React from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import { TYPE } from "../HomePage";

const PayConfirm = ({ show, data, onConfirm, onExit }) => {
  if (!show || !data) return null;

  const confirmColor = data?.type === TYPE.PAY ? "primary" : "error";

  return (
    <Dialog
      open={show}
      onClose={(event, reason) => {
        if (reason === "backdropClick") return;
        onExit();
      }}
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle sx={{ whiteSpace: "pre-line" }}>{data.title}</DialogTitle>
      <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
        <Button variant="outlined" color="inherit" onClick={onExit}>
          Tắt
        </Button>
        <Button
          variant="contained"
          color={confirmColor}
          onClick={() => onConfirm(data)}
        >
          Xác nhận
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default PayConfirm;

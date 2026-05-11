import React from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";

const CancelConfirm = ({ show, courtId, onConfirm, onExit }) => {
  if (!show) return null;

  return (
    <Dialog
      open={show}
      onClose={(event, reason) => {
        if (reason === "backdropClick") return;
        onExit();
      }}
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle>Xác nhận huỷ trận đấu ?</DialogTitle>
      <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
        <Button variant="outlined" color="inherit" onClick={onExit}>
          Tắt
        </Button>
        <Button variant="contained" color="error" onClick={() => onConfirm(courtId)}>
          Xác nhận
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default CancelConfirm;

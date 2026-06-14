import React from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";

const RentCancelConfirm = ({ show, rental, onConfirm, onExit }) => {
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
            <DialogTitle>Xác nhận huỷ thuê theo giờ?</DialogTitle>
            {rental && (
                <DialogContent dividers>
                    <Stack spacing={1}>
                        <Typography variant="body2">
                            Sân: <strong>{rental.courtName || ""}</strong>
                        </Typography>
                        <Typography variant="body2">
                            Người chơi: <strong>{rental.playerName || ""}</strong>
                        </Typography>
                    </Stack>
                </DialogContent>
            )}
            <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
                <Button variant="outlined" color="inherit" onClick={onExit}>
                    Tắt
                </Button>
                <Button variant="contained" color="error" onClick={onConfirm}>
                    Xác nhận
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default RentCancelConfirm;

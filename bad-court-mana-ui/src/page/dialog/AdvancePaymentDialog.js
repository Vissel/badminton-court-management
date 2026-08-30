import React, { useState, useEffect } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Chip from "@mui/material/Chip";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import { formatVND } from "../MoneyUtils";

const QUICK_AMOUNTS = [50000, 100000, 150000, 200000];

const AdvancePaymentDialog = ({ show, playerName, onConfirm, onSkip, onClose }) => {
    const [amount, setAmount] = useState("");

    useEffect(() => {
        if (show) setAmount("");
    }, [show]);

    if (!show) return null;

    const handleChange = (e) => {
        const val = e.target.value;
        // Allow only digits
        if (val === "" || /^\d+$/.test(val)) {
            setAmount(val);
        }
    };

    const handleQuickAmount = (val) => {
        setAmount(String(val));
    };

    const handleConfirm = () => {
        const numericAmount = amount ? Number(amount) : 0;
        onConfirm(playerName, numericAmount);
    };

    const handleSkip = () => {
        onSkip(playerName);
    };

    const numericVal = amount ? Number(amount) : 0;

    return (
        <Dialog
            open={show}
            onClose={(event, reason) => {
                if (reason === "backdropClick") return;
                onClose();
            }}
            maxWidth="xs"
            fullWidth
            PaperProps={{ sx: { borderRadius: 2.5 } }}
        >
            <DialogTitle sx={{ pb: 0.5, display: "flex", alignItems: "center", gap: 1 }}>
                <PersonOutlinedIcon fontSize="small" color="primary" />
                <span>Trả trước cho <strong>{playerName}</strong></span>
            </DialogTitle>

            <DialogContent sx={{ pt: 1 }}>
                <TextField
                    autoFocus
                    fullWidth
                    size="small"
                    label="Số tiền trả trước"
                    placeholder="0"
                    value={amount}
                    onChange={handleChange}
                    sx={{ mb: 1.5 }}
                />

                {numericVal > 0 && (
                    <Typography variant="body2" color="primary" fontWeight={600} sx={{ mb: 1 }}>
                        {formatVND(numericVal)} ₫
                    </Typography>
                )}

                <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5, display: "block" }}>
                    Chọn nhanh:
                </Typography>
                <Box sx={{ display: "flex", gap: 0.75, flexWrap: "wrap" }}>
                    {QUICK_AMOUNTS.map((val) => (
                        <Chip
                            key={val}
                            label={`${val / 1000}K`}
                            size="small"
                            variant={amount === String(val) ? "filled" : "outlined"}
                            color={amount === String(val) ? "primary" : "default"}
                            onClick={() => handleQuickAmount(val)}
                            sx={{ cursor: "pointer", fontWeight: 600, fontSize: "0.78rem" }}
                        />
                    ))}
                </Box>
            </DialogContent>

            <DialogActions sx={{ px: 3, pb: 2, pt: 0.5, gap: 1 }}>
                <Button variant="outlined" color="inherit" onClick={handleSkip} sx={{ flex: 1 }}>
                    Bỏ qua
                </Button>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={handleConfirm}
                    disableElevation
                    sx={{ flex: 1, borderRadius: 1.5, fontWeight: 700 }}
                    onKeyPress={(e) => {
                        if (e.key === "Enter") {
                            e.preventDefault();
                            handleConfirm();
                        }
                    }}
                >
                    Xác nhận
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default AdvancePaymentDialog;

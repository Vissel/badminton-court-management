import React, { useEffect, useState } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";

const pad2 = (n) => String(n).padStart(2, "0");

const formatISOToHM = (iso) => {
    if (!iso) return "";
    const d = new Date(iso);
    return `${pad2(d.getUTCHours())}:${pad2(d.getUTCMinutes())}`;
};

const formatVND = (n) =>
    Number(n).toLocaleString("it-IT", { style: "currency", currency: "VND" });

const RentFinishConfirm = ({ show, rental, onConfirm, onExit }) => {
    const [shuttleList, setShuttleList] = useState([]);
    const [courtFee, setCourtFee] = useState(0);

    useEffect(() => {
        if (show && rental) {
            setCourtFee(rental.fee || 0);
            if (rental.shuttleBalls) {
                setShuttleList(
                    rental.shuttleBalls.map((b) => ({
                        shuttleName: b.shuttleName,
                        cost: b.cost,
                        quantity: b.number || b.ballQuantity || 0,
                    }))
                );
            }
        }
    }, [show, rental]);

    const handleFeeChange = (val) => {
        const n = parseFloat(val);
        setCourtFee(isNaN(n) || n < 0 ? 0 : n);
    };

    const handleQuantityChange = (idx, val) => {
        const n = parseInt(val, 10);
        setShuttleList((prev) =>
            prev.map((s, i) =>
                i === idx ? { ...s, quantity: isNaN(n) || n < 0 ? 0 : n } : s
            )
        );
    };

    const shuttleTotal = shuttleList.reduce((sum, s) => sum + s.cost * s.quantity, 0);
    const totalFee = courtFee + shuttleTotal;

    if (!show) return null;

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
            <DialogTitle align="center">Xác nhận kết thúc thuê theo giờ</DialogTitle>
            {rental && (
                <DialogContent dividers>
                    <Stack spacing={2}>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={700} sx={{ minWidth: 100 }}>Sân:</Typography>
                            <Typography>{rental.courtName || ""}</Typography>
                        </Stack>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={700} sx={{ minWidth: 100 }}>Người chơi:</Typography>
                            <Typography>{rental.playerName || ""}</Typography>
                        </Stack>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={700} sx={{ minWidth: 100 }}>Bắt đầu:</Typography>
                            <Typography>{formatISOToHM(rental.startTime)}</Typography>
                        </Stack>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={700} sx={{ minWidth: 100 }}>Kết thúc:</Typography>
                            <Typography>{formatISOToHM(rental.endTime)}</Typography>
                        </Stack>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={700} sx={{ minWidth: 100 }}>Số giờ:</Typography>
                            <Typography>{rental.numTime ? `${rental.numTime}h` : ""}</Typography>
                        </Stack>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={700} sx={{ minWidth: 100 }}>Phí sân:</Typography>
                            <TextField
                                size="small"
                                type="number"
                                inputProps={{ min: 0, step: 10000 }}
                                value={courtFee}
                                onChange={(e) => handleFeeChange(e.target.value)}
                                sx={{ flex: 1 }}
                            />
                        </Stack>

                        {/* Editable shuttle ball list — same pattern as GameDialog */}
                        <Stack direction="row" spacing={1} alignItems="flex-start">
                            <Typography fontWeight={700} sx={{ minWidth: 100, pt: 1 }}>Cầu:</Typography>
                            <Stack sx={{ flex: 1 }}>
                                {shuttleList.map((s, idx) => (
                                    <Stack key={idx} direction="row" spacing={1} sx={{ mb: 1 }}>
                                        <TextField size="small" value={s.shuttleName} disabled sx={{ flex: 2 }} />
                                        <TextField size="small" value={formatVND(s.cost)} disabled sx={{ flex: 1 }} />
                                        <TextField
                                            size="small"
                                            type="number"
                                            inputProps={{ min: 0 }}
                                            value={s.quantity}
                                            onChange={(e) => handleQuantityChange(idx, e.target.value)}
                                            sx={{ flex: 1 }}
                                        />
                                    </Stack>
                                ))}
                            </Stack>
                        </Stack>

                        {/* Total fee */}
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={700} sx={{ minWidth: 100 }}>Tổng cộng:</Typography>
                            <Typography fontWeight={700} color="primary">
                                {formatVND(totalFee)}
                            </Typography>
                        </Stack>
                    </Stack>
                </DialogContent>
            )}
            <DialogActions sx={{ px: 3, py: 2, gap: 1 }}>
                <Button variant="outlined" color="inherit" onClick={onExit}>
                    Tắt
                </Button>
                <Button variant="contained" color="success" onClick={() => onConfirm({ courtFee, shuttleList })}>
                    Xác nhận kết thúc
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default RentFinishConfirm;

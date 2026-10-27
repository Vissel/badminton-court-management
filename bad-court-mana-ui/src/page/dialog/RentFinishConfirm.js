import React, { useEffect, useState } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Tooltip from "@mui/material/Tooltip";
import DraggableResizablePaper, { DIALOG_DRAG_HANDLE } from "./DraggableResizablePaper";

const pad2 = (n) => String(n).padStart(2, "0");

const formatISOToHM = (iso) => {
    if (!iso) return "";
    const d = new Date(iso);
    return `${pad2(d.getUTCHours())}:${pad2(d.getUTCMinutes())}`;
};

/** ISO string -> minutes since midnight, using UTC fields (backend wall-clock-as-UTC convention). */
const isoToMin = (iso) => {
    if (!iso) return null;
    const d = new Date(iso);
    return d.getUTCHours() * 60 + d.getUTCMinutes();
};

/** Minutes between two clock-minute values (positive; wraps overnight). */
const diffMin = (a, b) => {
    let x = b - a;
    if (x < 0) x += 24 * 60;
    return x;
};

/** ISO string for the current wall-clock time, matching the backend's UTC+7-as-UTC convention. */
const nowWallClockISO = () => {
    const now = new Date();
    const d = new Date();
    d.setUTCHours(now.getHours(), now.getMinutes(), 0, 0);
    return d.toISOString();
};

const formatVND = (n) =>
    Number(n).toLocaleString("it-IT", { style: "currency", currency: "VND" });

/** Decimal hours -> "Xh Ym" (omitting zero parts). */
const formatHoursMinutes = (hours) => {
    if (!hours) return "";
    const totalMin = Math.round(hours * 60);
    const h = Math.floor(totalMin / 60);
    const m = totalMin % 60;
    if (h && m) return `${h}h ${m}m`;
    if (h) return `${h}h`;
    return `${m}m`;
};

/** Compute the settled end time (now), prorated court fee, and elapsed hours for a rental. */
const computeSettleNow = (rental) => {
    const iso = nowWallClockISO();
    const sMin = isoToMin(rental?.startTime);
    const bookedMin = isoToMin(rental?.endTime);
    const nowMin = isoToMin(iso);
    let fee = rental?.fee || 0;
    let numTime = rental?.numTime || 0;
    if (sMin != null && bookedMin != null && nowMin != null) {
        const booked = diffMin(sMin, bookedMin);
        const elapsed = diffMin(sMin, nowMin);
        if (booked > 0) {
            fee = Math.round(((rental.fee || 0) * elapsed) / booked);
            numTime = Math.round((elapsed / 60) * 100) / 100;
        }
    }
    return { iso, fee, numTime };
};

const RentFinishConfirm = ({ show, rental, onConfirm, onExit }) => {
    const [shuttleList, setShuttleList] = useState([]);
    const [courtFee, setCourtFee] = useState(0);
    const [endISO, setEndISO] = useState(null);
    const [numTime, setNumTime] = useState(0);

    useEffect(() => {
        if (show && rental) {
            const { iso, fee, numTime: settledNumTime } = computeSettleNow(rental);
            setCourtFee(fee);
            setEndISO(iso);
            setNumTime(settledNumTime);
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

    /** Override the fee field with the originally-booked rent-by-time amount. */
    const handleUseOriginal = () => {
        setCourtFee(rental?.fee || 0);
        setEndISO(rental?.endTime || null);
        setNumTime(rental?.numTime || 0);
    };

    const originalMoney = rental?.fee || 0;

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
            PaperComponent={DraggableResizablePaper}
        >
            <DialogTitle align="center" className={DIALOG_DRAG_HANDLE} sx={{ cursor: "move" }}>Xác nhận kết thúc thuê theo giờ</DialogTitle>
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
                            <Typography sx={{ flex: 1 }}>{formatISOToHM(endISO)}</Typography>
                        </Stack>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography fontWeight={700} sx={{ minWidth: 100 }}>Số giờ:</Typography>
                            <Typography>{formatHoursMinutes(numTime)}</Typography>
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
                            {originalMoney > 0 && (
                                <Tooltip title="Bấm để dùng phí gốc của dịch vụ thuê theo giờ" placement="top">
                                    <Typography
                                        onClick={handleUseOriginal}
                                        sx={{
                                            minWidth: 96,
                                            textAlign: "right",
                                            cursor: "pointer",
                                            color: "primary.main",
                                            textDecoration: "underline dotted",
                                            "&:hover": { color: "primary.dark" },
                                        }}
                                    >
                                        Gốc: {formatVND(originalMoney)}
                                    </Typography>
                                </Tooltip>
                            )}
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
                            <Typography fontWeight={900} color="primary">
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
                <Button variant="contained" color="success" onClick={() => onConfirm({ courtFee, shuttleList, endTime: endISO, numTime })}>
                    Xác nhận kết thúc
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default RentFinishConfirm;

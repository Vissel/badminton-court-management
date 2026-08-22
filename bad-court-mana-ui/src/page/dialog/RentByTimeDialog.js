import React, { useEffect, useState } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import Box from "@mui/material/Box";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import { formatVND } from "../MoneyUtils";

// ── Pure helpers (no React state) ──────────────────────────────────────────

const pad2 = (n) => String(Math.abs(n)).padStart(2, "0");

/** HH:mm -> total minutes */
const hm2min = (h, m) => h * 60 + m;

/** Total minutes -> { h, m } */
const min2hm = (totalMin) => ({
    h: Math.floor(totalMin / 60),
    m: totalMin % 60,
});

/** Add duration minutes to a start time; wraps to 0-23 for display. */
const addMinutes = (startH, startM, durationMin) => {
    const total = hm2min(startH, startM) + durationMin;
    return { h: Math.floor(total / 60) % 24, m: total % 60 };
};

/** Minutes between start and end (positive; wraps overnight). */
const diffMin = (startH, startM, endH, endM) => {
    let d = hm2min(endH, endM) - hm2min(startH, startM);
    if (d < 0) d += 24 * 60;
    return d;
};

/** Fee (VND) from total duration minutes. */
const minutesToFee = (totalMin, hourlyRate) => Math.round((totalMin / 60) * hourlyRate);

/** Extract { h, m } from an ISO string using UTC fields (backend stores UTC+7 wall-clock as UTC label). */
const timeFromISO = (iso) => {
    if (!iso) return { h: 0, m: 0 };
    const d = new Date(iso);
    return { h: d.getUTCHours(), m: d.getUTCMinutes() };
};

/** Build ISO string using UTC hours/minutes so the value matches backend's UTC+7-as-UTC convention. */
const toISO = (h, m) => {
    const d = new Date();
    d.setUTCHours(h, m, 0, 0);
    return d.toISOString();
};

/**
 * Build ISO string for end time using UTC fields.
 * If end HH:mm ≤ start HH:mm the end is assumed to be the next day.
 */
const endToISO = (startH, startM, endH, endM) => {
    const start = new Date();
    start.setUTCHours(startH, startM, 0, 0);
    const end = new Date();
    end.setUTCHours(endH, endM, 0, 0);
    if (end <= start) end.setUTCDate(end.getUTCDate() + 1);
    return end.toISOString();
};

// ────────────────────────────────────────────────────────────────────────────

const RentByTimeDialog = ({
    show,
    courtId,
    courtName,
    playerName,
    ballOptions,
    editMode = false,
    initialData = null,
    hourlyRate = 100000,
    onConfirm,
    onExit,
}) => {
    // Start time
    const [startH, setStartH] = useState(0);
    const [startM, setStartM] = useState(0);
    // Duration
    const [durH, setDurH] = useState(1);
    const [durM, setDurM] = useState(0);
    // End time
    const [endH, setEndH] = useState(1);
    const [endM, setEndM] = useState(0);
    // Fee
    const [fee, setFee] = useState(hourlyRate);
    // Player name (editable)
    const [localPlayerName, setLocalPlayerName] = useState("");
    // Shuttles
    const [addedShuttles, setAddedShuttles] = useState([]);
    const [selectedBallName, setSelectedBallName] = useState("");

    // ── Initialise ──────────────────────────────────────────────────────────
    useEffect(() => {
        if (!show) return;
        if (editMode && initialData) {
            const st = timeFromISO(initialData.startTime);
            const et = timeFromISO(initialData.endTime);
            const totalMin = Math.round((initialData.numTime || 1) * 60);
            const dur = min2hm(totalMin);
            setStartH(st.h); setStartM(st.m);
            setEndH(et.h); setEndM(et.m);
            setDurH(dur.h); setDurM(dur.m);
            setFee(minutesToFee(totalMin, hourlyRate));
            setLocalPlayerName(playerName || "");
            setAddedShuttles(
                (initialData.shuttleBalls || []).map((b) => ({
                    shuttleName: b.shuttleName,
                    cost: b.cost,
                    costFormat: formatVND(b.cost),
                    number: b.number || 0,
                }))
            );
        } else {
            const now = new Date();
            const sh = now.getHours();
            const sm = now.getMinutes();
            const end = addMinutes(sh, sm, 60);
            setStartH(sh); setStartM(sm);
            setDurH(1); setDurM(0);
            setEndH(end.h); setEndM(end.m);
            setFee(hourlyRate);
            setLocalPlayerName(playerName || "");
            setAddedShuttles([]);
        }
        setSelectedBallName("");
    }, [show, editMode, initialData, hourlyRate, playerName]);

    // ── Cross-field handlers ────────────────────────────────────────────────

    /** Admin changes start time → shift end time, keep duration. */
    const handleStartChange = (h, m) => {
        setStartH(h); setStartM(m);
        const end = addMinutes(h, m, hm2min(durH, durM));
        setEndH(end.h); setEndM(end.m);
    };

    /** Admin changes duration → recalculate end time + fee. */
    const handleDurChange = (dh, dm) => {
        const clampedDm = Math.min(59, Math.max(0, dm));
        const clampedDh = Math.max(0, dh);
        setDurH(clampedDh); setDurM(clampedDm);
        const totalMin = hm2min(clampedDh, clampedDm);
        const end = addMinutes(startH, startM, totalMin);
        setEndH(end.h); setEndM(end.m);
        setFee(minutesToFee(totalMin, hourlyRate));
    };

    /** Admin changes end time → recalculate duration + fee. */
    const handleEndChange = (h, m) => {
        setEndH(h); setEndM(m);
        const totalMin = diffMin(startH, startM, h, m);
        const dur = min2hm(totalMin);
        setDurH(dur.h); setDurM(dur.m);
        setFee(minutesToFee(totalMin, hourlyRate));
    };

    /** Admin changes fee → back-derive duration + end time. */
    const handleFeeChange = (val) => {
        const f = Math.max(0, parseInt(val, 10) || 0);
        setFee(f);
        const totalMin = Math.round((f / hourlyRate) * 60);
        const dur = min2hm(totalMin);
        setDurH(dur.h); setDurM(dur.m);
        const end = addMinutes(startH, startM, totalMin);
        setEndH(end.h); setEndM(end.m);
    };

    // ── Shuttle handlers ────────────────────────────────────────────────────
    const availableBallOptions = ballOptions.filter(
        (b) => !addedShuttles.some((s) => s.shuttleName === b.shuttleName)
    );

    const handleAddShuttle = () => {
        if (!selectedBallName) return;
        const ball = ballOptions.find((b) => b.shuttleName === selectedBallName);
        if (!ball) return;
        setAddedShuttles((prev) => [
            ...prev,
            { shuttleName: ball.shuttleName, cost: ball.cost, costFormat: ball.costFormat, number: 1 },
        ]);
        setSelectedBallName("");
    };

    const handleShuttleNumberChange = (idx, val) => {
        const n = parseInt(val, 10);
        setAddedShuttles((prev) =>
            prev.map((s, i) => (i === idx ? { ...s, number: isNaN(n) || n < 0 ? 0 : n } : s))
        );
    };

    const handleRemoveShuttle = (idx) => {
        setAddedShuttles((prev) => prev.filter((_, i) => i !== idx));
    };

    // ── Confirm ─────────────────────────────────────────────────────────────
    const handleConfirm = () => {
        const totalMin = hm2min(durH, durM);
        const numTime = totalMin / 60; // decimal hours for backend
        const ballList = addedShuttles
            .filter((s) => s.number > 0)
            .map((s) => ({ shuttleName: s.shuttleName, shuttleCost: s.cost, ballQuantity: s.number }));

        onConfirm({
            courtId,
            courtName,
            playerName: localPlayerName,
            numTime: numTime,
            startTime: toISO(startH, startM),
            endTime: endToISO(startH, startM, endH, endM),
            fee,
            shuttleBalls: ballList,
            editMode,
        });
    };

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
            <DialogTitle align="center">
                {editMode ? "Cập nhật thuê theo giờ" : "Thuê theo giờ"}
            </DialogTitle>

            <DialogContent dividers>
                <Stack spacing={2}>
                    {/* Court */}
                    <Stack direction="row" spacing={1} alignItems="center">
                        <Typography fontWeight={700} sx={{ minWidth: 100 }}>Sân:</Typography>
                        <TextField size="small" value={courtName} disabled fullWidth />
                    </Stack>

                    {/* Player */}
                    <Stack direction="row" spacing={1} alignItems="center">
                        <Typography fontWeight={700} sx={{ minWidth: 100 }}>Người chơi:</Typography>
                        <TextField
                            size="small"
                            value={localPlayerName}
                            onChange={(e) => setLocalPlayerName(e.target.value)}
                            placeholder="Nhập tên người chơi"
                            fullWidth
                        />
                    </Stack>

                    {/* Start Time — time-only picker */}
                    <Stack direction="row" spacing={1} alignItems="center">
                        <Typography fontWeight={700} sx={{ minWidth: 100 }}>Bắt đầu:</Typography>
                        <TextField
                            size="small"
                            type="time"
                            value={`${pad2(startH)}:${pad2(startM)}`}
                            onChange={(e) => {
                                const [h, m] = (e.target.value || "00:00").split(":").map(Number);
                                handleStartChange(isNaN(h) ? 0 : h, isNaN(m) ? 0 : m);
                            }}
                            fullWidth
                        />
                    </Stack>

                    {/* Duration — split into Giờ + Phút */}
                    <Stack direction="row" spacing={1} alignItems="center">
                        <Typography fontWeight={700} sx={{ minWidth: 100 }}>Thời gian:</Typography>
                        <TextField
                            size="small"
                            type="number"
                            label="Giờ"
                            inputProps={{ min: 0, step: 1 }}
                            value={durH}
                            onChange={(e) => handleDurChange(parseInt(e.target.value, 10) || 0, durM)}
                            sx={{ flex: 1 }}
                        />
                        <TextField
                            size="small"
                            type="number"
                            label="Phút"
                            inputProps={{ min: 0, max: 59, step: 1 }}
                            value={durM}
                            onChange={(e) => handleDurChange(durH, parseInt(e.target.value, 10) || 0)}
                            sx={{ flex: 1 }}
                        />
                    </Stack>

                    {/* End Time — time-only picker */}
                    <Stack direction="row" spacing={1} alignItems="center">
                        <Typography fontWeight={700} sx={{ minWidth: 100 }}>Kết thúc:</Typography>
                        <TextField
                            size="small"
                            type="time"
                            value={`${pad2(endH)}:${pad2(endM)}`}
                            onChange={(e) => {
                                const [h, m] = (e.target.value || "00:00").split(":").map(Number);
                                handleEndChange(isNaN(h) ? 0 : h, isNaN(m) ? 0 : m);
                            }}
                            fullWidth
                        />
                    </Stack>

                    {/* Fee — editable, back-derives duration */}
                    <Stack direction="row" spacing={1} alignItems="center">
                        <Typography fontWeight={700} sx={{ minWidth: 100 }}>Phí:</Typography>
                        <TextField
                            size="small"
                            type="number"
                            inputProps={{ min: 0, step: 50000 }}
                            value={fee}
                            onChange={(e) => handleFeeChange(e.target.value)}
                            fullWidth
                        />
                    </Stack>

                    {/* Shuttle Section */}
                    <Box>
                        <Typography fontWeight={700} gutterBottom>Chọn cầu:</Typography>

                        {/* Dropdown + Add */}
                        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1.5 }}>
                            <FormControl size="small" fullWidth>
                                <InputLabel>Loại cầu</InputLabel>
                                <Select
                                    label="Loại cầu"
                                    value={selectedBallName}
                                    onChange={(e) => setSelectedBallName(e.target.value)}
                                >
                                    {availableBallOptions.map((b) => (
                                        <MenuItem key={b.shuttleName} value={b.shuttleName}>
                                            {b.shuttleName} — {b.costFormat}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                            <Button
                                variant="contained"
                                onClick={handleAddShuttle}
                                disabled={!selectedBallName}
                                sx={{ whiteSpace: "nowrap" }}
                            >
                                Thêm
                            </Button>
                        </Stack>

                        {/* Added shuttles list */}
                        {addedShuttles.length > 0 && (
                            <Stack spacing={1}>
                                {addedShuttles.map((s, idx) => (
                                    <Stack key={s.shuttleName} direction="row" spacing={1} alignItems="center">
                                        <TextField size="small" value={s.shuttleName} disabled sx={{ flex: 2 }} />
                                        <TextField size="small" value={s.costFormat} disabled sx={{ flex: 1 }} />
                                        <TextField
                                            size="small"
                                            type="number"
                                            inputProps={{ min: 0 }}
                                            value={s.number}
                                            onChange={(e) => handleShuttleNumberChange(idx, e.target.value)}
                                            sx={{ flex: 1 }}
                                        />
                                        <IconButton size="small" color="error" onClick={() => handleRemoveShuttle(idx)}>
                                            <DeleteIcon fontSize="small" />
                                        </IconButton>
                                    </Stack>
                                ))}
                            </Stack>
                        )}
                    </Box>
                </Stack>
            </DialogContent>

            <DialogActions sx={{ px: 3, py: 2, gap: 1 }}>
                <Button variant="outlined" color="inherit" onClick={onExit}>Hủy</Button>
                <Button variant="contained" color="success" onClick={handleConfirm}>
                    {editMode ? "Cập nhật" : "Bắt đầu"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default RentByTimeDialog;

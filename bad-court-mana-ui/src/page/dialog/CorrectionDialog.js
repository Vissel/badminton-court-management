import React, { useEffect, useState } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import Divider from "@mui/material/Divider";
import Alert from "@mui/material/Alert";
import IconButton from "@mui/material/IconButton";
import AddIcon from "@mui/icons-material/Add";
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import { adjustBill, replaceBill } from "../../api/billApi";
import { emitApiError } from "../../api/errorBus";
import DraggableResizablePaper, { DIALOG_DRAG_HANDLE } from "./DraggableResizablePaper";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";

const emptyLine = () => ({ itemName: "", amount: "" });

/**
 * Điều chỉnh / thay thế hoá đơn (G-1). Adjust sends only the delta lines;
 * replace sends the full new content (prefilled from the original items).
 * Both produce a NEW bill that keeps the original number intact.
 */
const CorrectionDialog = ({ show, bill, mode, onClose, onDone }) => {
    const [reason, setReason] = useState("");
    const [lines, setLines] = useState([emptyLine()]);
    const [saving, setSaving] = useState(false);
    const isReplace = mode === "REPLACE";

    useEffect(() => {
        if (!show) return;
        setReason("");
        if (isReplace) {
            // Replace = full replacement, so start from the original content.
            const items = (bill?.items || []).map((item) => ({
                itemName: item.itemName || "",
                amount: String(item.amount ?? ""),
            }));
            setLines(items.length ? items : [emptyLine()]);
        } else {
            setLines([emptyLine()]);
        }
    }, [show, isReplace, bill]);

    const setLine = (index, key, value) =>
        setLines((prev) => prev.map((l, i) => (i === index ? { ...l, [key]: value } : l)));

    const addLine = () => setLines((prev) => [...prev, emptyLine()]);
    const removeLine = (index) =>
        setLines((prev) => (prev.length === 1 ? prev : prev.filter((_, i) => i !== index)));

    const delta = lines.reduce((sum, l) => sum + (Number(l.amount) || 0), 0);

    const submit = () => {
        if (!reason.trim()) {
            emitApiError(isReplace ? "Vui lòng nhập lý do thay thế." : "Vui lòng nhập lý do điều chỉnh.");
            return;
        }
        const items = lines
            .filter((l) => l.itemName.trim() && l.amount !== "")
            .map((l) => ({ itemName: l.itemName.trim(), amount: Number(l.amount) }));
        if (items.length === 0) {
            emitApiError("Vui lòng nhập nội dung điều chỉnh.");
            return;
        }
        const payload = { reason: reason.trim(), items };
        setSaving(true);
        (isReplace ? replaceBill(bill.billId, payload) : adjustBill(bill.billId, payload))
            .then((res) => {
                const result = res?.data;
                if (result?.success && result.data) {
                    onDone?.(result.data);
                    onClose?.();
                } else {
                    emitApiError(
                        result?.errorMessage ||
                        (isReplace ? "Lập hoá đơn thay thế không thành công." : "Lập hoá đơn điều chỉnh không thành công.")
                    );
                }
            })
            .catch(() => { })
            .finally(() => setSaving(false));
    };

    return (
        <Dialog
            open={show}
            onClose={saving ? undefined : onClose}
            maxWidth="sm"
            fullWidth
            PaperComponent={DraggableResizablePaper}
        >
            <DialogTitle className={DIALOG_DRAG_HANDLE} sx={{ cursor: "move" }}>
                {isReplace ? "Lập hoá đơn thay thế" : "Lập hoá đơn điều chỉnh"} — {bill?.billNo}
            </DialogTitle>
            <DialogContent dividers>
                <Stack spacing={1.5}>
                    <Alert severity="info" icon={false}>
                        {isReplace
                            ? "Hoá đơn gốc sẽ được đánh dấu đã huỷ (lý do: thay thế) sau khi hoá đơn mới được lập."
                            : "Hoá đơn gốc giữ nguyên số. Hoá đơn điều chỉnh chỉ ghi phần chênh lệch (âm nếu giảm)."}
                    </Alert>

                    <Typography variant="body2" color="text.secondary">
                        Khách hàng: {bill?.buyerName || bill?.playerName || "—"} · Tổng gốc:{" "}
                        {formatVND(bill?.total)} {VN_CURRENCY}
                    </Typography>

                    <TextField
                        autoFocus
                        fullWidth
                        size="small"
                        label={isReplace ? "Lý do thay thế" : "Lý do điều chỉnh"}
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        multiline
                        minRows={2}
                    />

                    <Divider />
                    <Typography variant="subtitle2">Nội dung</Typography>
                    <Stack spacing={1}>
                        {lines.map((line, index) => (
                            <Stack key={index} direction="row" spacing={1} alignItems="center">
                                <TextField
                                    size="small"
                                    label="Nội dung"
                                    value={line.itemName}
                                    onChange={(e) => setLine(index, "itemName", e.target.value)}
                                    fullWidth
                                />
                                <TextField
                                    size="small"
                                    label={`Số tiền (${VN_CURRENCY})`}
                                    type="number"
                                    value={line.amount}
                                    onChange={(e) => setLine(index, "amount", e.target.value)}
                                    sx={{ width: 180 }}
                                    helperText={!isReplace ? "Âm nếu giảm" : undefined}
                                    FormHelperTextProps={{ sx: { whiteSpace: "nowrap" } }}
                                />
                                <IconButton size="small" onClick={() => removeLine(index)} disabled={lines.length === 1}>
                                    <DeleteOutlinedIcon fontSize="small" />
                                </IconButton>
                            </Stack>
                        ))}
                    </Stack>
                    <Button size="small" startIcon={<AddIcon />} onClick={addLine} sx={{ alignSelf: "flex-start" }}>
                        Thêm dòng
                    </Button>

                    {!isReplace && (
                        <Typography variant="body2" fontWeight={600} align="right">
                            Chênh lệch: {formatVND(delta)} {VN_CURRENCY}
                        </Typography>
                    )}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={saving}>
                    Đóng
                </Button>
                <Button variant="contained" onClick={submit} disabled={saving}>
                    {isReplace ? "Lập hoá đơn thay thế" : "Lập hoá đơn điều chỉnh"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default CorrectionDialog;

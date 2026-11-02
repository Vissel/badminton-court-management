import React, { useEffect, useState } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Grid from "@mui/material/Grid";
import { saveCustomer } from "../../api/customerApi";
import { emitApiError } from "../../api/errorBus";
import DraggableResizablePaper, { DIALOG_DRAG_HANDLE } from "./DraggableResizablePaper";

const EMPTY_CUSTOMER = {
    name: "",
    company: "",
    taxCode: "",
    address: "",
    phone: "",
    email: "",
    bankName: "",
    bankAccount: "",
};

/**
 * Buyer directory entry (G-9). Saves through POST /api/v1/customers — the
 * customer_id decides insert vs update, so the same dialog edits.
 */
const CustomerDialog = ({ show, customer, onClose, onDone }) => {
    const [form, setForm] = useState(EMPTY_CUSTOMER);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (!show) return;
        setForm({
            ...EMPTY_CUSTOMER,
            ...Object.fromEntries(
                Object.keys(EMPTY_CUSTOMER).map((key) => [key, customer?.[key] || ""])
            ),
        });
    }, [show, customer]);

    const setField = (field, value) => setForm((prev) => ({ ...prev, [field]: value }));

    const submit = () => {
        if (!form.name.trim()) {
            emitApiError("Vui lòng nhập tên khách hàng.");
            return;
        }
        const payload = {
            customerId: customer?.customerId || null,
            ...Object.fromEntries(
                Object.entries(form).map(([key, value]) => [key, value.trim() || null])
            ),
            name: form.name.trim(),
            active: true,
        };
        setSaving(true);
        saveCustomer(payload)
            .then((res) => {
                const result = res?.data;
                if (result?.success) {
                    onDone?.();
                    onClose?.();
                } else {
                    emitApiError(result?.errorMessage || "Lưu khách hàng thất bại.");
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
                {customer?.customerId ? "Sửa khách hàng" : "Thêm khách hàng"}
            </DialogTitle>
            <DialogContent dividers>
                <Grid container spacing={1.5}>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField
                            autoFocus
                            fullWidth
                            size="small"
                            label="Tên khách hàng"
                            value={form.name}
                            onChange={(e) => setField("name", e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Tên công ty"
                            value={form.company}
                            onChange={(e) => setField("company", e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Mã số thuế"
                            value={form.taxCode}
                            onChange={(e) => setField("taxCode", e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Điện thoại"
                            value={form.phone}
                            onChange={(e) => setField("phone", e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12 }}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Địa chỉ"
                            value={form.address}
                            onChange={(e) => setField("address", e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12 }}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Email nhận HĐĐT"
                            value={form.email}
                            onChange={(e) => setField("email", e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Ngân hàng"
                            value={form.bankName}
                            onChange={(e) => setField("bankName", e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField
                            fullWidth
                            size="small"
                            label="Số tài khoản"
                            value={form.bankAccount}
                            onChange={(e) => setField("bankAccount", e.target.value)}
                        />
                    </Grid>
                </Grid>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={saving}>
                    Đóng
                </Button>
                <Button variant="contained" onClick={submit} disabled={saving}>
                    Lưu
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default CustomerDialog;

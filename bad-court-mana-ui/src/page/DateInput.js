import React, { useEffect, useRef, useState } from "react";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import TextField from "@mui/material/TextField";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

const pad2 = (value) => String(value).padStart(2, "0");

/** ISO value "yyyy-MM-dd" -> the "dd/MM/yyyy" text the user sees. */
export const isoToDisplay = (iso) => {
    const match = ISO_DATE.exec(String(iso ?? "").trim());
    return match ? `${match[3]}/${match[2]}/${match[1]}` : "";
};

/** "dd/MM/yyyy" text -> "yyyy-MM-dd"; null when the text is not a real date. */
export const displayToIso = (text) => {
    const match = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(String(text ?? "").trim());
    if (!match) return null;
    const day = Number(match[1]);
    const month = Number(match[2]);
    const year = Number(match[3]);
    if (year < 1900 || month < 1 || month > 12 || day < 1 || day > 31) return null;
    // Rejects impossible dates such as 31/02 that Date would roll into March.
    const probe = new Date(Date.UTC(year, month - 1, day));
    if (probe.getUTCMonth() !== month - 1 || probe.getUTCDate() !== day) return null;
    return `${year}-${pad2(month)}-${pad2(day)}`;
};

/**
 * Keeps the typed text in dd/MM/yyyy shape. A plain digit run is separated
 * automatically ("03112026" -> "03/11/2026"), while explicit separators typed
 * or pasted by the user are preserved ("3/11/2026").
 */
export const maskDateInput = (text) => {
    const cleaned = String(text ?? "").replace(/[^\d/]/g, "").slice(0, 10);
    if (cleaned.includes("/")) return cleaned;
    return [cleaned.slice(0, 2), cleaned.slice(2, 4), cleaned.slice(4, 8)]
        .filter((part) => part.length > 0)
        .join("/");
};

/**
 * Date field that always shows dd/MM/yyyy (the browser locale would render a
 * native date input as mm/dd/yyyy). Callers keep working with ISO
 * "yyyy-MM-dd" values, which is what the backend filters expect.
 */
function DateInput({ value, onChange, disabled, ...rest }) {
    const [text, setText] = useState(() => isoToDisplay(value));
    const nativeRef = useRef(null);

    useEffect(() => {
        setText(isoToDisplay(value));
    }, [value]);

    const handleChange = (event) => {
        const masked = maskDateInput(event.target.value);
        setText(masked);
        if (!masked) {
            onChange("");
            return;
        }
        const iso = displayToIso(masked);
        if (iso) onChange(iso);
    };

    const handleBlur = () => {
        if (!text.trim()) {
            setText("");
            onChange("");
            return;
        }
        const iso = displayToIso(text);
        if (iso) {
            setText(isoToDisplay(iso));
            onChange(iso);
            return;
        }
        // Partial or impossible date: fall back to the last committed value.
        setText(isoToDisplay(value));
    };

    const openNativePicker = () => {
        const input = nativeRef.current;
        if (!input) return;
        if (typeof input.showPicker === "function") {
            try {
                input.showPicker();
                return;
            } catch {
                // Some browsers refuse showPicker outside a trusted gesture.
            }
        }
        input.focus();
        input.click();
    };

    return (
        <TextField
            placeholder="dd/mm/yyyy"
            {...rest}
            disabled={disabled}
            value={text}
            onChange={handleChange}
            onBlur={handleBlur}
            onKeyDown={(event) => {
                if (event.key === "Enter") event.currentTarget.blur();
                rest.onKeyDown?.(event);
            }}
            slotProps={{
                ...rest.slotProps,
                inputLabel: { shrink: true, ...rest.slotProps?.inputLabel },
                htmlInput: { maxLength: 10, inputMode: "numeric", ...rest.slotProps?.htmlInput },
                input: {
                    ...rest.slotProps?.input,
                    endAdornment: (
                        <InputAdornment position="end">
                            <Box sx={{ position: "relative", display: "inline-flex" }}>
                                <IconButton
                                    size="small"
                                    edge="end"
                                    aria-label="Chọn ngày"
                                    onClick={openNativePicker}
                                    disabled={disabled}
                                    tabIndex={-1}
                                >
                                    <CalendarMonthIcon fontSize="small" />
                                </IconButton>
                                {/* Native picker hidden behind the icon: it supplies the calendar
                    while the visible text stays dd/MM/yyyy. */}
                                <Box
                                    component="input"
                                    type="date"
                                    ref={nativeRef}
                                    value={value || ""}
                                    onChange={(event) => onChange(event.target.value)}
                                    disabled={disabled}
                                    tabIndex={-1}
                                    aria-hidden="true"
                                    sx={{
                                        position: "absolute",
                                        inset: 0,
                                        width: "100%",
                                        height: "100%",
                                        opacity: 0,
                                        border: 0,
                                        p: 0,
                                        m: 0,
                                        pointerEvents: "none",
                                    }}
                                />
                            </Box>
                        </InputAdornment>
                    ),
                },
            }}
        />
    );
}

export default DateInput;

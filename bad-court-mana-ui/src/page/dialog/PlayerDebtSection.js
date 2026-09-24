import React, { useState, useEffect, useCallback } from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import CloseIcon from "@mui/icons-material/Close";
import { getDebitSummary } from "../../api/debtApi";
import { VN_CURRENCY, formatVND } from "../MoneyUtils";
import DebitListDialog from "./DebitListDialog";

/**
 * Self-contained debt block for a player. Covers the whole debt flow:
 *  - summary chip ("Nợ: …") or "Không có nợ"
 *  - click the chip → DebitListDialog: remaining-debt list + prePay preview + pay
 *  - "+ Ghi nợ" → input row to record new debts during a payment
 *  - the debts recorded so far, each removable
 *
 * Props:
 *  playerName      player whose debt is shown
 *  active          falsy → block resets and nothing is fetched (dialog hidden)
 *  allowRecord     show the "+ Ghi nợ" affordance (payment flows only)
 *  payableAmount   amount still payable before any debt is recorded; caps the
 *                  total that may be converted into debt (deferred mode only)
 *  onDebtsChange   ({ debtList, debtNote, totalRecordedDebt, pendingDebt,
 *                    totalDebitAmount }) => void — fired whenever they change
 *  onRecordDebt    async (amount, note) => bool — when provided, "Ghi nợ"
 *                  creates the debt immediately via this callback instead of
 *                  buffering it locally; the summary refreshes on success
 *  leading         node rendered at the left of the chips row
 *  leadingFill     leading takes a full row (chips wrap to a second line,
 *                  right-aligned) — use when leading is a wide control like
 *                  a rename input
 *  chipRowSx       extra sx merged into the chips row
 */
const PlayerDebtSection = ({
  playerName,
  active = true,
  allowRecord = false,
  payableAmount = 0,
  onDebtsChange,
  onRecordDebt,
  leading,
  leadingFill = false,
  chipRowSx = {},
}) => {
  const [debitSummary, setDebitSummary] = useState(null);
  const [showDebitDialog, setShowDebitDialog] = useState(false);
  const [debtInput, setDebtInput] = useState(null);
  const [debtList, setDebtList] = useState([]);
  const [debtNote, setDebtNote] = useState("");
  const [showDebtInput, setShowDebtInput] = useState(false);
  const [recording, setRecording] = useState(false);

  // onRecordDebt → the debt is created immediately by the caller, so there is
  // no payable cap and nothing is buffered locally.
  const immediateRecord = typeof onRecordDebt === "function";

  const fetchDebitSummary = useCallback(() => {
    if (!playerName) return;
    getDebitSummary(playerName)
      .then((res) => {
        const body = res?.data;
        // Accept both the Result-wrapped ({success, data}) and the plain
        // DebitSummaryResponse ({playerName, totalDebts, numberDebit}) shapes.
        const summary = body?.data?.totalDebts ? body.data : body;
        setDebitSummary(summary?.totalDebts ? summary : null);
      })
      .catch(() => setDebitSummary(null));
  }, [playerName]);

  useEffect(() => {
    if (!active || !playerName) {
      setDebitSummary(null);
      return;
    }
    setDebtInput(null);
    setDebtList([]);
    setDebtNote("");
    setShowDebtInput(false);
    fetchDebitSummary();
  }, [active, playerName, fetchDebitSummary]);

  const totalRecordedDebt = debtList.reduce((sum, d) => sum + d, 0);
  const remainingAllowance = immediateRecord
    ? Infinity
    : Math.max(payableAmount - totalRecordedDebt, 0);
  const canOpenDebtInput = immediateRecord || remainingAllowance > 0;

  const debtInputVal =
    debtInput === null
      ? immediateRecord
        ? ""
        : String(remainingAllowance)
      : debtInput;
  const debtNum = /^\d+$/.test(debtInputVal) ? Number(debtInputVal) : 0;
  const canRecordDebt = debtNum > 0 && debtNum <= remainingAllowance;

  // A debt being typed previews until committed or cancelled (deferred mode
  // only — immediate records are not pending deductions)
  const pendingDebt =
    !immediateRecord && showDebtInput && debtInput !== null && canRecordDebt
      ? debtNum
      : 0;
  const totalDebitAmount = totalRecordedDebt + pendingDebt;

  useEffect(() => {
    onDebtsChange?.({
      debtList,
      debtNote,
      totalRecordedDebt,
      pendingDebt,
      totalDebitAmount,
    });
  }, [debtList, debtNote, totalRecordedDebt, pendingDebt, totalDebitAmount, onDebtsChange]);

  const handleDebtInputChange = (e) => {
    const val = e.target.value;
    if (val === "" || /^\d+$/.test(val)) {
      setDebtInput(val);
    }
  };

  const handleRecordDebt = async () => {
    if (!canRecordDebt || recording) return;
    if (immediateRecord) {
      setRecording(true);
      try {
        const ok = await onRecordDebt(debtNum, debtNote);
        if (ok !== false) {
          setDebtInput(null);
          setDebtNote("");
          setShowDebtInput(false);
          fetchDebitSummary();
        }
      } finally {
        setRecording(false);
      }
      return;
    }
    setDebtList([...debtList, debtNum]);
    setDebtInput(null);
    setShowDebtInput(false);
  };

  const handleCloseDebtInput = () => {
    setDebtInput(null);
    setShowDebtInput(false);
  };

  const removeDebtAt = (idx) => {
    setDebtList(debtList.filter((_, i) => i !== idx));
  };

  return (
    <>
      {/* Chips row: record a new debt / open the debt list dialog */}
      <Stack
        direction="row"
        alignItems="center"
        spacing={1}
        sx={{ minHeight: 24, mb: 1.5, flexWrap: "wrap", rowGap: 0.5, ...chipRowSx }}
      >
        {leadingFill ? (
          <Box sx={{ flexBasis: "100%", minWidth: 0 }}>{leading}</Box>
        ) : (
          leading
        )}
        <Box sx={{ flexGrow: 1 }} />
        {allowRecord && canOpenDebtInput && !showDebtInput && (
          <Chip
            label="+ Ghi nợ"
            size="small"
            color="warning"
            onClick={() => setShowDebtInput(true)}
            sx={{ cursor: "pointer", fontWeight: 600 }}
          />
        )}
        {debitSummary && (debitSummary.numberDebit || 0) > 0 ? (
          <Chip
            icon={<WarningAmberIcon fontSize="small" />}
            label={`Nợ: ${formatVND(debitSummary.totalDebts?.amount)} ${debitSummary.totalDebts?.currency || VN_CURRENCY} (${debitSummary.numberDebit})`}
            size="small"
            color="warning"
            onClick={() => setShowDebitDialog(true)}
            sx={{ cursor: "pointer", fontWeight: 600 }}
          />
        ) : (
          <Typography variant="caption" color="text.disabled">
            Không có nợ
          </Typography>
        )}
      </Stack>

      {/* Record-debt input row */}
      {allowRecord && canOpenDebtInput && showDebtInput && (
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1.5 }}>
          <Typography variant="body2" color="text.secondary" sx={{ minWidth: 60 }}>
            Ghi nợ
          </Typography>
          <TextField
            size="small"
            placeholder="0"
            value={debtInputVal}
            onChange={handleDebtInputChange}
            autoFocus
            sx={{ width: 120 }}
          />
          <TextField
            size="small"
            label="Ghi chú"
            placeholder="Ghi chú khoản nợ"
            value={debtNote}
            onChange={(e) => setDebtNote(e.target.value)}
            sx={{ flex: 1, minWidth: 0 }}
          />
          <Button
            variant="contained"
            color="warning"
            size="small"
            onClick={handleRecordDebt}
            disabled={!canRecordDebt || recording}
            sx={{ whiteSpace: "nowrap" }}
            disableElevation
          >
            {recording ? <CircularProgress size={16} color="inherit" /> : "Ghi nợ"}
          </Button>
          <IconButton size="small" onClick={handleCloseDebtInput} sx={{ p: 0.25 }}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Box>
      )}

      {/* Recorded debt lines */}
      {debtList.map((debt, i) => (
        <Box
          key={i}
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            px: 1.5,
            py: 1,
            mb: 0.5,
            bgcolor: "warning.light",
            borderRadius: 1,
          }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
            <WarningAmberIcon fontSize="small" color="warning" />
            <Typography variant="body2" color="warning.dark" fontWeight={600}>
              Ghi nợ
            </Typography>
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
            <Typography variant="body2" color="warning.dark" fontWeight={700}>
              {formatVND(debt)} {VN_CURRENCY}
            </Typography>
            <IconButton
              size="small"
              onClick={() => removeDebtAt(i)}
              sx={{ p: 0.25, color: "warning.dark" }}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>
      ))}
      {pendingDebt > 0 && (
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            px: 1.5,
            py: 1,
            mb: 0.5,
            bgcolor: "warning.light",
            borderRadius: 1,
          }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
            <WarningAmberIcon fontSize="small" color="warning" />
            <Typography variant="body2" color="warning.dark" fontWeight={600}>
              Ghi nợ
            </Typography>
          </Box>
          <Typography variant="body2" color="warning.dark" fontWeight={700}>
            {formatVND(pendingDebt)} {VN_CURRENCY}
          </Typography>
        </Box>
      )}

      <DebitListDialog
        show={showDebitDialog}
        playerName={playerName}
        onClose={() => setShowDebitDialog(false)}
        onPaid={fetchDebitSummary}
      />
    </>
  );
};

export default PlayerDebtSection;

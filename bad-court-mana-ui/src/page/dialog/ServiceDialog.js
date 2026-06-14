import React, { useCallback, useEffect, useState, useMemo } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemText from "@mui/material/ListItemText";
import Chip from "@mui/material/Chip";
import { TYPE } from "../HomePage";
import { VN_CURRENCY, formatVND } from "./../MoneyUtils";

const ADVANCE_SERVICE_NAME = "Tr\u1ea3 tr\u01b0\u1edbc";

const ServiceDialog = ({
  playerName,
  services = [],
  onClose,
  onPay,
  onDelete,
  onUpdateServices,
  onUpdatePlayerName,
  canEditPlayerName = false,
  hideActions = false,
  serviceOptions = [],
}) => {
  const [totalCost, setTotalCost] = useState(0);
  const [serviceName, setServiceName] = useState("");
  const [serviceCost, setServiceCost] = useState("");
  const [isEditingName, setIsEditingName] = useState(false);
  const [editPlayerName, setEditPlayerName] = useState(playerName || "");
  const [editError, setEditError] = useState("");

  // Filter available service options by current input
  const filteredServiceOptions = useMemo(() => {
    const q = serviceName.trim().toLowerCase();
    if (!q) return [];
    return serviceOptions.filter((s) =>
      s.serviceName.toLowerCase().includes(q)
    );
  }, [serviceName, serviceOptions]);

  const recalcTotal = useCallback((serviceList) => {
    return serviceList.reduce((sum, item) => {
      const amount = item.cost || 0;
      return sum + amount;
    }, 0);
  }, []);

  const handleAddService = useCallback(() => {
    if (!serviceName || !serviceCost) return;

    const newService = {
      serviceName: serviceName,
      cost: Number(serviceCost),
      costFormat: formatVND(serviceCost),
    };
    const updated = [...services, newService];

    onUpdateServices(playerName, updated);
    setServiceName("");
    setServiceCost("");
  }, [serviceName, serviceCost, services, playerName, onUpdateServices]);

  const handleSelectOption = useCallback((option) => {
    setServiceName(option.serviceName);
    setServiceCost(option.cost);
  }, []);

  const onPreDelete = () => {
    onClose(false);
    onDelete(playerName, TYPE.CANCEL, services, Number(totalCost));
  };

  const onPrePay = () => {
    onPay(playerName, TYPE.PAY, services, Number(totalCost));
  };

  const handleRemoveService = (index) => {
    const updated = services.filter((_, i) => i !== index);
    onUpdateServices(playerName, updated);
  };

  const displayServiceName = (name) => {
    if (name === "rentByTime") return "Thuê theo giờ";
    return name;
  };

  const handleCancelEditName = useCallback(() => {
    setIsEditingName(false);
    setEditPlayerName(playerName || "");
    setEditError("");
  }, [playerName]);

  const handleSaveEditName = useCallback(async () => {
    const trimmedName = editPlayerName.trim();
    if (!trimmedName) {
      setEditError("Tên người chơi không được để trống");
      return;
    }

    if (trimmedName === playerName) {
      handleCancelEditName();
      return;
    }

    try {
      const updated = await onUpdatePlayerName?.(playerName, trimmedName);
      if (updated) {
        setIsEditingName(false);
        setEditError("");
      } else {
        setEditError("Không thể đổi tên người chơi");
      }
    } catch {
      setEditError("Không thể đổi tên người chơi");
    }
  }, [editPlayerName, handleCancelEditName, onUpdatePlayerName, playerName]);

  const handleKeyDown = useCallback(
    (event) => {
      if (event.key === "Enter") {
        event.preventDefault();
        if (isEditingName) {
          handleSaveEditName();
        } else {
          handleAddService();
        }
      }

      if (event.key === "Escape") {
        if (isEditingName) {
          handleCancelEditName();
        } else {
          onClose(false);
        }
      }
    },
    [handleAddService, handleCancelEditName, handleSaveEditName, isEditingName, onClose]
  );

  useEffect(() => {
    setTotalCost(recalcTotal(services));
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [services, handleKeyDown, recalcTotal]);

  useEffect(() => {
    setEditPlayerName(playerName || "");
    setEditError("");
    setIsEditingName(false);
  }, [playerName]);

  return (
    <Dialog
      open
      fullWidth
      maxWidth="sm"
      onClose={(event, reason) => {
        if (reason === "backdropClick") return;
        onClose(false);
      }}
    >
      <DialogTitle sx={{ pr: 6 }}>
        <IconButton
          aria-label="close"
          onClick={() => onClose(false)}
          sx={{ position: "absolute", right: 8, top: 8 }}
        >
          <CloseIcon />
        </IconButton>
        <Typography variant="h6" align="center" component="span" display="block">
          Bảng chi phí của:
        </Typography>
        <Typography variant="subtitle1" align="center" fontWeight={600}>
          {playerName}
        </Typography>
        {canEditPlayerName && !hideActions && (
          <Box sx={{ mt: 1, display: "flex", justifyContent: "center" }}>
            {isEditingName ? (
              <Box sx={{ width: "100%", maxWidth: 360 }}>
                <Stack direction="row" spacing={1} alignItems="center" justifyContent="center">
                  <TextField
                    autoFocus
                    size="small"
                    label="Tên người chơi"
                    value={editPlayerName}
                    error={Boolean(editError)}
                    onChange={(e) => {
                      setEditPlayerName(e.target.value);
                      setEditError("");
                    }}
                    onKeyDown={(e) => {
                      e.stopPropagation();
                      if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                        e.preventDefault();
                        handleSaveEditName();
                      }
                      if (e.key === "Escape") {
                        e.preventDefault();
                        handleCancelEditName();
                      }
                    }}
                    sx={{ flex: 1 }}
                  />
                  <Button size="small" variant="contained" onClick={handleSaveEditName}>
                    Lưu
                  </Button>
                  <Button size="small" variant="outlined" onClick={handleCancelEditName}>
                    Huỷ
                  </Button>
                </Stack>
                {editError && (
                  <Typography variant="caption" color="error" sx={{ display: "block", mt: 0.5, textAlign: "center" }}>
                    {editError}
                  </Typography>
                )}
              </Box>
            ) : (
              <Button size="small" variant="text" onClick={() => setIsEditingName(true)}>
                Sửa tên
              </Button>
            )}
          </Box>
        )}
      </DialogTitle>
      <DialogContent dividers>
        <Typography variant="subtitle2" color="primary" gutterBottom>
          Tổng cộng: {formatVND(totalCost)} {VN_CURRENCY}
        </Typography>

        <Box sx={{ position: "relative", mb: 2 }}>
          <Stack direction="row" spacing={1}>
            <TextField
              size="small"
              label="Tên dịch vụ"
              placeholder="Tên dịch vụ"
              value={serviceName}
              onChange={(e) => setServiceName(e.target.value)}
              sx={{ flex: 2 }}
            />
            <TextField
              size="small"
              label="Giá"
              placeholder="Giá"
              value={serviceCost}
              onChange={(e) => setServiceCost(e.target.value)}
              sx={{ flex: 1 }}
            />
            <Button variant="contained" color="success" onClick={handleAddService} sx={{ flexShrink: 0 }}>
              +
            </Button>
          </Stack>

          {/* Dropdown of matching service options */}
          {filteredServiceOptions.length > 0 && (
            <Paper
              sx={{
                position: "absolute",
                top: "100%",
                left: 0,
                right: 0,
                zIndex: 20,
                maxHeight: 180,
                overflow: "auto",
                mt: 0.25,
              }}
            >
              {filteredServiceOptions.map((opt, idx) => (
                <Box
                  key={idx}
                  onClick={() => handleSelectOption(opt)}
                  sx={{
                    px: 1.5,
                    py: 0.75,
                    cursor: "pointer",
                    fontSize: "0.85rem",
                    display: "flex",
                    justifyContent: "space-between",
                    "&:hover": { bgcolor: "action.hover" },
                  }}
                >
                  <span>{opt.serviceName}</span>
                  <span style={{ color: "text.secondary" }}>
                    {opt.costFormat} {opt.currency}
                  </span>
                </Box>
              ))}
            </Paper>
          )}
        </Box>

        {services.length > 0 ? (
          <List dense disablePadding>
            {services.map((service, idx) => {
              const isAdvance = service.serviceName === ADVANCE_SERVICE_NAME;
              return (
                <ListItem
                  key={idx}
                  secondaryAction={
                    isAdvance ? (
                      <Chip label="Đã trả" size="small" color="info" variant="outlined" sx={{ fontSize: "0.7rem", height: 22 }} />
                    ) : (
                      <Button size="small" color="error" variant="outlined" onClick={() => handleRemoveService(idx)}>
                        ✕
                      </Button>
                    )
                  }
                  sx={{
                    borderBottom: 1,
                    borderColor: "divider",
                    py: 1,
                    bgcolor: isAdvance ? "info.light" : "transparent",
                  }}
                >
                  <ListItemText
                    primary={
                      isAdvance ? (
                        <Typography variant="body2" color="info.dark" fontWeight={600}>
                          {displayServiceName(service.serviceName)}
                        </Typography>
                      ) : (
                        displayServiceName(service.serviceName)
                      )
                    }
                    secondary={
                      isAdvance
                        ? `−${formatVND(Math.abs(service.cost))} ${VN_CURRENCY}`
                        : `${service.costFormat} ${VN_CURRENCY}`
                    }
                    slotProps={{
                      secondary: {
                        sx: isAdvance
                          ? { color: "info.dark", fontWeight: 600 }
                          : undefined,
                      },
                    }}
                  />
                </ListItem>
              );
            })}
          </List>
        ) : (
          <Typography variant="body2" color="text.secondary">
            Không có dịch vụ nào.
          </Typography>
        )}
      </DialogContent>
      {!hideActions && (
        <DialogActions sx={{ px: 3, py: 2, gap: 1, flexWrap: "wrap" }}>
          <Button variant="contained" onClick={onPrePay}>
            Thanh toán
          </Button>
          <Button variant="outlined" color="error" onClick={onPreDelete}>
            Xoá + không thanh toán
          </Button>
        </DialogActions>
      )}
    </Dialog>
  );
};

export default ServiceDialog;

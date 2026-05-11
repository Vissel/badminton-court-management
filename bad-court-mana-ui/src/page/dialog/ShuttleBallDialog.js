import { useEffect, useState, useCallback } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import Box from "@mui/material/Box";
import api from "../../api/index";

const ShuttleBallDialog = ({
  courtProcessing,
  show,
  onSaveBallOntoCourt,
  onCancel,
}) => {
  const [options, setOptions] = useState([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [quantity, setQuantity] = useState(0);
  const [addedItems, setAddedItems] = useState([]);

  const handleEscPress = useCallback(
    (event) => {
      if (event.key === "Escape") {
        onCancel();
      }
    },
    [onCancel]
  );

  useEffect(() => {
    if (!show) return undefined;

    setAddedItems([]);
    api
      .get("/court-mana/getShuttleBalls")
      .then((res) => res.data)
      .then((data) => {
        setOptions(
          data.map((b) => ({
            shuttleName: b.shuttleName,
            cost: b.cost,
            costFormat: b.costFormat,
            currency: b.currency,
          }))
        );
        setSelectedIndex(0);
        setQuantity(1);
      })
      .catch((err) => console.error("Error fetching shuttle balls:", err));

    document.addEventListener("keydown", handleEscPress);
    return () => {
      document.removeEventListener("keydown", handleEscPress);
    };
  }, [show, handleEscPress]);

  const handleDelete = (index) => {
    setAddedItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleAdd = () => {
    if (Number.isNaN(quantity) || quantity < 1) {
      return;
    }
    const intQuantity = parseInt(quantity, 10);
    const selectedValue = options[selectedIndex];

    setAddedItems((prev) => {
      const existing = prev.find(
        (it) => it.shuttleName === selectedValue.shuttleName
      );

      if (existing) {
        return prev.map((it) =>
          it.shuttleName === selectedValue.shuttleName
            ? { ...it, quantity: it.quantity + intQuantity }
            : it
        );
      }

      return [
        ...prev,
        {
          shuttleName: selectedValue.shuttleName,
          cost: selectedValue.cost,
          costFormat: selectedValue.costFormat,
          currency: selectedValue.currency,
          quantity: intQuantity,
        },
      ];
    });
    setQuantity(1);
  };

  const handleSave = () => {
    onSaveBallOntoCourt(courtProcessing, addedItems);
  };

  const checkAndSetQuantity = (newQuantity) => {
    const n = typeof newQuantity === "string" ? parseFloat(newQuantity) : newQuantity;
    if (!Number.isNaN(n)) {
      setQuantity(n);
    }
  };

  if (!show) return null;

  return (
    <Dialog open={show} onClose={onCancel} maxWidth="sm" fullWidth scroll="paper">
      <DialogTitle>Thêm cầu</DialogTitle>
      <DialogContent dividers>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={1} sx={{ mb: 2 }}>
          <TextField
            select
            label="Loại cầu"
            size="small"
            value={selectedIndex}
            onChange={(e) => setSelectedIndex(Number(e.target.value))}
            sx={{ flex: 1 }}
          >
            {options.map((ball, index) => (
              <MenuItem key={ball.shuttleName} value={index}>
                {ball.shuttleName} - {ball.costFormat} {ball.currency}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            type="number"
            label="Số lượng"
            size="small"
            value={quantity}
            onChange={(e) => checkAndSetQuantity(e.target.value)}
            sx={{ width: { xs: "100%", sm: 120 } }}
          />
          <Button variant="contained" color="success" onClick={handleAdd} sx={{ alignSelf: { sm: "center" } }}>
            +
          </Button>
        </Stack>

        <Box
          sx={{
            maxHeight: 320,
            overflowY: "auto",
            pr: 1,
          }}
        >
          {addedItems.length === 0 && (
            <Typography variant="body2" color="text.secondary" align="center">
              Chưa có cầu nào được thêm
            </Typography>
          )}

          {addedItems.map((item, index) => (
            <Stack
              key={index}
              direction="row"
              alignItems="center"
              justifyContent="space-between"
              sx={{
                py: 1.25,
                px: 1.5,
                mb: 1,
                borderRadius: 1,
                border: 1,
                borderColor: "divider",
                bgcolor: "grey.50",
              }}
            >
              <Box>
                <Typography component="span" fontWeight={600}>
                  {item.shuttleName}
                </Typography>
                {" – "}
                <Typography component="span">{item.costFormat}</Typography>
                {" – "}
                <Typography component="span" color="text.secondary">
                  Số lượng: {item.quantity}
                </Typography>
              </Box>
              <Button size="small" color="error" variant="outlined" onClick={() => handleDelete(index)}>
                X
              </Button>
            </Stack>
          ))}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
        <Button variant="contained" color="success" onClick={handleSave}>
          Lưu
        </Button>
        <Button variant="outlined" onClick={onCancel}>
          Hủy
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ShuttleBallDialog;

import React from "react";
import { useDrag } from "react-dnd";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import Tooltip from "@mui/material/Tooltip";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { ItemTypes } from "../ItemTypes";

const LOW_STOCK_LIMIT = 5;

const DraggableService = ({ serviceName, cost, costFormat, currency, itemId, stockOnHand }) => {
  const isStockable = itemId != null;
  const outOfStock = isStockable && (stockOnHand ?? 0) <= 0;
  const lowStock = isStockable && !outOfStock && stockOnHand <= LOW_STOCK_LIMIT;
  // Remaining count shown on hover. `stockOnHand` is refreshed by /checkStock
  // after each successful add, so the tooltip reflects the latest stock.
  const stockTip = !isStockable
    ? ""
    : outOfStock
      ? "Hết hàng"
      : `Còn ${stockOnHand ?? "…"}`;
  const [{ isDragging }, drag] = useDrag(
    () => ({
      type: ItemTypes.SERVICE,
      item: { serviceName, cost, costFormat, itemId, quantity: 1 },
      canDrag: !outOfStock,
      collect: (monitor) => ({
        isDragging: !!monitor.isDragging(),
      }),
    }),
    [serviceName, cost, costFormat, itemId, stockOnHand, outOfStock]
  );

  return (
    <Tooltip
      title={stockTip}
      placement="right"
      arrow
    >
      <Paper
        ref={drag}
        elevation={isDragging ? 0 : 1}
        sx={{
          opacity: isDragging || outOfStock ? 0.5 : 1,
          px: 1.25,
          py: 1,
          my: 0.75,
          mx: 0.5,
          cursor: outOfStock ? "not-allowed" : "grab",
          border: 1,
          borderColor: "divider",
          bgcolor: outOfStock ? "action.disabledBackground" : "grey.50",
        }}
      >
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 1 }}>
          <Typography variant="body2">
            {serviceName} - {costFormat} {currency}
          </Typography>
          {lowStock && (
            <Tooltip title="Sắp hết hàng" arrow>
              <WarningAmberIcon color="warning" fontSize="small" />
            </Tooltip>
          )}
        </Box>
      </Paper>
    </Tooltip>
  );
};

export default DraggableService;

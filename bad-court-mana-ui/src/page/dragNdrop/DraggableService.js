import React from "react";
import { useDrag } from "react-dnd";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { ItemTypes } from "../ItemTypes";

const DraggableService = ({ serviceName, cost, costFormat, currency }) => {
  const [{ isDragging }, drag] = useDrag(
    () => ({
      type: ItemTypes.SERVICE,
      item: { serviceName, cost, costFormat },
      collect: (monitor) => ({
        isDragging: !!monitor.isDragging(),
      }),
    }),
    [serviceName, cost, costFormat]
  );

  return (
    <Paper
      ref={drag}
      elevation={isDragging ? 0 : 1}
      sx={{
        opacity: isDragging ? 0.5 : 1,
        px: 1.25,
        py: 1,
        my: 0.75,
        mx: 0.5,
        cursor: "grab",
        border: 1,
        borderColor: "divider",
        bgcolor: "grey.50",
      }}
    >
      <Typography variant="body2">
        {serviceName} - {costFormat} {currency}
      </Typography>
    </Paper>
  );
};

export default DraggableService;

import React from "react";
import { useDrag } from "react-dnd";
import { ItemTypes } from "../ItemTypes";

const DraggableService = ({ serviceName, cost }) => {
  const [{ isDragging }, drag] = useDrag(
    () => ({
      type: ItemTypes.SERVICE,
      item: { serviceName, cost },
      collect: (monitor) => ({
        isDragging: !!monitor.isDragging(),
      }),
    }),
    [serviceName, cost]
  );

  return (
    <div
      ref={drag}
      style={{
        opacity: isDragging ? 0.5 : 1,
        padding: "5px 10px",
        margin: "5px",
        backgroundColor: "#cde",
        borderRadius: "4px",
        cursor: "move",
        border: "1px solid #aac",
      }}
    >
      {serviceName} - {cost}
    </div>
  );
};

export default DraggableService;

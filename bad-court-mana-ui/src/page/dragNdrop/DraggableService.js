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
    <div className="selection-box"
      ref={drag}
      style={{
        opacity: isDragging ? 0.5 : 1
      }}
    >
      {serviceName} - {cost}
    </div>
  );
};

export default DraggableService;

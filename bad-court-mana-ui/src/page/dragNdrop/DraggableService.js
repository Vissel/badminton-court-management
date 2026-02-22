import React from "react";
import { useDrag } from "react-dnd";
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
    <div className="selection-box"
      ref={drag}
      style={{
        opacity: isDragging ? 0.5 : 1
      }}
    >
      {serviceName} - {costFormat} {currency}
    </div>
  );
};

export default DraggableService;

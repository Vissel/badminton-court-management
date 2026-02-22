import { useState } from "react";
import { useDrag, useDrop } from "react-dnd";
import "./style.css";
import { ItemTypes } from "../ItemTypes";

function DraggablePlayer({ name, isLocked, onDropService, onClick, courtId, areaKey }) {
  const [{ isDragging }, drag] = useDrag(
    () => ({
      type: ItemTypes.PLAYER,
      item: { name, courtId, areaKey },
      canDrag: !isLocked,
      collect: (monitor) => ({ isDragging: !!monitor.isDragging() }),
    }),
    [isLocked,courtId, areaKey]
  );
  const [, drop] = useDrop(
    () => ({
      accept: ItemTypes.SERVICE,
      drop: (item) => {
        onDropService?.(name, item.serviceName, item.cost, item.costFormat);
        // trigger highlight animation
        setAnimate(true);
        setTimeout(() => setAnimate(false), 600);
      },
    }),
    [name]
  );

  // animation after dropping
  const [animate, setAnimate] = useState(false);
  return (
    <div
      ref={(node) => drag(drop(node))}
      onClick={() => onClick?.(name)}
      className={`player-box ${animate ? "player-highlight" : ""}`}
      style={{
        opacity: isDragging ? 0.5 : 1,
        padding: "5px",
        margin: "5px",
        backgroundColor: "white",
        border: "1px solid gray",
        textAlign: "center",
        cursor: isLocked ? "not-allowed" : "move",
        transition: "all 2s ease",
      }}
      title={`Currently placed in: ${name}`}
    >
      {name}
    </div>
  );
}
export default DraggablePlayer;

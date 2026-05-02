import { useState, useEffect } from "react";
import { ItemTypes } from "../ItemTypes";
import { useDrop } from "react-dnd";
import DraggablePlayer from "./DraggablePlayer";

export default function DropZone({
  courtId,
  areaKey,
  player,
  onDropPlayer,
  occupied,
  isLocked,
  onDropService,
}) {
  const [hasDropped, setHasDropped] = useState(!!player);

  const [{ isOver }, drop] = useDrop(
    () => ({
      accept: ItemTypes.PLAYER,
      drop: (item) => {
        if (!isLocked) {
          onDropPlayer(item.name, courtId, areaKey, item.courtId, item.areaKey);
          setHasDropped(true);
        }
      },
      collect: (monitor) => ({ isOver: !!monitor.isOver() }),
      canDrop: () => !isLocked,
    }),
    [player, isLocked]
  );

  useEffect(() => {
    if (!player) setHasDropped(false);
  }, [player]);

  const getBackgroundColor = () => {
    if (hasDropped) return "white";
    if (isOver) return "#def";
    return "rgb(255 255 255 / 39%)";
  };

  return (
    <div
      ref={drop}
      style={{
        // height: "50px",
        border: "1px dashed gray",
        margin: "5px",
        backgroundColor: getBackgroundColor(),
        textAlign: "center",
        lineHeight: "50px",
        transition: "background-color 2s ease",
      }}
      title={
        player
          ? `Player: ${player}`
          : `Drop here - Court ${courtId}, Area ${areaKey}`
      }
    >
      {player && (
        <DraggablePlayer
          name={player}
          isLocked={isLocked}
          onDropService={onDropService}
          courtId={courtId}
          areaKey={areaKey}
        />
      )}
    </div>
  );
}
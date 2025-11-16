import { useDrop } from "react-dnd";
import { ItemTypes } from "../ItemTypes";
import DraggablePlayer from "./DraggablePlayer";

export default function PlayerArea({
  availablePlayers,
  onDropPlayerBack,
  onAddPlayer,
  newPlayer,
  setNewPlayer,
  onDropService,
  onClickPlayer,
}) {
  const [{ isOver }, drop] = useDrop(
    () =>
      ({
        accept: ItemTypes.PLAYER,
        // if the player is already in availablePlayers, don't process the drop
        canDrop: (item) => !availablePlayers.includes(item.name),
        drop: (item) => onDropPlayerBack(item.name, item.courtId, item.areaKey),
        collect: (monitor) => ({ isOver: !!monitor.isOver() }),
      }),
      
      [availablePlayers, onDropPlayerBack] // ensure fresh closures
  );

  const handleAdd = () => {
    if (
      newPlayer.trim() !== "" &&
      !availablePlayers.includes(newPlayer.trim())
    ) {
      onAddPlayer(newPlayer.trim());
      setNewPlayer("");
    }
  };

  return (
    <div
      ref={drop}
      style={{
        width: "100%",
        padding: "2px",
        backgroundColor: isOver ? "#eef" : "#f8f9fa",
        transition: "background-color 2s ease",
      }}
    >
      <div class="player-area-header">
        <h5> Tổng người chơi: {availablePlayers.length} </h5>
      </div>
      <input
        type="text"
        placeholder="Tên người chơi"
        value={newPlayer}
        onChange={(e) => setNewPlayer(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && handleAdd()}
        style={{
          width: "100%",
          padding: "5px",
          marginBottom: "15px",
          boxSizing: "border-box",
        }}
      />
      {availablePlayers.map((p) => (
        <DraggablePlayer
          key={p}
          name={p}
          isLocked={false}
          onDropService={onDropService}
          onClick={onClickPlayer}
        />
      ))}
    </div>
  );
}

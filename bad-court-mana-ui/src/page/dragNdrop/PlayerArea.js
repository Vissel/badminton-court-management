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
      className="player-area-panel"
      style={{
        backgroundColor: isOver ? "#eef" : "#f8f9fa",
        transition: "background-color 2s ease",
      }}
    >
      <div className="player-area-header">
        <h5> Tổng người chơi: {availablePlayers.length} </h5>
      </div>
      <input
        type="text"
        className="player-area-input"
        placeholder="Tên người chơi"
        value={newPlayer}
        onChange={(e) => setNewPlayer(e.target.value)}
        onKeyDown={(e) => {
          // Avoid submitting while an IME composition is still in progress.
          if (e.key === "Enter" && !e.nativeEvent.isComposing) {
            handleAdd();
          }
        }}
      />
      <div className="player-area-list">
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
    </div>
  );
}

import { useDrop } from "react-dnd";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import Box from "@mui/material/Box";
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
    () => ({
      accept: ItemTypes.PLAYER,
      canDrop: (item) => !availablePlayers.includes(item.name),
      drop: (item) => onDropPlayerBack(item.name, item.courtId, item.areaKey),
      collect: (monitor) => ({ isOver: !!monitor.isOver() }),
    }),
    [availablePlayers, onDropPlayerBack]
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
    <Box
      ref={drop}
      className="player-area-panel"
      sx={{
        bgcolor: isOver ? "action.hover" : "grey.100",
        transition: "background-color 0.3s ease",
      }}
    >
      <Box className="player-area-header">
        <Typography variant="subtitle1" fontWeight={600}>
          Tổng người chơi: {availablePlayers.length}
        </Typography>
      </Box>
      <TextField
        className="player-area-input"
        size="small"
        fullWidth
        placeholder="Tên người chơi"
        value={newPlayer}
        onChange={(e) => setNewPlayer(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && !e.nativeEvent.isComposing) {
            handleAdd();
          }
        }}
        sx={{ mb: 2, mt: 0.5 }}
      />
      <Box className="player-area-list">
        {availablePlayers.map((p) => (
          <DraggablePlayer
            key={p}
            name={p}
            isLocked={false}
            onDropService={onDropService}
            onClick={onClickPlayer}
          />
        ))}
      </Box>
    </Box>
  );
}

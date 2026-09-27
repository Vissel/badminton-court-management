import { useState, useCallback, useMemo, useRef, useEffect } from "react";
import { useDrop } from "react-dnd";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import Box from "@mui/material/Box";
import InputAdornment from "@mui/material/InputAdornment";
import IconButton from "@mui/material/IconButton";
import SearchIcon from "@mui/icons-material/Search";
import CloseIcon from "@mui/icons-material/Close";
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
  const [duplicateWarning, setDuplicateWarning] = useState(false);
  const [isSearchMode, setIsSearchMode] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const searchInputRef = useRef(null);

  const filteredPlayers = useMemo(() => {
    if (!isSearchMode) return availablePlayers;
    const q = searchQuery.trim().toLowerCase();
    if (!q) return availablePlayers;
    return availablePlayers.filter((p) =>
      p.toLowerCase().includes(q)
    );
  }, [availablePlayers, isSearchMode, searchQuery]);

  const checkDuplicate = useCallback(
    (value) => {
      const trimmed = value.trim();
      setDuplicateWarning(trimmed !== "" && availablePlayers.includes(trimmed));
    },
    [availablePlayers]
  );

  const handleToggleSearch = useCallback(() => {
    if (isSearchMode) {
      // exit search → back to add mode
      setIsSearchMode(false);
      setSearchQuery("");
    } else {
      // enter search → clear add input
      setIsSearchMode(true);
      setNewPlayer("");
      setDuplicateWarning(false);
    }
  }, [isSearchMode, setNewPlayer]);

  useEffect(() => {
    if (isSearchMode && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isSearchMode]);

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
    const trimmed = newPlayer.trim();
    if (trimmed === "") return;

    if (availablePlayers.includes(trimmed)) {
      setDuplicateWarning(true);
      return;
    }

    onAddPlayer(trimmed);
    setNewPlayer("");
    setDuplicateWarning(false);
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
          {isSearchMode
            ? `Kết quả: ${filteredPlayers.length}/${availablePlayers.length}`
            : `Tổng người chơi: ${availablePlayers.length}`}
        </Typography>
      </Box>
      <TextField
        className="player-area-input"
        size="small"
        fullWidth
        placeholder={isSearchMode ? "Tìm người chơi..." : "Tên người chơi"}
        value={isSearchMode ? searchQuery : newPlayer}
        error={!isSearchMode && duplicateWarning}
        onChange={(e) => {
          if (isSearchMode) {
            setSearchQuery(e.target.value);
          } else {
            setNewPlayer(e.target.value);
            checkDuplicate(e.target.value);
          }
        }}
        onKeyDown={(e) => {
          if (e.key === "Enter" && !e.nativeEvent.isComposing) {
            if (!isSearchMode) handleAdd();
          }
        }}
        slotProps={{
          htmlInput: { ref: searchInputRef },
          input: {
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  size="small"
                  onClick={handleToggleSearch}
                  tabIndex={-1}
                >
                  {isSearchMode ? (
                    <CloseIcon fontSize="small" />
                  ) : (
                    <SearchIcon fontSize="small" />
                  )}
                </IconButton>
              </InputAdornment>
            ),
          },
        }}
        sx={{ mb: 2, mt: 0.5 }}
      />
      {!isSearchMode && duplicateWarning && (
        <Typography
          variant="caption"
          color="error"
          sx={{
            display: "block",
            mb: 1,
            mt: -1,
            ml: 0.5,
            fontWeight: 500,
          }}
        >
          Người chơi đã tồn tại
        </Typography>
      )}
      <Box className="player-area-list">
        {(isSearchMode ? filteredPlayers : availablePlayers).map((p) => (
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

import { useState, useEffect, useRef, useMemo, useCallback } from "react";
import { useDrop } from "react-dnd";
import TextField from "@mui/material/TextField";
import Paper from "@mui/material/Paper";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import AddIcon from "@mui/icons-material/Add";
import ClickAwayListener from "@mui/material/ClickAwayListener";
import { ItemTypes } from "../ItemTypes";
import DraggablePlayer from "./DraggablePlayer";

export default function DropZone({
  courtId,
  areaKey,
  player,
  onDropPlayer,
  occupied,
  isLocked,
  onDropService,
  availablePlayers,
  onClickPlayer,
}) {
  const [hasDropped, setHasDropped] = useState(!!player);
  const [showSearch, setShowSearch] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedPlayer, setSelectedPlayer] = useState("");
  const searchInputRef = useRef(null);

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
    [player, isLocked, courtId, areaKey, onDropPlayer]
  );

  useEffect(() => {
    if (!player) setHasDropped(false);
  }, [player]);

  // Close search when a player is placed externally (e.g. by DnD)
  useEffect(() => {
    if (player) {
      setShowSearch(false);
      setSearchQuery("");
      setSelectedPlayer("");
    }
  }, [player]);

  // Auto-focus search input when search mode opens
  useEffect(() => {
    if (showSearch && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [showSearch]);

  const filteredPlayers = useMemo(() => {
    if (!searchQuery.trim()) return availablePlayers || [];
    const q = searchQuery.trim().toLowerCase();
    return (availablePlayers || []).filter((p) =>
      p.toLowerCase().includes(q)
    );
  }, [availablePlayers, searchQuery]);

  const handleZoneClick = useCallback(() => {
    if (!player && !isLocked && !showSearch) {
      setShowSearch(true);
    }
  }, [player, isLocked, showSearch]);

  const handleAddPlayer = useCallback(
    (playerName) => {
      if (playerName && !isLocked) {
        onDropPlayer(playerName, courtId, areaKey, null, null);
        setShowSearch(false);
        setSearchQuery("");
        setSelectedPlayer("");
        setHasDropped(true);
      }
    },
    [isLocked, courtId, areaKey, onDropPlayer]
  );

  const handleCloseSearch = useCallback(() => {
    setShowSearch(false);
    setSearchQuery("");
    setSelectedPlayer("");
  }, []);

  const handleSelectPlayer = useCallback((name) => {
    setSelectedPlayer(name);
    setSearchQuery(name);
  }, []);

  const getBackgroundColor = () => {
    if (hasDropped) return "white";
    if (isOver) return "#def";
    return "rgb(255 255 255 / 39%)";
  };

  return (
    <ClickAwayListener onClickAway={handleCloseSearch}>
      <div
        ref={(node) => {
          drop(node);
        }}
        onClick={handleZoneClick}
        style={{
          border: "1px dashed gray",
          margin: "5px",
          backgroundColor: getBackgroundColor(),
          textAlign: "center",
          transition: "background-color 2s ease",
          position: "relative",
          cursor: !player && !isLocked && !showSearch ? "pointer" : "default",
          minHeight: showSearch ? "auto" : "50px",
        }}
        title={
          player ? `Player: ${player}` : `Court ${courtId}, Area ${areaKey}`
        }
      >
        {/* Player already placed – show draggable player */}
        {player && (
          <DraggablePlayer
            name={player}
            isLocked={isLocked}
            onDropService={onDropService}
            courtId={courtId}
            areaKey={areaKey}
            onClick={onClickPlayer}
          />
        )}

        {/* Empty / locked */}
        {/* {!player && isLocked && (
          <span style={{ fontSize: "0.75rem", color: "#ccc" }}>
            Occupied
          </span>
        )} */}

        {/* Search mode – TextField + dropdown */}
        {showSearch && !player && (
          <Box
            onClick={(e) => e.stopPropagation()}
            sx={{ p: 0.5 }}
          >
            <TextField
              size="small"
              fullWidth
              placeholder="Tìm người chơi..."
              value={selectedPlayer || searchQuery}
              onChange={(e) => {
                const val = e.target.value;
                setSearchQuery(val);
                if (selectedPlayer && val !== selectedPlayer) {
                  setSelectedPlayer("");
                }
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                  e.preventDefault();
                  if (selectedPlayer) {
                    handleAddPlayer(selectedPlayer);
                  } else if (filteredPlayers.length === 1) {
                    handleAddPlayer(filteredPlayers[0]);
                  }
                }
              }}
              slotProps={{
                htmlInput: { ref: searchInputRef },
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          if (selectedPlayer) {
                            handleAddPlayer(selectedPlayer);
                          } else if (filteredPlayers.length === 1) {
                            handleAddPlayer(filteredPlayers[0]);
                          }
                        }}
                        disabled={!selectedPlayer && filteredPlayers.length !== 1}
                        tabIndex={-1}
                      >
                        <AddIcon fontSize="small" color="success" />
                      </IconButton>
                    </InputAdornment>
                  ),
                  sx: { fontSize: "0.8rem", py: 0.25 },
                },
              }}
              sx={{
                "& .MuiOutlinedInput-root": {
                  bgcolor: "white",
                },
              }}
            />

            {/* Dropdown with matching players */}
            {!selectedPlayer && searchQuery.trim() && filteredPlayers.length > 0 && (
              <Paper
                sx={{
                  position: "absolute",
                  top: "100%",
                  left: 0,
                  right: 0,
                  zIndex: 20,
                  maxHeight: 150,
                  overflow: "auto",
                  mt: 0.25,
                }}
              >
                {filteredPlayers.map((p) => (
                  <Box
                    key={p}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleSelectPlayer(p);
                    }}
                    sx={{
                      px: 1,
                      py: 0.5,
                      cursor: "pointer",
                      fontSize: "0.8rem",
                      "&:hover": { bgcolor: "action.hover" },
                      textAlign: "left",
                    }}
                  >
                    {p}
                  </Box>
                ))}
              </Paper>
            )}
          </Box>
        )}

        {/* Empty zone – click hint */}
        {/* {!player && !showSearch && !isLocked && (
          <span style={{ fontSize: "0.75rem", color: "#888" }}>
            + Click để thêm
          </span>
        )} */}
      </div>
    </ClickAwayListener>
  );
}

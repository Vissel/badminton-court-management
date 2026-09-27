import React, { useEffect, useRef, useState } from "react";
import { DragDropContext, Droppable, Draggable } from "react-beautiful-dnd";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Paper from "@mui/material/Paper";
import "../App.css";
import "../index.css";

let playerId = 4;
const COURT_COUNT = 8;
const AREA_LABELS = ["A", "B", "C", "D"];

const HomePageError = () => {
  const [playerInput, setPlayerInput] = useState("");
  const [players, setPlayers] = useState([]);
  const initialPlayers = [
    { id: "player-1", label: "Player A" },
    { id: "player-2", label: "Player B" },
    { id: "player-3", label: "Player C" },
  ];
  const [courtAssignments, setCourtAssignments] = useState({});

  const handleAddPlayer = (e) => {
    if (e.key === "Enter" && playerInput.trim()) {
      const newPlayer = {
        id: `player-${playerId++}`,
        label: playerInput.trim(),
      };
      setPlayers([...players, newPlayer]);
      setPlayerInput("");
    }
  };

  const onDragEnd = (result) => {
    const { source, destination, draggableId } = result;
    if (!destination) return;
    console.log("Drag Ended - Source Droppable ID:", source.droppableId);
    const draggedPlayer = players.find((p) => p.id === draggableId);
    if (!draggedPlayer) return;

    if (destination.droppableId.startsWith("court")) {
      const newAssignments = {
        ...courtAssignments,
        [destination.droppableId]: draggedPlayer,
      };

      setCourtAssignments(newAssignments);
      setPlayers((prev) => prev.filter((p) => p.id !== draggableId));
    } else if (
      source.droppableId === "player-pool" &&
      destination.droppableId === "player-pool"
    ) {
      const newPlayers = Array.from(players);
      const [removed] = newPlayers.splice(source.index, 1);
      newPlayers.splice(destination.index, 0, removed);
      setPlayers(newPlayers);
    }
  };

  const courtAreaRef = useRef(null);
  useEffect(() => {
    setPlayers(initialPlayers);
  }, []);

  return (
    <Box ref={courtAreaRef} sx={{ height: "100%", overflow: "auto" }}>
      <DragDropContext onDragEnd={onDragEnd}>
        <Grid container sx={{ minHeight: "100%" }}>
          <Grid
            size={{ xs: 12, md: 2 }}
            sx={{
              borderRight: { md: 1 },
              borderColor: "divider",
              p: 2,
              display: "flex",
              flexDirection: "column",
            }}
          >
            <TextField
              size="small"
              fullWidth
              sx={{ mb: 2 }}
              placeholder="Player A"
              value={playerInput}
              onChange={(e) => setPlayerInput(e.target.value)}
              onKeyDown={handleAddPlayer}
            />

            <Droppable droppableId="player-pool">
              {(provided) => (
                <Box
                  className="player-list-scroll"
                  ref={provided.innerRef}
                  {...provided.droppableProps}
                >
                  {players.map((player, index) => (
                    <Draggable
                      key={player.id}
                      draggableId={player.id}
                      index={index}
                    >
                      {(prov) => (
                        <Card
                          ref={prov.innerRef}
                          {...prov.draggableProps}
                          {...prov.dragHandleProps}
                          sx={{ mb: 1 }}
                          variant="outlined"
                        >
                          <CardContent sx={{ py: 1, textAlign: "center" }}>
                            <Typography variant="body2">{player.label}</Typography>
                          </CardContent>
                        </Card>
                      )}
                    </Draggable>
                  ))}
                  {provided.placeholder}
                </Box>
              )}
            </Droppable>
          </Grid>

          <Grid size={{ xs: 12, md: 10 }} sx={{ p: 2, overflow: "auto" }}>
            <Grid container spacing={2}>
              {[...Array(COURT_COUNT)].map((_, courtIndex) => {
                const courtId = `court${courtIndex + 1}`;
                return (
                  <Grid key={courtId} size={{ xs: 12, md: 6 }}>
                    <Paper variant="outlined" className="court-wrapper" sx={{ position: "relative" }}>
                      <Typography sx={{ px: 1, pt: 1 }}>{courtId}</Typography>
                      <Box
                        component="img"
                        src="./bad-court2.jpg"
                        alt={`Court ${courtIndex + 1}`}
                        sx={{ width: "100%", display: "block" }}
                      />
                      {AREA_LABELS.map((area) => {
                        const areaId = `${courtId}-area${area}`;
                        return (
                          <Droppable key={areaId} droppableId={areaId}>
                            {(provided, snapshot) => (
                              <div
                                ref={provided.innerRef}
                                {...provided.droppableProps}
                                className={`court-area court-area-${area.toLowerCase()} ${
                                  snapshot.isDraggingOver ? "bg-highlight" : ""
                                }`}
                              >
                                {courtAssignments[areaId] && (
                                  <Card variant="outlined" className="small-card">
                                    <CardContent sx={{ py: 0.5, textAlign: "center" }}>
                                      <Typography variant="caption">
                                        {courtAssignments[areaId].label}
                                      </Typography>
                                    </CardContent>
                                  </Card>
                                )}
                                {provided.placeholder}
                              </div>
                            )}
                          </Droppable>
                        );
                      })}
                    </Paper>
                  </Grid>
                );
              })}
            </Grid>
          </Grid>
        </Grid>
      </DragDropContext>
    </Box>
  );
};

export default HomePageError;

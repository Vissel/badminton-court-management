import React, { useState } from "react";
import { DragDropContext, Droppable, Draggable } from "react-beautiful-dnd";
import "bootstrap/dist/css/bootstrap.min.css";
import "../App.css"; // Assume this contains styles for court layout and positioning

const initialPlayers = ["Player A", "Player B", "Player C"];

const generateCourtAreas = (courtId) => {
  return ["A", "B", "C", "D"].map((area) => `${courtId}-area${area}`);
};

const HomePage = () => {
  const [players, setPlayers] = useState(initialPlayers);
  const [playerInput, setPlayerInput] = useState("");
  const [courtStatus, setCourtStatus] = useState({}); // { court1: "idle" | "in-progress" }
  const [courtPlayers, setCourtPlayers] = useState({}); // { court1: [players] }

  const onDragEnd = (result) => {
    const { source, destination, draggableId } = result;
    if (!destination) return;

    // Moving player from pool to court area
    if (source.droppableId === "player-pool" && destination.droppableId.includes("court")) {
      const courtId = destination.droppableId.split("-")[0];
      if (courtStatus[courtId] === "in-progress") return;

      setPlayers((prev) => prev.filter((p) => p !== draggableId));
      setCourtPlayers((prev) => {
        const newList = prev[courtId] ? [...prev[courtId], draggableId] : [draggableId];
        return { ...prev, [courtId]: newList };
      });
    }

    // Moving player back to player_area (only allowed if court is not in-progress)
    if (destination.droppableId === "player-pool" && source.droppableId.includes("court")) {
      const courtId = source.droppableId.split("-")[0];
      if (courtStatus[courtId] === "in-progress") return;

      setCourtPlayers((prev) => {
        const updated = (prev[courtId] || []).filter((p) => p !== draggableId);
        return { ...prev, [courtId]: updated };
      });
      setPlayers((prev) => [...prev, draggableId]);
    }
  };

  const handleAddPlayer = (e) => {
    e.preventDefault();
    if (playerInput && !players.includes(playerInput)) {
      setPlayers([...players, playerInput]);
      setPlayerInput("");
    }
  };

  const handleStart = (courtId) => {
    setCourtStatus((prev) => ({ ...prev, [courtId]: "in-progress" }));
  };

  const handleFinish = (courtId) => {
    setPlayers((prev) => [...prev, ...(courtPlayers[courtId] || [])]);
    setCourtPlayers((prev) => ({ ...prev, [courtId]: [] }));
    setCourtStatus((prev) => ({ ...prev, [courtId]: "idle" }));
  };

  const handleCancel = (courtId) => {
    const cancelledPlayers = (courtPlayers[courtId] || []).map((p) => p + " (C)");
    setPlayers((prev) => [...prev, ...cancelledPlayers]);
    setCourtPlayers((prev) => ({ ...prev, [courtId]: [] }));
    setCourtStatus((prev) => ({ ...prev, [courtId]: "idle" }));
  };

  const renderCourt = (courtId, index) => {
    const playersInCourt = courtPlayers[courtId] || [];
    const isReadyToStart = playersInCourt.length === 4 && courtStatus[courtId] !== "in-progress";
    const inProgress = courtStatus[courtId] === "in-progress";
    return (
      <div
        key={courtId}
        className="court-wrapper position-relative m-2"
        onMouseEnter={() => {}}
      >
        <img
          src="./bad-court.webp"
          alt="court"
          className="img-fluid"
          style={{ maxWidth: "100%" }}
        />

        {generateCourtAreas(courtId).map((areaId) => (
          <Droppable key={areaId} droppableId={areaId}>
            {(provided) => (
              <div
                className="court-area"
                ref={provided.innerRef}
                {...provided.droppableProps}
              >
                {(courtPlayers[courtId] || [])
                  .filter((_, idx) => areaId.endsWith(String.fromCharCode(65 + idx)))
                  .map((player, idx) => (
                    <Draggable key={player} draggableId={player} index={idx}>
                      {(provided) => (
                        <div
                          ref={provided.innerRef}
                          {...provided.draggableProps}
                          {...provided.dragHandleProps}
                          className="badge bg-primary m-1"
                        >
                          {player}
                        </div>
                      )}
                    </Draggable>
                  ))}
                {provided.placeholder}
              </div>
            )}
          </Droppable>
        ))}

        <div className="position-absolute top-0 start-0 p-2">
          {isReadyToStart && (
            <button
              className="btn btn-success btn-sm"
              onClick={() => handleStart(courtId)}
            >
              Start
            </button>
          )}
          {inProgress && (
            <>
              <span className="badge bg-warning">In-progress</span>
              <button
                className="btn btn-danger btn-sm ms-1"
                onClick={() => handleFinish(courtId)}
              >
                Finish
              </button>
              <button
                className="btn btn-outline-danger btn-sm ms-1"
                onClick={() => handleCancel(courtId)}
              >
                Cancel
              </button>
            </>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="container-fluid p-2">
      <DragDropContext onDragEnd={onDragEnd}>
        <div className="court_area d-flex flex-wrap justify-content-center align-items-end">
          {[...Array(8)].map((_, idx) => renderCourt(`court${idx + 1}`, idx))}
        </div>

        <div className="player_area sticky-bottom bg-light p-3 mt-3">
          <form onSubmit={handleAddPlayer} className="d-flex mb-2">
            <input
              value={playerInput}
              onChange={(e) => setPlayerInput(e.target.value)}
              className="form-control me-2"
              placeholder="Add new player"
            />
            <button className="btn btn-primary">Add</button>
          </form>

          <Droppable droppableId="player-pool" direction="horizontal">
            {(provided) => (
              <div
                ref={provided.innerRef}
                {...provided.droppableProps}
                className="d-flex flex-wrap"
              >
                {players.map((p, idx) => (
                  <Draggable key={p} draggableId={p} index={idx}>
                    {(provided) => (
                      <div
                        className="badge bg-secondary m-1"
                        ref={provided.innerRef}
                        {...provided.draggableProps}
                        {...provided.dragHandleProps}
                      >
                        {p}
                      </div>
                    )}
                  </Draggable>
                ))}
                {provided.placeholder}
              </div>
            )}
          </Droppable>
        </div>
      </DragDropContext>
    </div>
  );
};

export default HomePage;

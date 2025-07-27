import React, { useEffect, useRef, useState } from "react";
import { DragDropContext, Droppable, Draggable } from "react-beautiful-dnd";
import "../App.css"; // Make sure you define court-area positions here
import "../index.css";
import "bootstrap/dist/css/bootstrap.min.css";

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
  const [courtAssignments, setCourtAssignments] = useState({}); // { court1-areaA: playerObj }
  const [courtStatus, setCourtStatus] = useState({}); // { court1: 'default' | 'ready' | 'in-progress' }

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

    // If dropped into a court-area, assign it
    if (destination.droppableId.startsWith("court")) {
      const newAssignments = {
        ...courtAssignments,
        [destination.droppableId]: draggedPlayer,
      };

      // Remove from player list
      //   const remainingPlayers = players.filter((p) => p.id !== draggableId);
      //   setPlayers(remainingPlayers);
      setCourtAssignments(newAssignments);
      // Defer player removal to allow react-beautiful-dnd cleanup
      // setTimeout(() => {
      setPlayers((prev) => prev.filter((p) => p.id !== draggableId));
      // }, 0);
    }
    // You might also want to handle dragging within the player-pool if that's a desired feature
    else if (
      source.droppableId === "player-pool" &&
      destination.droppableId === "player-pool"
    ) {
      // Reorder players within the player-pool
      const newPlayers = Array.from(players);
      const [removed] = newPlayers.splice(source.index, 1);
      newPlayers.splice(destination.index, 0, removed);
      setPlayers(newPlayers);
    }
  };

  const courtAreaRef = useRef(null);
  useEffect(() => {
    setPlayers(initialPlayers);

    // Scroll to the bottom of the court area when the component mounts
    // if (courtAreaRef.current) {
    //   courtAreaRef.current.scrollTop = courtAreaRef.current.scrollHeight;
    // }
  }, []);

  return (
    <div className="container-fluid vh-100 overflow-auto" ref={courtAreaRef}>
      <div className="row h-100">
        {/* Player Area */}
        <DragDropContext onDragEnd={onDragEnd}>
          <div className="col-2 border-end p-3 d-flex flex-column">
            <input
              type="text"
              className="form-control mb-3"
              placeholder="Player A"
              value={playerInput}
              onChange={(e) => setPlayerInput(e.target.value)}
              onKeyDown={handleAddPlayer}
            />

            <Droppable droppableId="player-pool">
              {(provided) => (
                <div
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
                      {(provided) => (
                        <div
                          className="card mb-2"
                          ref={provided.innerRef}
                          {...provided.draggableProps}
                          {...provided.dragHandleProps}
                        >
                          <div className="card-body py-2 text-center">
                            {player.label}
                          </div>
                        </div>
                      )}
                    </Draggable>
                  ))}
                  {provided.placeholder}
                </div>
              )}
            </Droppable>
          </div>

          {/* Court Area */}
          <div className="col-10 p-3 overflow-auto">
            <div className="row">
              {[...Array(COURT_COUNT)].map((_, courtIndex) => {
                const courtId = `court${courtIndex + 1}`;
                return (
                  <div key={courtId} className="col-md-6 mb-4">
                    <div className="court-wrapper position-relative border">
                      <p>{courtId}</p>
                      <img
                        src="./bad-court2.jpg"
                        alt={`Court ${courtIndex + 1}`}
                        className="img-fruid w-100"
                      />
                      {AREA_LABELS.map((area, idx) => {
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
                                  <div className="card small-card text-center">
                                    {courtAssignments[areaId].label}
                                  </div>
                                )}
                                {provided.placeholder}
                              </div>
                            )}
                          </Droppable>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </DragDropContext>
      </div>
    </div>
  );
};

export default HomePageError;

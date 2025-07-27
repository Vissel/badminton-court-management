import React, { useEffect, useRef, useState } from "react";
import { DndProvider, useDrag, useDrop } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";

const ItemTypes = {
  PLAYER: "player",
};

const playersInitial = ["Player A", "Player B", "Player C"];
const courtIds = [8, 7, 6, 5, 4, 3, 2, 1];
const areaKeys = ["A", "B", "C", "D"];

function DraggablePlayer({ name, isLocked }) {
  const [{ isDragging }, drag] = useDrag(
    () => ({
      type: ItemTypes.PLAYER,
      item: { name },
      canDrag: !isLocked,
      collect: (monitor) => ({ isDragging: !!monitor.isDragging() }),
    }),
    [isLocked]
  );

  return (
    <div
      ref={drag}
      style={{
        opacity: isDragging ? 0.5 : 1,
        padding: "3px",
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

function DropZone({ courtId, areaKey, player, onDropPlayer, isLocked }) {
  const [hasDropped, setHasDropped] = useState(!!player);

  const [{ isOver }, drop] = useDrop(
    () => ({
      accept: ItemTypes.PLAYER,
      drop: (item) => {
        if (!isLocked) {
          onDropPlayer(item.name, courtId, areaKey);
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
      {player && <DraggablePlayer name={player} isLocked={isLocked} />}
    </div>
  );
}

function Court({
  id,
  players,
  onDropPlayer,
  isLocked,
  onStart,
  onFinish,
  onCancel,
}) {
  const [hovering, setHovering] = useState(false);
  const filledPlayers = Object.values(players).filter(Boolean);
  const readyToStart = filledPlayers.length === 4;
  return (
    <div
      style={{ width: "45%", margin: "10px", position: "relative" }}
      onMouseEnter={() => setHovering(true)}
      onMouseLeave={() => setHovering(false)}
    >
      <h5>
        court{id} {isLocked && "(In-Progress)"}{" "}
      </h5>
      <div
        style={{
          backgroundColor: "#006f4a",
          backgroundImage: 'url("/bad-court2.jpg")',
          backgroundSize: "cover",
          height: "180px",
          display: "grid",
          gridTemplateColumns: "repeat(2, 1fr)",
          gridTemplateRows: "repeat(2, 1fr)",
          gap: "5px",
          padding: "10px",
          position: "relative",
          transition: "all 2s ease",
        }}
      >
        {areaKeys.map((areaKey) => (
          <DropZone
            key={areaKey}
            courtId={id}
            areaKey={areaKey}
            player={players[areaKey]}
            onDropPlayer={onDropPlayer}
            isLocked={isLocked}
          />
        ))}
        {/* Start button */}
        {!isLocked && hovering && (
          <button
            onClick={() => onStart(id)}
            style={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%, -50%)",
              padding: "10px 20px",
              backgroundColor: "#007bff",
              color: "white",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              zIndex: 1,
              transition: "opacity 2s ease",
            }}
          >
            Start
          </button>
        )}
        {/* Finish and Cancel buttons */}
        {isLocked && hovering && (
          <div
            style={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%, -50%)",
              display: "flex",
              gap: "10px",
              zIndex: 2,
              transition: "opacity 2s ease",
            }}
          >
            <button className="btn btn-danger" onClick={() => onFinish(id)}>
              Finish
            </button>
            <button
              className="btn btn-outline-danger"
              onClick={() => onCancel(id)}
            >
              Cancel
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function PlayerArea({
  availablePlayers,
  onDropPlayerBack,
  onAddPlayer,
  newPlayer,
  setNewPlayer,
}) {
  const [{ isOver }, drop] = useDrop(() => ({
    accept: ItemTypes.PLAYER,
    drop: (item) => onDropPlayerBack(item.name),
    collect: (monitor) => ({ isOver: !!monitor.isOver() }),
  }));

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
        width: "20%",
        padding: "20px",
        backgroundColor: isOver ? "#eef" : "#f8f9fa",
        borderRight: "1px solid #ccc",
        transition: "background-color 2s ease",
      }}
    >
      <input
        type="text"
        placeholder="Player A"
        value={newPlayer}
        onChange={(e) => setNewPlayer(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && handleAdd()}
        style={{
          width: "100%",
          padding: "10px",
          marginBottom: "15px",
          boxSizing: "border-box",
        }}
      />
      {availablePlayers.map((p) => (
        <DraggablePlayer key={p} name={p} isLocked={false} />
      ))}
    </div>
  );
}

/* HomePage */
function HomePage() {
  const [availablePlayers, setAvailablePlayers] = useState(playersInitial);
  const [newPlayer, setNewPlayer] = useState("");
  const scrollRef = useRef(null);
  const [courts, setCourts] = useState(() => {
    const initialCourts = {};
    courtIds.forEach((id) => {
      initialCourts[id] = { A: null, B: null, C: null, D: null };
    });
    return initialCourts;
  });
  //   For scrolling to the end
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, []);

  const onDropPlayer = (playerName, courtId, areaKey) => {
    setCourts((prev) => {
      const updated = { ...prev };
      for (const id in updated) {
        for (const key in updated[id]) {
          if (updated[id][key] === playerName) updated[id][key] = null;
        }
      }
      updated[courtId][areaKey] = playerName;
      return updated;
    });
    setAvailablePlayers((prev) => prev.filter((p) => p !== playerName));
  };

  const onDropPlayerBack = (playerName) => {
    setCourts((prev) => {
      const updated = { ...prev };
      for (const id in updated) {
        for (const key in updated[id]) {
          if (updated[id][key] === playerName) updated[id][key] = null;
        }
      }
      return updated;
    });
    setAvailablePlayers((prev) =>
      prev.includes(playerName) ? prev : [...prev, playerName]
    );
  };

  const onAddPlayer = (name) => {
    setAvailablePlayers((prev) => [...prev, name]);
  };

  /* On Start*/
  const [lockedCourts, setLockedCourts] = useState({});
  const onStart = (courtId) => {
    setLockedCourts((prev) => ({ ...prev, [courtId]: true }));
  };

  /** On Finish */
  const onFinish = (courtId) => {
    setCourts((prev) => {
      const updated = { ...prev };
      const playersToReturn = Object.values(updated[courtId]).filter(Boolean);
      courtIds.forEach((id) => {
        for (const key in updated[id]) {
          if (playersToReturn.includes(updated[id][key]))
            updated[id][key] = null;
        }
      });
      return updated;
    });
    setAvailablePlayers((prev) => [
      ...prev,
      ...Object.values(courts[courtId]).filter(Boolean),
    ]);
    setLockedCourts((prev) => {
      const updated = { ...prev };
      delete updated[courtId];
      return updated;
    });

    alert(`On finish of court ${courtId}`);
  };
  /**On Cancel */
  const onCancel = (courtId) => {
    onFinish(courtId);
    alert(`On Cancel court ${courtId}`);
  };
  return (
    <DndProvider backend={HTML5Backend}>
      <div style={{ display: "flex", height: "100vh" }}>
        <PlayerArea
          availablePlayers={availablePlayers}
          onDropPlayerBack={onDropPlayerBack}
          onAddPlayer={onAddPlayer}
          newPlayer={newPlayer}
          setNewPlayer={setNewPlayer}
        />
        <div
          ref={scrollRef}
          style={{
            display: "flex",
            flexWrap: "wrap",
            width: "80%",
            padding: "20px",
            gap: "20px",
            overflowY: "scroll",
          }}
        >
          {courtIds.map((id) => (
            <Court
              key={id}
              id={id}
              players={courts[id]}
              onDropPlayer={onDropPlayer}
              isLocked={lockedCourts[id]}
              onStart={onStart}
              onFinish={onFinish}
              onCancel={onCancel}
            />
          ))}
        </div>
      </div>
    </DndProvider>
  );
}

export default HomePage;

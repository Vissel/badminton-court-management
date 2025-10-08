import { useState } from "react";
import DropZone from "./DropZone";

const areaKeys = ["A", "C", "B", "D"];
export default function Court({
  id,
  players,
  onDropPlayer,
  occupied,
  isLocked,
  onStart,
  onFinish,
  onCancel,
  onDropService,
}) {
  const [hovering, setHovering] = useState(false);
  const [readyToStart, setReadyToStart] = useState(false);
  const filledPlayers = Object.values(players).filter(Boolean);

  const initReadyMap = () => {
    return Object.fromEntries(areaKeys.map((item) => [item, false]));
  };
  // A, C, B, D

  return (
    <div
      style={{ width: "100%", position: "relative" }}
      onMouseEnter={() => setHovering(true)}
      onMouseLeave={() => setHovering(false)}
    >
      <div class="d-flex">
        <div
          style={{
            left: "0%",
            height: "45px",
          }}
        >
          court{id} {isLocked && "(In-Progress)"}{" "}
        </div>

        {/* Finish and Cancel buttons */}
        {isLocked && (
          <div
            style={{
              position: "absolute",
              // top: "50%",
              // left: "50%",
              right: "0%",
              // transform: "translate(-50%, -50%)",
              display: "flex",
              gap: "10px",
              zIndex: 2,
              transition: "opacity 2s ease",
            }}
          >
            <button
              className="btn btn-success"
              onClick={() => onFinish(id)}
            >
              Finish
            </button>
            <button
              className="btn btn-outline-secondary"
              onClick={() => onCancel(id)}
            >
              Cancel
            </button>
          </div>
        )}
      </div>
      <div>
        <div
          style={{
            backgroundColor: "white",
            backgroundImage: 'url("/bad-court2.jpg")',
            backgroundSize: "cover",
            backgroundRepeat: "no-repeat",
            backgroundPosition: "center",
            height: "250px",
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
              occupied={occupied}
              isLocked={isLocked}
              onDropService={onDropService}
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
                backgroundColor: "#4198f7",
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
        </div>
      </div>
    </div>
  );
}

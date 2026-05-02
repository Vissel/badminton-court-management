import React, { useEffect, useCallback } from "react";
import "./ServiceDialog.css";

const CancelConfirm = ({ show, courtId, onConfirm, onExit }) => {
  const handleEscPress = useCallback((event) => {
    if (event.key === "Escape") {
      onExit();
    }
  }, [onExit]);

  useEffect(() => {
    if (show) {
      // Add event listener when the component mounts
      document.addEventListener("keydown", handleEscPress);

      // Clean up: remove event listener when the component unmounts
      return () => {
        document.removeEventListener("keydown", handleEscPress);
      };
    }
  }, [show, handleEscPress]);
  if (!show) return null;
  return (
    <div className="dialog-overlay">
      <div className="service-dialog-box">
        <h5 className="mb-3 text-center">Xác nhận huỷ trận đấu ?</h5>

        {/* Buttons */}
        <div className="dialog-actions mt-4">
          <button
            className="btn btn-danger me-2"
            onClick={() => onConfirm(courtId)}
          >
            Xác nhận
          </button>
          <button className="btn btn-secondary" onClick={onExit}>
            Tắt
          </button>
        </div>
      </div>
    </div>
  );
};

export default CancelConfirm;

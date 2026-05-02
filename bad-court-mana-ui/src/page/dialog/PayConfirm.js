import React, { useEffect, useCallback, useState } from "react";
import "./ServiceDialog.css";
import { TYPE } from "../HomePage";

const PayConfirm = ({ show, data, onConfirm, onExit }) => {
  const [btnClass, setBtnClass] = useState('');

  const handleEscPress = useCallback((event) => {
    if (event.key === "Escape") {
      onExit();
    }
  }, [onExit]);

  useEffect(() => {
    if (show) {
      if(TYPE.PAY ===data.type){
        setBtnClass('btn-primary')
      }else{
        setBtnClass('btn-danger');
      }
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
        <h5 className="mb-3 text-center">{data.title}</h5>

        {/* Buttons */}
        <div className="dialog-actions mt-4">
          <button
            className={`btn ${btnClass} me-2`}
            onClick={() => onConfirm(data)}
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

export default PayConfirm;

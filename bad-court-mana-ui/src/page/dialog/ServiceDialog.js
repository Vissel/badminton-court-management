import React, { useCallback, useEffect, useState } from "react";
import "./ServiceDialog.css";
import { TYPE } from "../HomePage";

const ServiceDialog = ({ playerName, services, onClose, onPay, onDelete }) => {
  const [totalCost, setTotalCost] = useState(0);

  const onPreDelete = () => {
    onClose(false);
    onDelete(playerName, TYPE.CANCEL, services, Number(totalCost));
  };

  const onPrePay = () => {
    onPay(playerName, TYPE.PAY, services, Number(totalCost));
  };

  // Define a callback function to handle the keydown event
  const handleEscPress = useCallback(
    (event) => {
      if (event.key === "Escape") {
        console.log("[ServiceDialog] Escape key pressed!");
        onClose();
      }
    },
    [onClose]
  ); // Empty dependency array means this callback is memoized and won't change on re-renders

  // Use useEffect to add and remove the event listener
  useEffect(() => {
    if (services) {
      const totalAmount = services.reduce((sum, item) => {
        const amount =
          Number(item.slice(item.lastIndexOf("-") + 1).trim()) || 0;
        return sum + amount;
      }, 0);
      console.log(totalAmount);
      setTotalCost(totalAmount);
    }

    // Add event listener when the component mounts
    document.addEventListener("keydown", handleEscPress);

    // Clean up: remove event listener when the component unmounts
    return () => {
      document.removeEventListener("keydown", handleEscPress);
    };
  }, [handleEscPress]); // Dependency array includes handleEscPress to ensure the correct function is always used

  return (
    <div className="dialog-overlay">
      <div className="service-dialog-box">
        <div
          style={{
            display: "flex",
            flexDirection: "row",
            justifyContent: "flex-end",
          }}
        >
          <div className="btn btn-secondary close-button" onClick={onClose}>
            X
          </div>
        </div>
        <h3> Bảng chi phí của: {playerName}</h3>
        <h6 style={{color: "blue"}}>Tổng cộng: {totalCost} vnd </h6>
        {services.length > 0 ? (
          <ul className="service-list">
            {services.map((service, idx) => (
              <li key={idx}>{service} vnd</li>
            ))}
          </ul>
        ) : (
          <p>No services assigned.</p>
        )}

        <div className="dialog-actions">
          <button className="btn btn-primary" onClick={onPrePay}>
            Thanh toán
          </button>
          <button className="btn btn-outline-danger" onClick={onPreDelete}>
            Xoá + không thanh toán
          </button>
        </div>
      </div>
    </div>
  );
};

export default ServiceDialog;

import React from "react";
import "./ServiceDialog.css";

const ServiceDialog = ({ playerName, services, onClose, onPay }) => {
  return (
    <div className="dialog-overlay">
      <div className="dialog-box">
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
        <h3>Services for {playerName}</h3>
        {services.length > 0 ? (
          <ul className="service-list">
            {services.map((service, idx) => (
              <li key={idx}>{service}</li>
            ))}
          </ul>
        ) : (
          <p>No services assigned.</p>
        )}

        <div className="dialog-actions">
          <button className="btn btn-primary" onClick={onPay}>
            Pay
          </button>
          <button className="btn btn-outline-danger" onClick={onPay}>
            Remove
          </button>
          <div className="dialog-cancel">
            <button className="btn btn-secondary" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ServiceDialog;

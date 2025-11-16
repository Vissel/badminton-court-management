import React, {useCallback, useEffect} from "react";
import "./ServiceDialog.css";

const ServiceDialog = ({ playerName, services, onClose, onPay, onDelete }) => {
  const handleDelete =()=>{
    onDelete(playerName);
    onClose(false);
  }

  // Define a callback function to handle the keydown event
  const handleEscPress = useCallback((event) => {
    if (event.key === 'Escape') {
      console.log('[ServiceDialog] Escape key pressed!');
      onClose();
    }
  }, []); // Empty dependency array means this callback is memoized and won't change on re-renders

  // Use useEffect to add and remove the event listener
  useEffect(() => {
    // Add event listener when the component mounts
    document.addEventListener('keydown', handleEscPress);

    // Clean up: remove event listener when the component unmounts
    return () => {
      document.removeEventListener('keydown', handleEscPress);
    };
  }, [handleEscPress]); // Dependency array includes handleEscPress to ensure the correct function is always used

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
        <h3> Bảng chi phí của: {playerName}</h3>
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
          <button className="btn btn-primary" onClick={onPay}>
            Thanh toán
          </button>
          <button className="btn btn-outline-danger" onClick={handleDelete}>
            Xoá + không thanh toán
          </button>
        </div>
      </div>
    </div>
  );
};

export default ServiceDialog;

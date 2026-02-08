import React, { useCallback, useEffect, useState } from "react";
import "./ServiceDialog.css";
import { TYPE } from "../HomePage";

const ServiceDialog = ({
  playerName,
  services = [],
  onClose,
  onPay,
  onDelete,
  onUpdateServices,
}) => {
  const [totalCost, setTotalCost] = useState(0);
  const [serviceName, setServiceName] = useState("");
  const [serviceCost, setServiceCost] = useState("");

  const recalcTotal = useCallback((serviceList) => {
    return serviceList.reduce((sum, item) => {
      const amount = Number(item.slice(item.lastIndexOf("-") + 1).trim()) || 0;
      return sum + amount;
    }, 0);
  }, []);

  const onPreDelete = () => {
    onClose(false);
    onDelete(playerName, TYPE.CANCEL, services, Number(totalCost));
  };

  const onPrePay = () => {
    onPay(playerName, TYPE.PAY, services, Number(totalCost));
  };

  const handleAddService = () => {
    if (!serviceName || !serviceCost) return;

    const newService = `${serviceName}-${serviceCost}`;
    const updated = [...services, newService];

    onUpdateServices(playerName, updated);
    setServiceName("");
    setServiceCost("");
  };

  const handleRemoveService = (index) => {
    const updated = services.filter((_, i) => i !== index);
    onUpdateServices(playerName, updated);
  };

  // const handleEscPress = useCallback(
  //   (event) => {
  //     if (event.key === "Escape") {
  //       onClose();
  //     }
  //   },
  //   [onClose]
  // );
  const handleKeyDown = useCallback(
  (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      handleAddService();
    }

    if (event.key === "Escape") {
      onClose(false); // close dialog
    }
  },
  [handleAddService, onClose]
);

  useEffect(() => {
    setTotalCost(recalcTotal(services));
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [services, handleKeyDown, recalcTotal]);

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

        <h3>Bảng chi phí của: {playerName}</h3>
        <h6 style={{ color: "blue" }}>Tổng cộng: {totalCost} vnd</h6>

        {/* ➕ ADD SERVICE */}
        <div className="d-flex bd-highlight align-items-center">
          <div className="p-2 flex-grow-1 bd-highlight">
            <input
              type="text"
              placeholder="Tên dịch vụ"
              value={serviceName}
              onChange={(e) => setServiceName(e.target.value)}
            />
          </div>
          <div className="p-2 bd-highlight">
            <input
              type="number"
              placeholder="Giá"
              value={serviceCost}
              onChange={(e) => setServiceCost(e.target.value)}
            />
          </div>
          <div className="p-2 bd-highlight">
            <button className="btn btn-success" onClick={handleAddService}>
              +
            </button>
          </div>
        </div>

        <div className="add-service"></div>

        {/* SERVICE LIST */}
        {services.length > 0 ? (
          <ul className="service-list">
            {services.map((service, idx) => (
              <li key={idx} className="service-item">
                <div className="d-flex justify-content-between">
                  <div className="p-2 bd-highlight">
                    <span>{service} vnd</span>
                  </div>

                  <div className="p-2 bd-highlight">
                    <button
                      className="btn btn-sm btn-outline-danger"
                      onClick={() => handleRemoveService(idx)}
                    >
                      ✕
                    </button>
                  </div>
                </div>
              </li>
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

import React, { useCallback, useEffect, useState } from "react";
import "./ServiceDialog.css";
import { TYPE } from "../HomePage";
import { VN_CURRENCY, formatVND, rawNumber } from "./../MoneyUtils";

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
  const [servicesVO, setServicesVO] = useState([]);
  const recalcTotal = useCallback((serviceList) => {
    return serviceList.reduce((sum, item) => {
      const amount = item.cost || 0;
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

    const newService = {
      serviceName: serviceName,
      cost: Number(serviceCost),
      costFormat: formatVND(serviceCost),
    };
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
    // setServicesVO(
    //   services.map(s=> {return `${s.slice(0, s.lastIndexOf("-")).trim()} - ${formatVND(s.slice(s.lastIndexOf("-") + 1).trim())}`;}
    // ));
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

        <h3 style={{ textAlign: "center" }}>Bảng chi phí của: </h3>
        <h4 style={{ textAlign: "center" }}>{playerName} </h4>
        <div className="d-flex bd-highlight align-items-start">
          <h6 style={{ color: "blue" }}>
            Tổng cộng: {formatVND(totalCost)} {VN_CURRENCY}
          </h6>
        </div>
        {/* ➕ ADD SERVICE */}
        <div className="service-row">
          <input
            className="service-col-name"
            type="text"
            placeholder="Tên dịch vụ"
            value={serviceName}
            onChange={(e) => setServiceName(e.target.value)}
          />
          <input
            className="service-col-cost"
            type="text"
            placeholder="Giá"
            value={serviceCost}
            onChange={(e) => setServiceCost(e.target.value)}
          />
          <button className="btn btn-success service-col-action" onClick={handleAddService}>
            +
          </button>
        </div>

        {/* SERVICE LIST */}
        {services.length > 0 ? (
          <ul className="service-list">
            {services.map((service, idx) => (
              <li key={idx} className="service-row">
                <span className="service-col-name">
                  {service.serviceName}
                </span>
                <span className="service-col-cost">
                  {service.costFormat} {VN_CURRENCY}
                </span>
                <button
                  className="btn btn-sm btn-outline-danger service-col-action"
                  onClick={() => handleRemoveService(idx)}
                >
                  ✕
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-muted">Không có dịch vụ nào.</p>
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

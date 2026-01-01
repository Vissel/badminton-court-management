// src/components/ShuttleBallDialog.js
import { useEffect, useState, useCallback } from "react";
import "./Dialog.css"; // reuse same CSS as GameDialog
import api from "../../api/index";

const ShuttleBallDialog = ({
  courtProcessing,
  show,
  onSaveBallOntoCourt,
  onCancel,
}) => {
  const [options, setOptions] = useState([]);
  const [selectedValue, setSelectedValue] = useState("");
  const [quantity, setQuantity] = useState(0);
  const [addedItems, setAddedItems] = useState([]);

  const handleEscPress = useCallback((event) => {
    if (event.key === "Escape") {
      console.log("[ShuttleBallDialog] Escape key pressed!");
      onCancel();
    }
  }, []);

  useEffect(() => {
    if (show) {
      setAddedItems([]);
      api
        .get("/court-mana/getShuttleBalls")
        .then((res) => res.data)
        .then((data) => {
          setOptions(data.map((b) => `${b.shuttleName} - ${b.shuttleCost}`));
          setSelectedValue("");
        })
        .catch((err) => console.error("Error fetching shuttle balls:", err));

      // Add event listener when the component mounts
      document.addEventListener("keydown", handleEscPress);

      // Clean up: remove event listener when the component unmounts
      return () => {
        document.removeEventListener("keydown", handleEscPress);
      };
    }
  }, [show, handleEscPress]);

  const handleDelete = (index) => {
    setAddedItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleAdd = () => {
    if (!selectedValue || Number.isNaN(quantity) || quantity < 1) {
      return;
    }
    const intQuantity = parseInt(quantity);
    // Check if item already exists → update quantity
    setAddedItems((prev) => {
      const existing = prev.find((it) => it.name === selectedValue);
      if (existing) {
        return prev.map((it) =>
          it.name === selectedValue
            ? { ...it, quantity: it.quantity + intQuantity }
            : it
        );
      }
      return [...prev, { name: selectedValue, quantity: intQuantity }];
    });
    setQuantity(1);
  };

  const handleSave = (courtProcessing) => {
    // convert to {key:value} map format
    const resultMap = [];
    addedItems.forEach((item) => {
      resultMap[item.name] = item.quantity;
    });
    console.log("✅ Saved shuttle balls map:", resultMap);
    onSaveBallOntoCourt(courtProcessing, resultMap);
  };

  const checkAndSetQuantity = (newQuantity) => {
    if (!Number.isNaN(newQuantity)) {
      setQuantity(newQuantity);
    }
   
  };

  if (!show) return null;

  return (
    <div className="dialog-overlay">
      <div
        className="dialog-box shuttle-dialog-box"
        style={{
          width: "500px",
        }}
      >
        <div className="dialog-content">
          <h5>Thêm cầu</h5>
          <div className="d-flex align-items-center mb-2">
            <select
              className="form-select me-2"
              value={selectedValue}
              onChange={(e) => setSelectedValue(e.target.value)}
            >
              <option value="">-- Chọn cầu --</option>
              {options.map((opt, i) => (
                <option key={i} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
            <input
              type="number"
              className="form-control"
              value={quantity}
              onChange={(e) => checkAndSetQuantity(e.target.value)}
              style={{ width: "25%", margin: "0 7px 0 0" }}
            />
            <button className="btn btn-primary" onClick={handleAdd}>
              Thêm
            </button>
          </div>
        </div>
        {/* Scrollable rows */}
        <div className="dialog-scrollable">
          <div className="added-list">
            {addedItems.length === 0 && (
              <div className="text-muted text-center">
                Chưa có cầu nào được thêm
              </div>
            )}

            {addedItems.map((item, index) => (
              <div
                key={index}
                className="added-item d-flex justify-content-between align-items-center"
              >
                <div>
                  <strong>{item.name}</strong> &nbsp;–&nbsp;
                  <span className="text-secondary">
                    Số lượng: {item.quantity}
                  </span>
                </div>
                <button
                  className="btn btn-sm btn-outline-danger"
                  onClick={() => handleDelete(index)}
                >
                  X
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Buttons (non-scrollable) */}
        <div className="fixed-bottom-actions">
          <button
            className="btn btn-success"
            onClick={() => handleSave(courtProcessing)}
          >
            Lưu
          </button>
          <button className="btn btn-secondary" onClick={onCancel}>
            Hủy
          </button>
        </div>
      </div>
    </div>
  );
};

export default ShuttleBallDialog;

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
  const [selectedIndex, setSelectedIndex] = useState();
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
          // setOptions(data.map((b) => `${b.shuttleName} - ${b.shuttleCost}`));
          setOptions(
            data.map((b) => {
              return {
                shuttleName: b.shuttleName,
                cost: b.cost,
                costFormat: b.costFormat,
                currency: b.currency,
              };
            })
          );
          setSelectedIndex(0);
          setQuantity(1);
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
    if (Number.isNaN(quantity) || quantity < 1) {
      return;
    }
    const intQuantity = parseInt(quantity);
    // Check if item already exists → update quantity
    const selectedValue = options[selectedIndex]; // This is the full object: {shuttleName, cost, ...}

    setAddedItems((prev) => {
      // 1. Search using 'shuttleName' since that is the key in your objects
      const existing = prev.find(
        (it) => it.shuttleName === selectedValue.shuttleName
      );

      if (existing) {
        // 2. Update quantity of the existing object
        return prev.map((it) =>
          it.shuttleName === selectedValue.shuttleName
            ? { ...it, quantity: it.quantity + intQuantity }
            : it
        );
      }

      // 3. Add new item with all required fields + initial quantity
      return [
        ...prev,
        {
          shuttleName: selectedValue.shuttleName,
          cost: selectedValue.cost,
          costFormat: selectedValue.costFormat,
          currency: selectedValue.currency,
          quantity: intQuantity,
        },
      ];
    });
    setQuantity(1);
  };

  const handleSave = (courtProcessing) => {
    // convert to {key:value} map format
    // const resultMap = [];
    // addedItems.forEach((item) => {
    //   resultMap[item] = item.quantity;
    // });
    console.log("✅ Saved shuttle balls map:", addedItems);
    onSaveBallOntoCourt(courtProcessing, addedItems);
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
              value={selectedIndex}
              onChange={(e) => setSelectedIndex(e.target.value)}
            >
              {/* <option value="">-- Chọn cầu --</option> */}
              {options.map((ball, index) => (
                <option key={ball.shuttleName} value={index}>
                  {ball.shuttleName} - {ball.costFormat} {ball.currency}
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
            <button className="btn btn-success" onClick={handleAdd}>
              +
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
                  <strong>{item.shuttleName}</strong>
                  &nbsp;–&nbsp;
                  <span>{item.costFormat}</span> &nbsp;–&nbsp;
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

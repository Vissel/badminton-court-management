import React, { useEffect, useState } from "react";
import "./GameDialog.css";
const myObject = {
      propertyOne: 'value1',
      propertyTwo: 123,
      nestedObject: {
        nestedProperty: 'anotherValue'
      }
    };
const GameDialog = ({ show, data, onConfirm, onCancel }) => {
  const [formData, setFormData] = useState(null);
  const [totalCost, setTotalCost] = useState(null);
  const [actualCost, setActualCost] = useState(null);

  useEffect(() => {
    if (show && data) {
      setFormData(JSON.parse(JSON.stringify(data)));
      // setTotalCostByBallList(data);

      let total = 0.0;
      Object.entries(data.ballResultMap).forEach(([key, qty]) => {
        const match = key.match(/shuttleName=(.*?), cost=(.*?)[)\]]/);
        const cost = match ? parseFloat(match[2]) : 0;
        total += cost * qty;
      });

      setTotalCost(
        total.toLocaleString("it-IT", { style: "currency", currency: "VND" })
      );
    }
  }, [data]);
  
  if (!show || !formData) return null;

  const getTotalCostByBall = (newMap) =>{
    let total = 0.0;
    Object.entries(newMap).forEach(([key, qty]) => {
        const match = key.slice(key.lastIndexOf("-") + 1).trim()
        const cost = match ? parseFloat(match) : 0;
        total += cost * qty;
      });
      return total.toLocaleString("it-IT", { style: "currency", currency: "VND" });
  }

  
  const handleInputChange = (path, value) => {
    const keys = path.split(".");
    const updated = { ...formData };
    let temp = updated;
    for (let i = 0; i < keys.length - 1; i++) temp = temp[keys[i]];
    temp[keys[keys.length - 1]] = value;
    setFormData(updated);
  };

  const handleBallChange = (index, field, value) => {
    const updatedBalls = parseBallMap();
    updatedBalls[index][field] = value;

    const newMap = {};
    updatedBalls.forEach((b) => {
      const key = `${b.shuttleName} - ${b.cost}`;
      newMap[key] = b.quantity;
    });
    setFormData({ ...formData, ballResultMap: newMap });
    setTotalCost(getTotalCostByBall(newMap));
  };

  const parseBallMap = () => {
    if (!formData.ballResultMap) return [];

    return Object.entries(formData.ballResultMap).map(([key, qty]) => {
      const match = key.match(/shuttleName=(.*?), cost=(.*?)[)\]]/);
      return {
        shuttleName: match ? match[1] : key,
        cost: match ? parseFloat(match[2]) : 0,
        quantity: qty,
      };
    });
  };
  const ballList = parseBallMap();

  return (
    <div className="dialog-overlay">
      <div className="dialog-box">
        <h5 className="mb-3 text-center">Game Summary</h5>

        {/* Court Name */}
        <div class="d-flex flex-row">
          <div class="align-self-center p-2">
            <label className="form-label fw-bold">Tong tien: {totalCost}</label>
          </div>
          <div className="p-2">
            <p></p>
          </div>
          <div class="align-self-center p-2">
            <label className="form-label fw-bold" style={{ color: " #4207e6" }}>
              Tong thuc te:
            </label>
          </div>
        </div>
        <div class="d-flex flex-row">
          <div class="align-self-center p-2">
            <label className="form-label fw-bold">Court:</label>
          </div>

          <div className="p-2">
            <input
              type="text"
              className="form-control"
              value={formData.courtResult?.courtName || ""}
              disabled
            />
          </div>
        </div>
        {/* Shuttle Balls */}
        <div className="d-flex flex-row">
          <div class="p-2">
            <label className="form-label fw-bold">Shuttle Balls:</label>
          </div>
          <div className="ball-list">
            {ballList.map((b, index) => (
              <div key={index} className="d-flex align-items-center mb-2">
                <input
                  type="text"
                  className="form-control me-2"
                  value={b.shuttleName}
                  disabled
                  style={{ width: "20%" }}
                />
                <input
                  type="number"
                  className="form-control me-2"
                  value={b.cost}
                  disabled
                  style={{ width: "20%" }}
                />
                <input
                  type="number"
                  className="form-control"
                  value={b.quantity}
                  onChange={(e) =>
                    handleBallChange(
                      index,
                      "quantity",
                      parseInt(e.target.value)
                    )
                  }
                  style={{ width: "20%" }}
                />
              </div>
            ))}
          </div>
        </div>

        {/* Teams */}
        <div className="team-container">
          {/* Team One */}
          <div className="team">
            <h6 className="text-center">Team One</h6>

            {/* Row 1 */}
            <div className="player-row">
              <input
                type="text"
                className="form-control me-2"
                disabled
                value={formData.teamOneResult?.playerOneName || ""}
              />
              <input
                type="number"
                className="form-control"
                value={formData.teamOneResult?.expenseOne || 0}
                onChange={(e) =>
                  handleInputChange(
                    "teamOneResult.expenseOne",
                    parseFloat(e.target.value)
                  )
                }
              />
            </div>

            {/* Row 2 */}
            <div className="player-row">
              <input
                type="text"
                className="form-control me-2"
                disabled
                value={formData.teamOneResult?.playerTwoName || ""}
              />
              <input
                type="number"
                className="form-control"
                value={formData.teamOneResult?.expenseTwo || 0}
                onChange={(e) =>
                  handleInputChange(
                    "teamOneResult.expenseTwo",
                    parseFloat(e.target.value)
                  )
                }
              />
            </div>
          </div>

          {/* Divider */}
          <div className="divider"></div>

          {/* Team Two */}
          <div className="team">
            <h6 className="text-center">Team Two</h6>

            {/* Row 1 */}
            <div className="player-row">
              <input
                type="text"
                className="form-control me-2"
                disabled
                value={formData.teamTwoResult?.playerOneName || ""}
              />
              <input
                type="number"
                className="form-control"
                value={formData.teamTwoResult?.expenseOne || 0}
                onChange={(e) =>
                  handleInputChange(
                    "teamTwoResult.expenseOne",
                    parseFloat(e.target.value)
                  )
                }
              />
            </div>

            {/* Row 2 */}
            <div className="player-row">
              <input
                type="text"
                className="form-control me-2"
                disabled
                value={formData.teamTwoResult?.playerTwoName || ""}
              />
              <input
                type="number"
                className="form-control"
                value={formData.teamTwoResult?.expenseTwo || 0}
                onChange={(e) =>
                  handleInputChange(
                    "teamTwoResult.expenseTwo",
                    parseFloat(e.target.value)
                  )
                }
              />
            </div>
          </div>
        </div>

        {/* Buttons */}
        <div className="dialog-actions mt-4">
          <button
            className="btn btn-success me-2"
            onClick={() => onConfirm(formData)}
          >
            Confirm
          </button>
          <button className="btn btn-secondary" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
};

export default GameDialog;

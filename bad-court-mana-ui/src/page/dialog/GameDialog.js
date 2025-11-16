import React, { useEffect, useState, useCallback } from "react";
import "./GameDialog.css";
const WIN = "Win";

const GameDialog = ({ show, data, onConfirm, onCancel }) => {
  const [formData, setFormData] = useState(null);
  const [totalCostStr, setTotalCostStr] = useState(null);
  const [totalCost, setTotalCost] = useState(0);
  const [actualCostStr, setActualCostStr] = useState(null);
  const [actualCost, setActualCost] = useState(0);
  const [ballList, setBallList] = useState([]);
  const [winnerTeam, setWinnerTeam] = useState(null);
  const [team1ClassName, setTeam1ClassName] = useState("team"); // ADD THIS
  const [team2ClassName, setTeam2ClassName] = useState("team"); // ADD THIS

  const DEFAULT_BTN_CLASS = "btn btn-outline-secondary me-2";
  const SUCCESS_BTN_CLASS = "btn btn-success me-2";
  const [team1BtnClassName, setBtnTeam1ClassName] = useState(DEFAULT_BTN_CLASS);
  const [team2BtnClassName, setBtnTeam2ClassName] = useState(DEFAULT_BTN_CLASS);

  const setTotalCostValue = (number) => {
    setTotalCostStr(
      number.toLocaleString("it-IT", { style: "currency", currency: "VND" })
    );
    setTotalCost(number);
  };
  const setActualCostValue = (number) => {
    setActualCostStr(
      number.toLocaleString("it-IT", { style: "currency", currency: "VND" })
    );
    setActualCost(number);
  };
  const parseBallMap = () => {
    if (!data.ballResultMap) return [];

    return Object.entries(data.ballResultMap).map(([key, qty]) => {
      const match = key.match(/shuttleName=(.*?), cost=(.*?)[)\]]/);
      return {
        shuttleName: match ? match[1] : key,
        cost: match ? parseFloat(match[2]) : 0,
        quantity: qty,
      };
    });
  };

  const checkPlayerNotNull = (playerName) => {
    return playerName != null;
  };
  const setExpenseToPlayer = (playerName, expenseOfPlayer, cost) => {
    if (checkPlayerNotNull(playerName)) {
      [expenseOfPlayer] = cost;
    }
  };
  const handleTeamExpenses = (total) => {
    let perPlayer;
    const teamOneResult = data.teamOneResult;
    const teamTwoResult = data.teamTwoResult;

    let loseTeam = null;
    if (teamOneResult.win === WIN) {
      loseTeam = teamTwoResult;
    } 
    if (teamTwoResult.win === WIN) {
      loseTeam = teamOneResult;
    }

    // calculate the expenses in only case finding lose team
    if (loseTeam != null) {
    let dividedNumer = 0;
    if (checkPlayerNotNull(loseTeam.playerOneName)) {
      dividedNumer += 1;
    }
    if (checkPlayerNotNull(loseTeam.playerTwoName)) {
      dividedNumer += 1;
    }
    perPlayer = total / dividedNumer;
    if (checkPlayerNotNull(loseTeam.playerOneName)) {
      loseTeam.expenseOne = perPlayer;
    }
    if (checkPlayerNotNull(loseTeam.playerTwoName)) {
      loseTeam.expenseTwo = perPlayer;
    }
  }
    return {
      teamOneResult,
      teamTwoResult,
    };
  };

  const handleEscPress = useCallback((event) => {
      if (event.key === 'Escape') {
        console.log('[GameDialog] Escape key pressed!');
        onCancel();
      }
    }, []);

  useEffect(() => {
    if (show && data) {
      const parsed = parseBallMap();
      setBallList(parsed);

      let total = 0.0;

      // calculate total
      parsed.forEach((b) => (total += b.cost * b.quantity));

      // autofill team expenses
      const expenseUpdate = handleTeamExpenses(total);
      setFormData((prev) => ({
        ...prev,
        ...expenseUpdate,
        courtResult: data.courtResult,
        gameState: data.state,
      }));

      setTotalCostValue(total);
       // Add event listener when the component mounts
    document.addEventListener('keydown', handleEscPress);

    // Clean up: remove event listener when the component unmounts
    return () => {
      document.removeEventListener('keydown', handleEscPress);
    };
    }
  }, [data,handleEscPress]);

  if (!show || !formData) return null;

  const getTotalCostByBall = (newMap) => {
    let total = 0.0;
    Object.entries(newMap).forEach(([key, qty]) => {
      const match = key.slice(key.lastIndexOf("-") + 1).trim();
      const cost = match ? parseFloat(match) : 0;
      total += cost * qty;
    });
    return total.toLocaleString("it-IT", {
      style: "currency",
      currency: "VND",
    });
  };

  const calculateActualCost = (team) => {
    let updatedFormData = { ...formData };
    if (team === "team1") {
      // Team 1 wins, Team 2 pays
      setTeam1ClassName("team team-win");
      setBtnTeam1ClassName(SUCCESS_BTN_CLASS);
      setTeam2ClassName("team");
      setBtnTeam2ClassName(DEFAULT_BTN_CLASS);

      // 1. Reset Team 1 (Winner) expenses to 0
      updatedFormData.teamOneResult.expenseOne = 0;
      updatedFormData.teamOneResult.expenseTwo = 0;

      // 2. Calculate and set Team 2 (Loser) expenses
      const teamResult = updatedFormData.teamTwoResult;
      let dividedNumer = 0;
      if (checkPlayerNotNull(teamResult.playerOneName)) {
        dividedNumer += 1;
      }
      if (checkPlayerNotNull(teamResult.playerTwoName)) {
        dividedNumer += 1;
      }

      if (dividedNumer > 0) {
        const perPlayer = totalCost / dividedNumer;
        if (checkPlayerNotNull(teamResult.playerOneName)) {
          teamResult.expenseOne = perPlayer;
        }
        if (checkPlayerNotNull(teamResult.playerTwoName)) {
          teamResult.expenseTwo = perPlayer;
        }
      } else {
        // Handle case where team has no players (shouldn't happen, but safe)
        teamResult.expenseOne = 0;
        teamResult.expenseTwo = 0;
      }
    } else if (team === "team2") {
      // Team 2 wins, Team 1 pays
      setTeam1ClassName("team");
      setBtnTeam1ClassName(DEFAULT_BTN_CLASS)
      setTeam2ClassName("team team-win");
      setBtnTeam2ClassName(SUCCESS_BTN_CLASS);

      // 1. Reset Team 2 (Winner) expenses to 0
      updatedFormData.teamTwoResult.expenseOne = 0;
      updatedFormData.teamTwoResult.expenseTwo = 0;

      // 2. Calculate and set Team 1 (Loser) expenses
      const teamResult = updatedFormData.teamOneResult;
      let dividedNumer = 0;
      if (checkPlayerNotNull(teamResult.playerOneName)) {
        dividedNumer += 1;
      }
      if (checkPlayerNotNull(teamResult.playerTwoName)) {
        dividedNumer += 1;
      }

      if (dividedNumer > 0) {
        const perPlayer = totalCost / dividedNumer;
        if (checkPlayerNotNull(teamResult.playerOneName)) {
          teamResult.expenseOne = perPlayer;
        }
        if (checkPlayerNotNull(teamResult.playerTwoName)) {
          teamResult.expenseTwo = perPlayer;
        }
      } else {
        // Handle case where team has no players
        teamResult.expenseOne = 0;
        teamResult.expenseTwo = 0;
      }
    }

    // Update formData state with the new expenses
    setFormData(updatedFormData);
    const actualCost = getActualCost(updatedFormData);
    setActualCostValue(actualCost);
  };

  const getActualCost = (formData) => {
    const total =
      formData.teamOneResult.expenseOne +
      formData.teamOneResult.expenseTwo +
      formData.teamTwoResult.expenseOne +
      formData.teamTwoResult.expenseTwo;

    return total.toLocaleString("it-IT", {
      style: "currency",
      currency: "VND",
    });
  };

  const handleInputChange = (path, value) => {
    const keys = path.split(".");
    const updated = { ...formData };
    let temp = updated;
    for (let i = 0; i < keys.length - 1; i++) temp = temp[keys[i]];
    temp[keys[keys.length - 1]] = value;
    setFormData(updated);
  };

  const handleBallChange = (index, field, value) => {
    if (Number.isNaN(value)) return;

    // update ballList immutably
    const updatedBalls = ballList.map((b, i) =>
      i === index ? { ...b, quantity: value } : b
    );
    setBallList(updatedBalls);

    // rebuild ballResultMap for saving
    const newMap = {};
    updatedBalls.forEach((b) => {
      const key = `shuttleName=${b.shuttleName}, cost=${b.cost}`;
      newMap[key] = b.quantity;
    });
    setFormData({ ...formData, ballResultMap: newMap });

    // re-calculate total cost by ball number
    let total = 0;
    updatedBalls.forEach((b) => {
      total += b.cost * b.quantity;
    });
    setTotalCostValue(total);

    // re-calculate team expense cost and actual cost
    if (winnerTeam) {
      const updatedFormData = calculateActualCost(winnerTeam);
      // Update formData state (including the new ball map and the new expenses)
      setFormData(updatedFormData);
    }
  };

  // Handle winer clicking
  const handleWinClick = (team) => {
    setWinnerTeam(team);

    calculateActualCost(team);
  };

  return (
    <div className="dialog-overlay">
      <div className="dialog-box">
        <h5 className="mb-3 text-center">Thông tin trận cầu</h5>
        {/* Court Name */}
        <div className="d-flex flex-row">
          <div className="align-self-center p-2">
            <label className="form-label fw-bold">
              Tổng tiền : {totalCostStr}
            </label>
          </div>
          <div className="p-2">
            <p></p>
          </div>
          <div className="align-self-center p-2">
            <label className="form-label fw-bold" style={{ color: " #4207e6" }}>
              Tổng thực tế: {actualCostStr}
            </label>
          </div>
        </div>
        <div className="d-flex align-self-start">
          <div className="p-2" style={{ width: "31.2%" }}>
            <label className="form-label fw-bold">Sân :</label>
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
        <div className="d-flex align-self-start">
          <div className="p-2" style={{ width: "35%" }}>
            <label className="form-label fw-bold">
              Số lượng cầu đã sử dụng:
            </label>
          </div>
          <div className="ball-list p-2">
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
          <div className={team1ClassName}>
            <button
              className={team1BtnClassName}
              onClick={() => handleWinClick("team1")}
            >
              <h6 className="text-center">Đội 1:</h6>
            </button>
            {/* Row 1 */}
            <div className="player-row">
              <input
                type="text"
                className="form-control me-2"
                title={formData.teamOneResult?.playerOneName || ""}
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
                title={formData.teamOneResult?.playerTwoName || ""}
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
          <div className={team2ClassName}>
            <button
              className={team2BtnClassName}
              onClick={() => handleWinClick("team2")}
            >
              <h6 className="text-center">Đội 2:</h6>
            </button>
            {/* Row 1 */}
            <div className="player-row">
              <input
                type="text"
                className="form-control me-2"
                title={formData.teamTwoResult?.playerOneName || ""}
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
                title={formData.teamTwoResult?.playerTwoName || ""}
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
            disabled={winnerTeam===null}
            onClick={() => onConfirm(formData)}
          >
            Xác nhận
          </button>
          <button className="btn btn-secondary" onClick={onCancel}>
            Tắt
          </button>
        </div>
      </div>
    </div>
  );
};

export default GameDialog;

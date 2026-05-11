import React, { useEffect, useState, useCallback } from "react";
import api from "../../api";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import Box from "@mui/material/Box";

import "./GameDialog.css";

const WIN = "Win";
const TEAM_ONE = "teamOne";
const TEAM_TWO = "teamTwo";

const GameDialog = ({ show, data, onConfirm, onExit }) => {
  const [formData, setFormData] = useState(null);
  const [totalCost, setTotalCost] = useState(0);
  const [actualCost, setActualCost] = useState(0);

  const [winnerTeam, setWinnerTeam] = useState(null);
  const [winnerEdited, setWinnerEdited] = useState(false);

  const [team1ClassName, setTeam1ClassName] = useState("team");
  const [team2ClassName, setTeam2ClassName] = useState("team");

  const formatCurrency = (n) =>
    Number(n).toLocaleString("it-IT", { style: "currency", currency: "VND" });

  const actualMatch = actualCost === totalCost;

  const parseBallMap = useCallback((ballMap) => {
    if (!ballMap) return [];

    return Object.entries(ballMap).map(([key, qty]) => {
      const match = key.match(/shuttleName=(.*?), cost=(.*?)[)\]]/);
      return {
        shuttleName: match ? match[1] : key,
        cost: match ? parseFloat(match[2]) : 0,
        quantity: qty,
      };
    });
  }, []);

  const checkPlayerNotNull = (playerName) => {
    return playerName != null;
  };

  const handleTeamExpenses = useCallback(
    (total) => {
      const teamOneResult = data.teamOneResult;
      const teamTwoResult = data.teamTwoResult;

      let loseTeam = null;
      if (teamOneResult.win === WIN) {
        loseTeam = teamTwoResult;
      }
      if (teamTwoResult.win === WIN) {
        loseTeam = teamOneResult;
      }

      if (loseTeam != null) {
        let dividedNumer = 0;
        if (checkPlayerNotNull(loseTeam.playerOneName)) {
          dividedNumer += 1;
        }
        if (checkPlayerNotNull(loseTeam.playerTwoName)) {
          dividedNumer += 1;
        }
        const perPlayer = dividedNumer > 0 ? total / dividedNumer : 0;
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
    },
    [data]
  );

  useEffect(() => {
    if (show && data) {
      const parsed = parseBallMap(data.ballResultMap);

      let total = 0.0;
      parsed.forEach((b) => {
        total += b.cost * b.quantity;
      });

      const expenseUpdate = handleTeamExpenses(total);
      setFormData((prev) => ({
        ...prev,
        ...expenseUpdate,
        courtResult: data.courtResult,
        gameState: data.state,
        ballList: parsed,
      }));
      setTotalCost(total);
    }
    setWinnerEdited(false);
  }, [show, data, parseBallMap, handleTeamExpenses]);

  useEffect(() => {
    if (!formData) return;

    const sum =
      Number(formData.teamOneResult?.expenseOne || 0) +
      Number(formData.teamOneResult?.expenseTwo || 0) +
      Number(formData.teamTwoResult?.expenseOne || 0) +
      Number(formData.teamTwoResult?.expenseTwo || 0);

    setActualCost(sum);
  }, [formData]);

  const calculateActualCost = (team) => {
    let updatedFormData = { ...formData };
    if (team === TEAM_ONE) {
      updatedFormData.teamOneResult.win = true;
      updatedFormData.teamOneResult.expenseOne = 0;
      updatedFormData.teamOneResult.expenseTwo = 0;

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
        teamResult.expenseOne = 0;
        teamResult.expenseTwo = 0;
      }
    } else if (team === TEAM_TWO) {
      updatedFormData.teamTwoResult.win = true;
      updatedFormData.teamTwoResult.expenseOne = 0;
      updatedFormData.teamTwoResult.expenseTwo = 0;

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
        teamResult.expenseOne = 0;
        teamResult.expenseTwo = 0;
      }
    }

    setFormData(updatedFormData);
  };

  const handleExpenseChange = (teamKey, memberKey, value) => {
    const nValue = Number(value);
    if (Number.isNaN(nValue)) return;
    const updated = { ...formData };
    if (teamKey === TEAM_ONE) {
      updated.teamOneResult[memberKey] = nValue;
    }
    if (teamKey === TEAM_TWO) {
      updated.teamTwoResult[memberKey] = nValue;
    }
    if (!winnerEdited) setWinnerEdited(true);

    setFormData(updated);
  };

  const handleBallChange = (index, field, value) => {
    const updatedBalls = formData.ballList.map((b, i) =>
      i === index
        ? {
            ...b,
            quantity: value,
          }
        : b
    );

    if (Number.isNaN(value)) {
      setFormData({
        ...formData,
        ballList: updatedBalls,
      });
      return;
    }
    let change = false;
    let ballDTO = {};

    const newMap = {};
    updatedBalls.forEach((b, i) => {
      const key = `shuttleName=${b.shuttleName}, cost=${b.cost}`;
      if (i === index) {
        if (newMap[key] !== value) {
          change = true;
          ballDTO = {
            shuttleName: b.shuttleName,
            shuttleCost: Number(b.cost),
            ballQuantity: value,
          };
        }
        newMap[key] = value;
      } else {
        newMap[key] = b.quantity;
      }
    });

    try {
      if (change) {
        api.post(
          `/court-mana/changeBallQuantity?courtId=${formData.courtResult.courtId}`,
          ballDTO
        );
      }
      setFormData({
        ...formData,
        ballResultMap: newMap,
        ballList: updatedBalls,
      });

      let total = 0;
      updatedBalls.forEach((b) => {
        total += b.cost * b.quantity;
      });

      setTotalCost(total);
    } catch (error) {
      console.log(
        `Error while change shuttle ball in court: ${formData.courtResult.courtId}, quantity change to: ${value}`
      );
    }
  };

  const setWinnerClassName = (winTeam) => {
    if (winTeam === TEAM_ONE) {
      setTeam1ClassName("team team-win");
      setTeam2ClassName("team");
    } else if (winTeam === TEAM_TWO) {
      setTeam1ClassName("team");
      setTeam2ClassName("team team-win");
    }
  };

  const handleWinClick = (team) => {
    setWinnerTeam(team);
    setWinnerClassName(team);
    if (!winnerEdited) {
      calculateActualCost(team);
    }
  };

  if (!show || !formData) return null;

  return (
    <Dialog
      open={show}
      onClose={(event, reason) => {
        if (reason === "backdropClick") return;
        onExit();
      }}
      maxWidth="lg"
      fullWidth
      scroll="paper"
    >
      <DialogTitle align="center">Thông tin trận cầu</DialogTitle>
      <DialogContent dividers>
        <Stack direction="row" spacing={2} flexWrap="wrap" sx={{ mb: 2 }}>
          <Typography fontWeight={700}>Tổng tiền : {formatCurrency(totalCost)}</Typography>
          <Typography fontWeight={700} color={actualMatch ? "primary" : "error"}>
            Tổng thực tế: {formatCurrency(actualCost)}
          </Typography>
        </Stack>

        <Stack direction={{ xs: "column", sm: "row" }} spacing={1} sx={{ mb: 2 }} alignItems={{ sm: "center" }}>
          <Typography fontWeight={700} sx={{ minWidth: 72 }}>
            Sân :
          </Typography>
          <TextField
            size="small"
            fullWidth
            value={formData.courtResult?.courtName || ""}
            disabled
          />
        </Stack>

        <Stack direction={{ xs: "column", md: "row" }} spacing={2} sx={{ mb: 2 }}>
          <Typography fontWeight={700} sx={{ minWidth: { md: 200 } }}>
            Số lượng cầu đã sử dụng:
          </Typography>
          <Box className="ball-list" sx={{ flex: 1 }}>
            {formData.ballList.map((b, index) => (
              <Stack key={index} direction="row" spacing={1} sx={{ mb: 1 }}>
                <TextField size="small" value={b.shuttleName} disabled sx={{ flex: 2 }} />
                <TextField size="small" value={b.cost} disabled sx={{ flex: 1 }} />
                <TextField
                  size="small"
                  type="number"
                  value={b.quantity}
                  onChange={(e) =>
                    handleBallChange(index, "quantity", parseInt(e.target.value, 10))
                  }
                  sx={{ flex: 1 }}
                />
              </Stack>
            ))}
          </Box>
        </Stack>

        <div className="team-container">
          <div className={team1ClassName}>
            <Button
              fullWidth
              variant={winnerTeam === TEAM_ONE ? "contained" : "outlined"}
              color="success"
              onClick={() => handleWinClick(TEAM_ONE)}
              sx={{ mb: 1 }}
            >
              <Typography variant="subtitle2" component="span">
                Đội 1:
              </Typography>
            </Button>
            <div className="player-row">
              <TextField
                size="small"
                title={formData.teamOneResult?.playerOneName || ""}
                disabled
                value={formData.teamOneResult?.playerOneName || ""}
                sx={{ mr: 1, flex: 1 }}
              />
              <TextField
                size="small"
                type="number"
                value={formData.teamOneResult?.expenseOne ?? 0}
                onChange={(e) =>
                  handleExpenseChange(TEAM_ONE, "expenseOne", e.target.value)
                }
                sx={{ width: 120 }}
              />
            </div>

            <div className="player-row">
              <TextField
                size="small"
                title={formData.teamOneResult?.playerTwoName || ""}
                disabled
                value={formData.teamOneResult?.playerTwoName || ""}
                sx={{ mr: 1, flex: 1 }}
              />
              <TextField
                size="small"
                type="number"
                value={formData.teamOneResult?.expenseTwo ?? 0}
                onChange={(e) =>
                  handleExpenseChange(TEAM_ONE, "expenseTwo", e.target.value)
                }
                sx={{ width: 120 }}
              />
            </div>
          </div>

          <div className="divider" />

          <div className={team2ClassName}>
            <Button
              fullWidth
              variant={winnerTeam === TEAM_TWO ? "contained" : "outlined"}
              color="success"
              onClick={() => handleWinClick(TEAM_TWO)}
              sx={{ mb: 1 }}
            >
              <Typography variant="subtitle2" component="span">
                Đội 2:
              </Typography>
            </Button>
            <div className="player-row">
              <TextField
                size="small"
                title={formData.teamTwoResult?.playerOneName || ""}
                disabled
                value={formData.teamTwoResult?.playerOneName || ""}
                sx={{ mr: 1, flex: 1 }}
              />
              <TextField
                size="small"
                type="number"
                value={formData.teamTwoResult?.expenseOne ?? 0}
                onChange={(e) =>
                  handleExpenseChange(TEAM_TWO, "expenseOne", e.target.value)
                }
                sx={{ width: 120 }}
              />
            </div>

            <div className="player-row">
              <TextField
                size="small"
                title={formData.teamTwoResult?.playerTwoName || ""}
                disabled
                value={formData.teamTwoResult?.playerTwoName || ""}
                sx={{ mr: 1, flex: 1 }}
              />
              <TextField
                size="small"
                type="number"
                value={formData.teamTwoResult?.expenseTwo ?? 0}
                onChange={(e) =>
                  handleExpenseChange(TEAM_TWO, "expenseTwo", e.target.value)
                }
                sx={{ width: 120 }}
              />
            </div>
          </div>
        </div>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2, gap: 1 }}>
        <Button variant="contained" color="success" disabled={winnerTeam === null} onClick={() => onConfirm(formData, winnerTeam)}>
          Xác nhận
        </Button>
        <Button variant="outlined" color="inherit" onClick={onExit}>
          Tắt
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default GameDialog;

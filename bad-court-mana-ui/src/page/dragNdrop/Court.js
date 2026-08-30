import { useState, useEffect } from "react";
import Stack from "@mui/material/Stack";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import ButtonGroup from "@mui/material/ButtonGroup";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import AddIcon from "@mui/icons-material/Add";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import DropZone from "./DropZone";

const areaKeys = ["A", "C", "B", "D"];

function formatTime(totalSeconds) {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return ` (${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")})`;
}

export default function Court({
  id,
  name,
  players,
  onDropPlayer,
  occupied,
  isLocked,
  onStart,
  showAddedBallDialog,
  onFinish,
  onCancel,
  onDropService,
  availablePlayers,
  onClickPlayer,
  rentalInfo,
  onRentByTime,
  onFinishRent,
  onCancelRent,
  onUpdateRent,
}) {
  const [hovering, setHovering] = useState(false);
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [remainingSeconds, setRemainingSeconds] = useState(0);

  const isRental = !!rentalInfo;
  const effectivelyLocked = isLocked || isRental;

  // Countdown timer for rental time
  useEffect(() => {
    if (isRental && (rentalInfo.remainingMinutes >= 0 || rentalInfo.remainingSeconds >= 0)) {
      // Store the initial timestamp when the rental info was received
      const initialTimestamp = Date.now();
      // Calculate total remaining seconds from both remainingMinutes and remainingSeconds
      const initialTotalSeconds = (rentalInfo.remainingMinutes * 60) + rentalInfo.remainingSeconds;

      const timerId = setInterval(() => {
        const elapsedSeconds = Math.floor((Date.now() - initialTimestamp) / 1000);
        const currentRemainingSeconds = Math.max(0, initialTotalSeconds - elapsedSeconds);
        setRemainingSeconds(currentRemainingSeconds);

        if (currentRemainingSeconds <= 0) {
          clearInterval(timerId);
        }
      }, 1000);

      // Set initial value immediately
      setRemainingSeconds(initialTotalSeconds);

      return () => clearInterval(timerId);
    } else {
      setRemainingSeconds(0);
    }
  }, [isRental, rentalInfo?.remainingMinutes, rentalInfo?.remainingSeconds]);

  const handleMenuOpen = (e) => {
    e.stopPropagation();
    setMenuAnchor(e.currentTarget);
  };
  const handleMenuClose = () => setMenuAnchor(null);

  return (
    <Box
      sx={{ width: "100%", position: "relative" }}
      onMouseEnter={() => setHovering(true)}
      onMouseLeave={() => setHovering(false)}
    >
      <Stack
        direction="row"
        alignItems="flex-start"
        justifyContent="space-between"
        sx={{ pr: effectivelyLocked ? (isRental ? 26 : 18) : 0 }}
      >
        <Typography variant="body2" sx={{ py: 0.5 }}>
          {name}
          {isRental && remainingSeconds >= 0 && formatTime(remainingSeconds)}
          {isLocked && !isRental && "(Đang diễn ra ...)"}
        </Typography>

        {effectivelyLocked && (
          <Stack direction="row" spacing={1} sx={{ position: "absolute", right: 0, top: 0, zIndex: 2 }}>
            {isRental ? (
              <>
                <Button variant="contained" color="success" size="small" onClick={() => onFinishRent(id)}>
                  Kết thúc
                </Button>
                <Button variant="outlined" color="primary" size="small" onClick={() => onUpdateRent(id)}>
                  Cập nhật
                </Button>
                <Button variant="outlined" color="error" size="small" onClick={() => onCancelRent(id)}>
                  Huỷ
                </Button>
              </>
            ) : (
              <>
                <Button variant="contained" color="success" size="small" onClick={() => onFinish(id)}>
                  Kết thúc
                </Button>
                <Button variant="outlined" color="error" size="small" onClick={() => onCancel(id)}>
                  Huỷ
                </Button>
              </>
            )}
          </Stack>
        )}
      </Stack>
      <Box>
        <Box
          sx={{
            backgroundColor: "white",
            backgroundImage: 'url("bad-court2.jpg")',
            backgroundSize: "cover",
            backgroundRepeat: "no-repeat",
            backgroundPosition: "center",
            height: "250px",
            display: "grid",
            gridTemplateColumns: "repeat(2, 1fr)",
            gridTemplateRows: "repeat(2, 1fr)",
            gap: "5px",
            p: "10px",
            position: "relative",
            transition: "all 2s ease",
          }}
        >
          {areaKeys.map((areaKey) => (
            <DropZone
              key={areaKey}
              courtId={id}
              areaKey={areaKey}
              player={players[areaKey]}
              onDropPlayer={onDropPlayer}
              occupied={occupied}
              isLocked={effectivelyLocked}
              onDropService={onDropService}
              availablePlayers={availablePlayers}
              onClickPlayer={onClickPlayer}
            />
          ))}
          {!effectivelyLocked && hovering && (
            <ButtonGroup
              variant="contained"
              sx={{
                position: "absolute",
                top: "50%",
                left: "50%",
                transform: "translate(-50%, -50%)",
                zIndex: 1,
              }}
            >
              <Button onClick={() => onStart(id)}>Bắt đầu</Button>
              <Button size="small" onClick={handleMenuOpen} sx={{ px: 0.5, minWidth: 0 }}>
                <ArrowDropDownIcon />
              </Button>
            </ButtonGroup>
          )}
          {isLocked && !isRental && hovering && (
            <Button
              onClick={() => showAddedBallDialog(id)}
              title="Thêm cầu"
              sx={{
                position: "absolute",
                top: "50%",
                left: "50%",
                transform: "translate(-50%, -50%)",
                zIndex: 1,
                bgcolor: "white",
                color: "primary.main",
                minWidth: 0,
                px: 1,
              }}
            >
              <Stack direction="row" alignItems="center" spacing={0.5}>
                <AddIcon fontSize="small" />
                <Box
                  component="img"
                  src="icon.png"
                  alt=""
                  sx={{ width: 25, height: "auto" }}
                />
              </Stack>
            </Button>
          )}
        </Box>
      </Box>

      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={handleMenuClose}>
        <MenuItem
          onClick={() => {
            onRentByTime(id);
            handleMenuClose();
          }}
        >
          Thuê theo giờ
        </MenuItem>
      </Menu>
    </Box>
  );
}

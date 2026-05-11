import { useState } from "react";
import Stack from "@mui/material/Stack";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import AddIcon from "@mui/icons-material/Add";
import DropZone from "./DropZone";

const areaKeys = ["A", "C", "B", "D"];

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
}) {
  const [hovering, setHovering] = useState(false);

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
        sx={{ pr: isLocked ? 18 : 0 }}
      >
        <Typography variant="body2" sx={{ py: 0.5 }}>
          {name} {isLocked && "(Đang diễn ra ...)"}{" "}
        </Typography>

        {isLocked && (
          <Stack direction="row" spacing={1} sx={{ position: "absolute", right: 0, top: 0, zIndex: 2 }}>
            <Button variant="contained" color="success" size="small" onClick={() => onFinish(id)}>
              Kết thúc
            </Button>
            <Button variant="outlined" color="error" size="small" onClick={() => onCancel(id)}>
              Huỷ
            </Button>
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
              isLocked={isLocked}
              onDropService={onDropService}
            />
          ))}
          {!isLocked && hovering && (
            <Button
              variant="contained"
              onClick={() => onStart(id)}
              sx={{
                position: "absolute",
                top: "50%",
                left: "50%",
                transform: "translate(-50%, -50%)",
                zIndex: 1,
              }}
            >
              Bắt đầu
            </Button>
          )}
          {isLocked && hovering && (
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
    </Box>
  );
}

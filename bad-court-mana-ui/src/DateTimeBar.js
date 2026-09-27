import React, { useState, useEffect, useContext } from "react";
import { useLocation } from "react-router-dom";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import api from "./api/index";
import { emitApiError } from "./api/errorBus";
import PayConfirm from "./page/dialog/PayConfirm";
import { AuthContext } from "./context/AuthContext";
import { formatVNDateTime } from "./page/DateTimeUtils";

function DateTimeBar() {
  const [currentDateTime, setCurrentDateTime] = useState(new Date());
  const [ending, setEnding] = useState(false);
  const [show, setShow] = useState(false);
  const [data, setData] = useState();
  const { logout } = useContext(AuthContext);

  useEffect(() => {
    const endedSessionTitle = `Sau kết thúc phiên làm việc:\n 
    1) Tất cả trận cầu đang diễn ra trên sân sẽ kết thúc.\n
    2) Tất cả người chơi sẽ được xoá khỏi phiên làm việc.\n
    Bạn có chắc Kết thúc phiên làm việc ?`;
    setData({ title: endedSessionTitle });
    const timerId = setInterval(() => {
      setCurrentDateTime(new Date());
    }, 1000);

    return () => clearInterval(timerId);
  }, []);

  const location = useLocation();
  const isHomePage = location.pathname === "/home";

  const onEndSession = () => {
    setShow(true);
  };

  const handleEndSession = async () => {
    setEnding(true);
    try {
      const closedSessionResp = await api.post(`/session/deleteSession`);
      if (!closedSessionResp || closedSessionResp.status !== 200) {
        emitApiError(`Có lỗi khi kết thúc phiên làm việc. ${closedSessionResp?.data?.message || ""} Thử lại.`);
        return;
      }
      alert("Kết thúc phiên làm việc thành công.");
      logout();
    } catch (error) {
      console.log(`Unexpected error: ${error}`);
    } finally {
      setEnding(false);
      setShow(false);
    }
  };

  const handleExit = () => {
    setShow(false);
  };

  return (
    <Box
      sx={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        flexWrap: "wrap",
        gap: 1,
        px: 2,
        py: 1,
        mb: 1,
        borderBottom: 1,
        borderColor: "divider",
      }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        {isHomePage && (
          <Button
            variant="outlined"
            color="error"
            size="small"
            onClick={onEndSession}
            disabled={ending}
            sx={{ width: "fit-content", flexShrink: 0 }}
          >
            {ending ? "Ending..." : "Đóng cửa"}
          </Button>
        )}
      </Box>
      <Typography variant="body2" color="text.secondary" sx={{ textAlign: { xs: "left", sm: "right" }, flex: 1 }}>
        {formatVNDateTime(currentDateTime)}
      </Typography>
      <PayConfirm
        show={show}
        data={data}
        onConfirm={handleEndSession}
        onExit={handleExit}
      />
    </Box>
  );
}

export default DateTimeBar;

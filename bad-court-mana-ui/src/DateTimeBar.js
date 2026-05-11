import React, { useState, useEffect, useContext } from "react";
import { useLocation } from "react-router-dom";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import api from "./api/index";
import PayConfirm from "./page/dialog/PayConfirm";
import { AuthContext } from "./context/AuthContext";

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

  const formatVietnameseDateTime = (date) => {
    const weekday = date.toLocaleDateString("vi-VN", { weekday: "long" });
    const day = date.getDate().toString().padStart(2, "0");
    const month = (date.getMonth() + 1).toString().padStart(2, "0");
    const year = date.getFullYear();
    const time = date.toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    });

    return `${weekday}, ngày ${day}, tháng ${month}, năm ${year} - ${time}`;
  };

  const onEndSession = () => {
    setShow(true);
  };

  const handleEndSession = async () => {
    setEnding(true);
    try {
      const closedSessionResp = await api.post(`/session/deleteSession`);
      if (closedSessionResp.status !== 200) {
        const errorMessage = `Có lỗi khi kết thúc phiên làm việc. ${closedSessionResp.data.message}.\n Thử lại.`;
        alert(errorMessage);
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
        {formatVietnameseDateTime(currentDateTime)}
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

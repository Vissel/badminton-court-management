import React, { useState, useEffect, useContext } from "react";
import { useLocation } from "react-router-dom";
import "./DateTimeBar.css";
import api from "./api/index";
import PayConfirm from "./page/dialog/PayConfirm";
import {AuthContext} from "./context/AuthContext";

function DateTimeBar() {
  const [currentDateTime, setCurrentDateTime] = useState(new Date());
  const [ending, setEnding] = useState(false); // prevent double click
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

  // =========================
  // END SESSION HANDLER
  // =========================
  const onEndSession = () => {
    setShow(true);
  };
  const handleEndSession = async () => {
    console.log("Clicking on End session.");
    try {
      const closedSessionResp = await api.post(`/session/deleteSession`);
      if (closedSessionResp.status !== 200) {
        const errorMessage = `Có lỗi khi kết thúc phiên làm việc. ${closedSessionResp.data.message}.\n Thử lại.`;

        alert(errorMessage);
        console.log(errorMessage);
        return;
      }
      alert("Kết thúc phiên làm việc thành công.");
      logout();
    } catch (error) {
      console.log(`Unexpected error: ${error}`);
    } finally {
      setShow(false);
    }
  };
  const handleExit = () => {
    console.log("Clicking on Exit.");
    setShow(false);
  };

  return (
    <div className="d-flex justify-content-between align-items-center px-3 date-time-bar">
      {/* End Session Button */}
      <div class="d-grid gap-2 d-md-flex d-flex bd-highlight align-items-center">
        {isHomePage && (
          <button
            className="btn btn-outline-danger btn-sm w-auto"
            style={{ width: "fit-content" }}
            onClick={onEndSession}
            disabled={ending}
          >
            {ending ? "Ending..." : "Đóng cửa"}
          </button>
        )}
      </div>
      {/* Date Time */}
      <div>
        <span>{formatVietnameseDateTime(currentDateTime)}</span>
      </div>
      <PayConfirm
        show={show}
        data={data}
        onConfirm={handleEndSession}
        onExit={handleExit}
      />
    </div>
  );
}

export default DateTimeBar;

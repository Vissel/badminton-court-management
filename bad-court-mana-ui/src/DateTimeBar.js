import React, { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import "./DateTimeBar.css";
import api from "./api/index";
import PayConfirm from "./page/dialog/PayConfirm";

function DateTimeBar() {
  const [currentDateTime, setCurrentDateTime] = useState(new Date());
  const [ending, setEnding] = useState(false); // prevent double click
  const [show, setShow] = useState(false);
  const [data, setData] = useState();

  useEffect(() => {
    setData({ title: "Bạn có chắc Kết thúc phiên làm việc ?" });
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
      await api.post(`/session/deleteSession`);
    } catch (error) {
      console.log(
        `Error while performing end session. Error:name=[${error.name}]; message=[${error.message}]`
      );
      alert(`Có lỗi khi kết thúc phiên làm việc. Thử lại.`);
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

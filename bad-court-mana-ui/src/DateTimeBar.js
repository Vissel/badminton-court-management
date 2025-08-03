import React, { useState, useEffect } from 'react';
import './DateTimeBar.css'; // Make sure this CSS file is in the same directory

function DateTimeBar() {
  const [currentDateTime, setCurrentDateTime] = useState(new Date());

  useEffect(() => {
    // Set up an interval to update the time every second
    const timerId = setInterval(() => {
      setCurrentDateTime(new Date());
    }, 1000); // Update every 1000 milliseconds (1 second)

    // Clean up the interval when the component unmounts
    return () => {
      clearInterval(timerId);
    };
  }, []); // The empty dependency array ensures this effect runs only once on mount

  // Helper function to format the date and time in Vietnamese
  const formatVietnameseDateTime = (date) => {
    // Options for date formatting
    const dateOptions = {
      weekday: 'long',  // e.g., Thứ Hai, Thứ Ba
      day: '2-digit',   // e.g., 01, 15
      month: '2-digit', // e.g., 01, 12
      year: 'numeric',  // e.g., 2023
    };

    // Options for time formatting (24-hour)
    const timeOptions = {
      hour: '2-digit',    // e.g., 08, 17
      minute: '2-digit',  // e.g., 05, 30
      second: '2-digit',  // e.g., 01, 45
      hour12: false,      // Use 24-hour format
    };

    // Format the date part
    const formattedDate = date.toLocaleDateString('vi-VN', dateOptions);

    // Format the time part
    const formattedTime = date.toLocaleTimeString('vi-VN', timeOptions);

    // Manually reconstruct to match "Thứ 5, ngày dd, tháng mm, năm yyyy - HH:MM:ss"
    // toLocaleDateString('vi-VN') typically gives "Thứ Hai, dd/mm/yyyy"
    // We need to re-order and add "ngày", "tháng", "năm" prefixes.

    const weekday = date.toLocaleDateString('vi-VN', { weekday: 'long' });
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0'); // Month is 0-indexed
    const year = date.getFullYear();

    return `${weekday}, ngày ${day}, tháng ${month}, năm ${year} - ${formattedTime}`;
  };

  return (
    <div className="date-time-bar">
      <span>{formatVietnameseDateTime(currentDateTime)}</span>
    </div>
  );
}

export default DateTimeBar;
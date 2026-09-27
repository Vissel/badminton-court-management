export const parseServerDateTime = (dateTime) => {
  if (!dateTime) return null;
  if (dateTime instanceof Date) {
    return isNaN(dateTime.getTime()) ? null : dateTime;
  }
  if (typeof dateTime !== "string") return null;

  if (dateTime.includes("T")) {
    const parsed = new Date(dateTime);
    return isNaN(parsed.getTime()) ? null : parsed;
  }

  // Server display format: "yyyy-MM-dd H:mm:ss" is already the wall-clock time
  // the server wants to show (it applies the UTC+7 offset before sending).
  // Parse it as a UTC Date with those exact components.
  const [datePart, timePart] = dateTime.split(" ");
  if (!datePart || !timePart) return null;
  const [y, mo, d] = datePart.split("-").map(Number);
  const [h, m, s] = timePart.split(":").map(Number);
  if ([y, mo, d, h, m, s].some((n) => Number.isNaN(n))) return null;
  const parsed = new Date(Date.UTC(y, mo - 1, d, h, m, s));
  return isNaN(parsed.getTime()) ? null : parsed;
};

// Converts back to the server's display format "yyyy-MM-dd H:mm:ss"
// (hour not zero-padded, UTC wall-clock). Endpoints like /debit/pay
// match debits by comparing this string literally.
export const toServerDateTimeString = (input) => {
  const date = parseServerDateTime(input);
  if (!date) return input ? String(input) : "";
  const pad2 = (n) => String(n).padStart(2, "0");
  return `${date.getUTCFullYear()}-${pad2(date.getUTCMonth() + 1)}-${pad2(date.getUTCDate())} ${date.getUTCHours()}:${pad2(date.getUTCMinutes())}:${pad2(date.getUTCSeconds())}`;
};

const buildOptions = (baseOptions, timeZone) => ({
  ...baseOptions,
  ...(timeZone && { timeZone }),
});

export const formatVNDateTime = (input) => {
  let date;
  let timeZone;
  if (input instanceof Date) {
    // Local system time, e.g. the clock in the top bar.
    date = input;
    timeZone = undefined;
  } else if (typeof input === "string") {
    // Server date strings are already shifted to UTC+7 wall-clock time;
    // format them as-is in UTC so we do not double-shift.
    date = parseServerDateTime(input);
    timeZone = "UTC";
  } else {
    return input ? String(input) : "";
  }
  if (!date) return input ? String(input) : "";

  const weekday = date.toLocaleDateString(
    "vi-VN",
    buildOptions({ weekday: "long" }, timeZone)
  );
  const day = date.toLocaleDateString(
    "vi-VN",
    buildOptions({ day: "2-digit" }, timeZone)
  );
  const month = date.toLocaleDateString(
    "vi-VN",
    buildOptions({ month: "2-digit" }, timeZone)
  );
  const year = date.toLocaleDateString(
    "vi-VN",
    buildOptions({ year: "numeric" }, timeZone)
  );
  const time = date.toLocaleTimeString(
    "vi-VN",
    buildOptions(
      { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false },
      timeZone
    )
  );

  return `${weekday}, ngày ${day}, tháng ${month}, năm ${year} - ${time}`;
};

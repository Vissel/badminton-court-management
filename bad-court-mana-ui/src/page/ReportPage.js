import React, { useState, useEffect } from "react";

import api, { backendHost } from "../api/index";

// -------- dummy data for UI testing --------
const DUMMY_REPORTS = Array.from({ length: 23 }).map((_, i) => ({
  id: i + 1,
  date: `2025-01-${String((i % 28) + 1).padStart(2, "0")}`,
  fromTo: i % 2 === 0 ? "08:00 - 10:00" : "18:00 - 20:00",
  total: 100000 + i * 5000,
}));
// ------------------------------------------

export default function ReportPage() {
  const [reports, setReports] = useState([]);
  const [originalReports, setOriginalReports] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10); // default 10 rows
  const [totalPages, setTotalPages] = useState(0);
  const [numberRows, setNumberRows] = useState(0);
  const [totalRows, setTotalRows] = useState(0);
  const [loading, setLoading] = useState(false);

  const [sortField, setSortField] = useState("date");
  const [sortDir, setSortDir] = useState("ASC");
  const [month, setMonth] = useState("");
  const [monthOptions, setMonthOptions] = useState([]);

  // -------- debounce search --------
  useEffect(() => {
    const timer = setTimeout(() => {
      setCurrentPage(1);
      setDebouncedSearch(searchText);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchText]);

  // -------- call API / dummy pagination --------
  useEffect(() => {
    fetchReports({
      pagination: {
        current: 0,
        pageSize: 10,
      },
    });

    api
      .get(`/api/v1/manager/getMonthYear`)
      .then((res) => {
        setMonthOptions(res.data);
      })
      .catch((exception) => {
        console.log(`Cannot get month year: ${exception.error}`);
      });
  }, []);

  const fetchReports = async (payload) => {
    try {
      setLoading(true);

      const response = await api.post("/api/v1/manager/reportList", payload);
      const pageResponse = response.data.data;
      const pagination = pageResponse.pagination;
      const list = pageResponse.list;

      // setCurrentPage(pagination.current);
      setPageSize(pagination.pageSize);
      setTotalPages(pagination.totalPage);

      setReports(list);
      setOriginalReports(list);
      setNumberRows(list.length);
      setTotalRows(pageResponse.total);
    } catch (error) {
      console.error("Failed to fetch reports", error);
    } finally {
      setLoading(false);
    }
  };

  const toggleSort = (field) => {
    if (sortField === field) setSortDir(sortDir === "ASC" ? "DESC" : "ASC");
    else {
      setSortField(field);
      setSortDir("ASC");
    }
  };
  const handlePageSizeChange = (size) => {
    fetchReports({
      pagination: {
        current: 0,
        pageSize: size,
      },
    });
    setPageSize(size);
  };
  const handlePageChange = (current) => {
    fetchReports({
      pagination: {
        current: current - 1,
        pageSize: pageSize,
      },
    });
    setCurrentPage(current);
  };

  const handleExport = async (sessionId) => {
    // need handle loading
    try {
      const response = await api.get(
        `/api/v1/manager/reportExport/${sessionId}`,
        { responseType: "blob" }
      );

      // Create file download
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const disposition = response.headers["content-disposition"] || "";
      const match = disposition.match(/filename="?([^"]+)"?/);
      const fileName = match ? match[1] : `report_${sessionId}.xlsx`;

      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();

      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Failed to export report", error);
      alert("Export failed. Please try again.");
    }
  };

  const handleExportReport = async () => {
    try {
      const sessionIds = reports.map((r) => r.sessionId);

      const response = await api.post(`/api/v1/manager/reportToken`, {
        sessionIds: sessionIds,
      });

      window.location.href = `${backendHost}/api/v1/manager/stream/reportExportList/${response.data.reportToken}`;
    } catch (error) {
      console.error("Failed to export report", error);
      alert("Export failed. Please try again.");
    }
  };

  const handleMonthChange = (month) => {
    fetchReports({
      yearMonth: month,
      pagination: {
        current: currentPage - 1,
        pageSize: pageSize,
      },
    });
    setMonth(month);
  };

  const handleFilter = (text) => {
    setSearchText(text);
    const keyword = text.trim().toLowerCase();

    if (!keyword) {
      setReports(originalReports);
      setNumberRows(originalReports.length);
      return;
    }

    const filtered = originalReports.filter((row, index) => {
      const searchString = `
      ${(currentPage - 1) * pageSize + index + 1}
      ${row.date?.viDateString ?? ""}
      ${row.during ?? ""}
      ${row.grossRevenueFormat ?? ""}
    `.toLowerCase();

      return searchString.includes(keyword);
    });

    setReports(filtered);
    setNumberRows(filtered.length);
  };
  return (
    <div className="container mt-4">
      <h3 className="mb-3">Trang quản lý</h3>

      {/* Search + Month filter + Total rows */}
      <div className="row mb-4 align-items-center">
        <div className="col-md-3">
          <input
            type="text"
            className="form-control"
            placeholder="Tìm kiếm bằng ngày, thời gian, số tổng,..."
            value={searchText}
            onChange={(e) => handleFilter(e.target.value)}
          />
        </div>
        <div className="col-md-3">
          <select
            className="form-select"
            value={month}
            onChange={(e) => handleMonthChange(e.target.value)}
          >
            {monthOptions.map((m) => (
              <option key={m.value} value={m.value}>
                {m.label}
              </option>
            ))}
          </select>
        </div>
        <div className="col-md-3 text-muted">
          Tổng hiển thị: <strong>{numberRows}</strong>
        </div>
        <div className="col-md-3 text-end">
          <button
            className="btn btn-success btn-sm"
            onClick={() => handleExportReport()}
          >
            Xuất Excel tất cả hàng hiển thị
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="table-responsive">
        <table className="table table-bordered table-hover align-middle">
          <thead className="table-light">
            <tr>
              <th style={{ width: "60px" }}>STT</th>
              <th
                onClick={() => toggleSort("date")}
                style={{ cursor: "pointer" }}
              >
                Ngày{" "}
                {sortField === "date" ? (sortDir === "ASC" ? "▲" : "▼") : ""}
              </th>
              <th
                onClick={() => toggleSort("fromTo")}
                style={{ cursor: "pointer" }}
              >
                Khoảng thời gian{" "}
                {sortField === "fromTo" ? (sortDir === "ASC" ? "▲" : "▼") : ""}
              </th>
              <th
                onClick={() => toggleSort("total")}
                style={{ cursor: "pointer" }}
              >
                Tồng tiền đã thanh toán{" "}
                {sortField === "total" ? (sortDir === "ASC" ? "▲" : "▼") : ""}
              </th>
              <th style={{ width: "120px" }}>Xuất</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={5} className="text-center">
                  Loading...
                </td>
              </tr>
            )}
            {!loading && reports.length === 0 && (
              <tr>
                <td colSpan={5} className="text-center text-muted">
                  No data found
                </td>
              </tr>
            )}
            {!loading &&
              reports.map((row, index) => (
                <tr key={row.id}>
                  <td title={`index:${index}`}>
                    {(currentPage - 1) * pageSize + index + 1}
                  </td>
                  <td>{row.date.viDateString}</td>
                  <td>{row.during}</td>
                  <td>{row.grossRevenueFormat}</td>
                  <td className="text-center">
                    <button
                      className="btn btn-success btn-sm"
                      onClick={() => handleExport(row.sessionId)}
                    >
                      Excel
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {/* Pagination + page size */}
      <div className="d-flex justify-content-between align-items-center">
        <div className="p-2">
          <select
            className="form-select w-auto"
            value={pageSize}
            onChange={(e) => handlePageSizeChange(Number(e.target.value))}
          >
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={30}>30</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
        </div>
        <div className="p-2 text-muted">
          Tổng số phiên làm việc: <strong>{totalRows}</strong>
        </div>

        <div className="p-2">
          <ul className="pagination mb-0">
            <li className={`page-item ${currentPage === 1 ? "disabled" : ""}`}>
              <button
                className="page-link"
                onClick={() => handlePageChange(currentPage - 1)}
              >
                <i className="bi bi-chevron-left"></i>
              </button>
            </li>
            {[...Array(totalPages)].map((_, i) => (
              <li
                key={i}
                className={`page-item ${currentPage === i + 1 ? "active" : ""}`}
              >
                <button
                  className="page-link"
                  onClick={() => handlePageChange(i + 1)}
                >
                  {i + 1}
                </button>
              </li>
            ))}
            <li
              className={`page-item ${
                currentPage === totalPages ? "disabled" : ""
              }`}
            >
              <button
                className="page-link"
                onClick={() => handlePageChange(currentPage + 1)}
              >
                <i className="bi bi-chevron-right"></i>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}

import React, { useState,  useEffect } from "react";

import axios from "axios";

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
  const [searchText, setSearchText] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10); // default 10 rows
  const [totalPages, setTotalPages] = useState(0);
  const [totalRows, setTotalRows] = useState(0);
  const [loading, setLoading] = useState(false);

  const [sortField, setSortField] = useState("date");
  const [sortDir, setSortDir] = useState("ASC");
  const [month, setMonth] = useState("");

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
    fetchReports(currentPage, debouncedSearch, pageSize, sortField, sortDir, month);
  }, [currentPage, debouncedSearch, pageSize, sortField, sortDir, month]);

  const fetchReports = async (page, keyword, size, sortBy, direction, monthFilter) => {
    try {
      setLoading(true);

      let data = [...DUMMY_REPORTS];

      if (monthFilter) {
        data = data.filter((r) => r.date.startsWith(monthFilter));
      }

      if (keyword) {
        const lower = keyword.toLowerCase();
        data = data.filter(
          (r) =>
            r.date.toLowerCase().includes(lower) ||
            r.fromTo.toLowerCase().includes(lower) ||
            r.total.toString().includes(lower)
        );
      }

      data.sort((a, b) => {
        const v1 = a[sortBy];
        const v2 = b[sortBy];
        if (v1 < v2) return direction === "ASC" ? -1 : 1;
        if (v1 > v2) return direction === "ASC" ? 1 : -1;
        return 0;
      });

      setTotalRows(data.length);
      setTotalPages(Math.ceil(data.length / size));

      const start = (page - 1) * size;
      const end = start + size;
      setReports(data.slice(start, end));

      // ===== API placeholder =====
      // await axios.get("/api/reports", { params: { page: page - 1, size, search: keyword, sort: `${sortBy},${direction}`, month: monthFilter }});
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

  const handlePageChange = (page) => {
    if (page >= 1 && page <= totalPages) setCurrentPage(page);
  };

  return (
    <div className="container mt-4">
      <h3 className="mb-3">Manager</h3>

      {/* Search + Month filter + Total rows */}
      <div className="row mb-3 align-items-center">
        <div className="col-md-4">
          <input
            type="text"
            className="form-control"
            placeholder="Search by date, time, total..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
        </div>
        <div className="col-md-3">
          <select className="form-select" value={month} onChange={(e) => setMonth(e.target.value)}>
            <option value="">All months</option>
            <option value="2025-01">Jan 2025</option>
          </select>
        </div>
        <div className="col-md-3 text-end text-muted">
          Total: <strong>{totalRows}</strong> rows
        </div>
      </div>

      {/* Table */}
      <div className="table-responsive">
        <table className="table table-bordered table-hover align-middle">
          <thead className="table-light">
            <tr>
              <th style={{ width: "60px" }}>No</th>
              <th onClick={() => toggleSort("date")} style={{ cursor: "pointer" }}>
                Date {sortField === "date" ? (sortDir === "ASC" ? "▲" : "▼") : ""}
              </th>
              <th onClick={() => toggleSort("fromTo")} style={{ cursor: "pointer" }}>
                From - To {sortField === "fromTo" ? (sortDir === "ASC" ? "▲" : "▼") : ""}
              </th>
              <th onClick={() => toggleSort("total")} style={{ cursor: "pointer" }}>
                Total {sortField === "total" ? (sortDir === "ASC" ? "▲" : "▼") : ""}
              </th>
              <th style={{ width: "120px" }}>Export</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan={5} className="text-center">Loading...</td></tr>
            )}
            {!loading && reports.length === 0 && (
              <tr><td colSpan={5} className="text-center text-muted">No data found</td></tr>
            )}
            {!loading && reports.map((row, index) => (
              <tr key={row.id}>
                <td>{(currentPage - 1) * pageSize + index + 1}</td>
                <td>{row.date}</td>
                <td>{row.fromTo}</td>
                <td>{row.total.toLocaleString()}</td>
                <td className="text-center"><button className="btn btn-success btn-sm">Export</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination + page size */}
      {totalPages > 1 && (
        <div className="d-flex justify-content-between align-items-center mt-3">
          <select className="form-select w-auto" value={pageSize} onChange={(e) => { setCurrentPage(1); setPageSize(Number(e.target.value)); }}>
            <option value={5}>5</option>
            <option value={10}>10</option>
            <option value={20}>20</option>
          </select>

          <ul className="pagination mb-0">
            <li className={`page-item ${currentPage === 1 ? "disabled" : ""}`}>
              <button className="page-link" onClick={() => handlePageChange(currentPage - 1)}>
                <i className="bi bi-chevron-left"></i>
              </button>
            </li>
            {[...Array(totalPages)].map((_, i) => (
              <li key={i} className={`page-item ${currentPage === i + 1 ? "active" : ""}`}>
                <button className="page-link" onClick={() => handlePageChange(i + 1)}>{i + 1}</button>
              </li>
            ))}
            <li className={`page-item ${currentPage === totalPages ? "disabled" : ""}`}>
              <button className="page-link" onClick={() => handlePageChange(currentPage + 1)}>
                <i className="bi bi-chevron-right"></i>
              </button>
            </li>
          </ul>
        </div>
      )}
    </div>
  );
}

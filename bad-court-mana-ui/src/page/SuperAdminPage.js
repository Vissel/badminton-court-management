import { useState, useEffect, useRef, useContext, useCallback } from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import MenuItem from "@mui/material/MenuItem";
import Chip from "@mui/material/Chip";
import Checkbox from "@mui/material/Checkbox";
import InputAdornment from "@mui/material/InputAdornment";
import Paper from "@mui/material/Paper";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import SearchIcon from "@mui/icons-material/Search";
import api from "../api/index";
import { emitApiError } from "../api/errorBus";
import { encryptPassword, withKeyRetry } from "../api/rsaCrypto";
import { AuthContext } from "../context/AuthContext";
import { getUsername } from "../api/tokenStore";
import { normalizeSearchText } from "./TextUtils";
import DraggableResizablePaper, { DIALOG_DRAG_HANDLE } from "./dialog/DraggableResizablePaper";

const RESET_TOKEN_TTL = 3 * 60 * 1000;

function SuperAdminPage() {
  const { hasRole, vnRoleMapping } = useContext(AuthContext);
  const isRoot = hasRole("Root");
  const myUsername = getUsername();

  const [users, setUsers] = useState([]);
  const [userSearch, setUserSearch] = useState("");
  const [reg, setReg] = useState({
    userName: "",
    password: "",
    repeatPassword: "",
    role: isRoot ? "Administrator" : "Coordinator",
  });
  const [forgot, setForgot] = useState({ userName: "" });
  const [resetToken, setResetToken] = useState(null);
  const [resetUserName, setResetUserName] = useState("");
  const [resetPass, setResetPass] = useState({ newPass: "", repeatNewPass: "" });
  const [secondsLeft, setSecondsLeft] = useState(0);
  // Removal is confirmed first: the account is hidden, not erased. The same
  // dialog serves the row button (one account) and the multi-select (many).
  const [deleteTargets, setDeleteTargets] = useState([]);
  const [deleting, setDeleting] = useState(false);
  // Rows ticked in the table; only root can tick, and only removable rows.
  const [selectedIds, setSelectedIds] = useState(() => new Set());
  const [snackbar, setSnackbar] = useState({ open: false, message: "" });
  const timerRef = useRef(null);
  const expireRef = useRef(null);

  // Root is provisioned outside the app and is never offered as a choice.
  const roleOptions = isRoot
    ? ["Administrator", "Coordinator"]
    : ["Coordinator"];

  const primaryRole = (user) => user.roles?.[0] || "Coordinator";

  // Administrators available as managers when root assigns a coordinator.
  const administrators = users.filter((user) => (user.roles || []).includes("Administrator"));

  const canManage = (user) =>
    isRoot || (user.roles || []).every((role) => role === "Coordinator");

  // Vietnamese label for a role key; falls back to the raw key when missing.
  const vnLabel = (role) => vnRoleMapping?.[role] ?? role;

  // Free-text search over both the login name and the role, whether the user
  // types the raw key ("Administrator") or its Vietnamese label ("Quản trị viên").
  const searchTerm = normalizeSearchText(userSearch);
  const visibleUsers = searchTerm
    ? users.filter((user) => {
      const nameMatches = normalizeSearchText(user.username).includes(searchTerm);
      const roleMatches = (user.roles || []).some(
        (role) =>
          normalizeSearchText(role).includes(searchTerm) ||
          normalizeSearchText(vnLabel(role)).includes(searchTerm)
      );
      return nameMatches || roleMatches;
    })
    : users;

  // Root alone removes accounts, never itself and never a provisioned root one.
  const canRemove = (user) =>
    isRoot && primaryRole(user) !== "Root" && user.username !== myUsername;
  const selectableUsers = visibleUsers.filter(canRemove);
  const selectedUsers = users.filter((user) => selectedIds.has(user.userId));
  const allSelected =
    selectableUsers.length > 0 &&
    selectableUsers.every((user) => selectedIds.has(user.userId));
  const someSelected = selectableUsers.some((user) => selectedIds.has(user.userId));

  const toggleUserSelection = (userId) => {
    setSelectedIds((ids) => {
      const next = new Set(ids);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });
  };

  // Header checkbox ticks (or clears) every removable row currently listed.
  const toggleAllSelection = () => {
    setSelectedIds((ids) => {
      const next = new Set(ids);
      const clear = selectableUsers.every((user) => next.has(user.userId));
      selectableUsers.forEach((user) => {
        if (clear) next.delete(user.userId);
        else next.add(user.userId);
      });
      return next;
    });
  };

  const loadUsers = useCallback(async () => {
    try {
      const res = await api.get("/api/v1/users");
      if (res?.data?.success) {
        const next = res.data.data || [];
        setUsers(next);
        // Accounts that left the list must not stay ticked.
        setSelectedIds(
          (ids) => new Set([...ids].filter((id) => next.some((user) => user.userId === id)))
        );
      }
    } catch (err) {
      console.error(err);
    }
  }, []);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const clearResetState = () => {
    setResetToken(null);
    setResetUserName("");
    setResetPass({ newPass: "", repeatNewPass: "" });
    setSecondsLeft(0);
    clearInterval(timerRef.current);
    clearTimeout(expireRef.current);
  };

  useEffect(
    () => () => {
      clearInterval(timerRef.current);
      clearTimeout(expireRef.current);
    },
    []
  );

  const handleRegister = async () => {
    if (!reg.userName || !reg.password || !reg.repeatPassword) {
      emitApiError("Vui lòng điền đầy đủ thông tin.");
      return;
    }
    if (reg.password !== reg.repeatPassword) {
      emitApiError("Mật khẩu không khớp.");
      return;
    }
    try {
      const res = await withKeyRetry(async (cfg) =>
        api.post("/api/v1/users", {
          userName: reg.userName,
          password: await encryptPassword(reg.password),
          role: reg.role,
        }, cfg)
      );
      if (res?.data?.success) {
        setSnackbar({ open: true, message: "Đăng ký người dùng thành công!" });
        setReg({ userName: "", password: "", repeatPassword: "", role: roleOptions[0] });
        loadUsers();
      } else {
        emitApiError(res?.data?.errorMessage || "Không thể tạo người dùng.");
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleRoleChange = async (user, role) => {
    try {
      const res = await api.put(`/api/v1/users/${user.userId}/role`, { role });
      if (res?.data?.success) {
        setSnackbar({ open: true, message: `Đã cập nhật vai trò cho ${user.username}.` });
        loadUsers();
      } else {
        emitApiError(res?.data?.errorMessage || "Không thể cập nhật vai trò.");
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Root-only: assign a coordinator to an administrator (empty = unlink).
  const handleManagerChange = async (user, managerUserId) => {
    try {
      const res = await api.put(`/api/v1/users/${user.userId}/manager`, {
        managerUserId: managerUserId === "" ? null : Number(managerUserId),
      });
      if (res?.data?.success) {
        setSnackbar({
          open: true,
          message:
            managerUserId === ""
              ? `Đã bỏ gán người quản lý cho ${user.username}.`
              : `Đã gán ${user.username} cho ${res.data.data?.managerUsername || "quản trị viên"}.`,
        });
        loadUsers();
      } else {
        emitApiError(res?.data?.errorMessage || "Không thể cập nhật người quản lý.");
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleStatusChange = async (user) => {
    try {
      const res = await api.put(`/api/v1/users/${user.userId}/status`, {
        active: !user.active,
      });
      if (res?.data?.success) {
        setSnackbar({
          open: true,
          message: `${user.active ? "Đã vô hiệu" : "Đã kích hoạt"} tài khoản ${user.username}.`,
        });
        loadUsers();
      } else {
        emitApiError(res?.data?.errorMessage || "Không thể cập nhật trạng thái.");
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Root-only bulk removal: one request for the whole ticked selection. The
  // backend returns what it removed and what it had to skip (root accounts,
  // the signed-in account, rows that no longer exist).
  const handleDeleteUsers = async () => {
    if (deleteTargets.length === 0) return;
    setDeleting(true);
    try {
      const res = await api.post("/api/v1/users/bulk-delete", {
        userIds: deleteTargets.map((user) => user.userId),
      });
      if (res?.data?.success) {
        const removed = res.data.data?.removed || [];
        const skipped = res.data.data?.skipped || [];
        setSnackbar({
          open: true,
          message:
            removed.length === 1
              ? `Đã xoá người dùng ${removed[0]}.`
              : `Đã xoá ${removed.length} người dùng.`,
        });
        if (skipped.length > 0) {
          emitApiError(`Không xoá được: ${skipped.join("; ")}.`);
        }
        setDeleteTargets([]);
        setSelectedIds(new Set());
        loadUsers();
      } else {
        emitApiError(res?.data?.errorMessage || "Không thể xoá người dùng.");
      }
    } catch (err) {
      console.error(err);
    } finally {
      setDeleting(false);
    }
  };

  const handleResetPassword = async () => {
    if (!resetPass.newPass || !resetPass.repeatNewPass) {
      emitApiError("Vui lòng điền đầy đủ mật khẩu mới.");
      return;
    }
    if (resetPass.newPass !== resetPass.repeatNewPass) {
      emitApiError("Mật khẩu không khớp.");
      return;
    }
    try {
      const res = await withKeyRetry(async (cfg) => {
        const [newPass, repeatNewPass] = await Promise.all([
          encryptPassword(resetPass.newPass),
          encryptPassword(resetPass.repeatNewPass),
        ]);
        return api.post("/api/v1/users/reset-password", {
          userName: resetUserName,
          newPass,
          repeatNewPass,
          resetToken,
        }, cfg);
      });
      if (res?.status === 200) {
        setSnackbar({ open: true, message: res.data || "Đặt lại mật khẩu thành công!" });
        clearResetState();
        setForgot({ userName: "" });
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleForgotPassword = async () => {
    if (!forgot.userName) {
      emitApiError("Vui lòng nhập tên đăng nhập.");
      return;
    }
    try {
      const res = await api.get(
        `/api/v1/users/forgot-password?username=${forgot.userName}`,
        {}
      );
      if (res?.status === 200) {
        const token = res.data;
        setResetToken(token);
        setResetUserName(forgot.userName);
        setSecondsLeft(RESET_TOKEN_TTL / 1000);

        timerRef.current = setInterval(() => {
          setSecondsLeft((s) => {
            if (s <= 1) {
              clearInterval(timerRef.current);
              return 0;
            }
            return s - 1;
          });
        }, 1000);

        expireRef.current = setTimeout(clearResetState, RESET_TOKEN_TTL);
      }
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <Box sx={{ mt: 2, px: 2, maxWidth: 960 }}>
      <Grid container spacing={4}>
        <Grid size={{ xs: 12 }}>
          <Typography variant="h5" gutterBottom>
            User management
          </Typography>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            alignItems={{ xs: "stretch", sm: "center" }}
            sx={{ mb: 1.5, maxWidth: isRoot ? 900 : 720 }}
          >
            <TextField
              size="small"
              label="Tìm kiếm"
              placeholder="Tên đăng nhập hoặc vai trò…"
              value={userSearch}
              onChange={(e) => setUserSearch(e.target.value)}
              sx={{ minWidth: 260 }}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" />
                    </InputAdornment>
                  ),
                },
              }}
            />
            {isRoot && selectedIds.size > 0 && (
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography variant="body2" color="text.secondary">
                  Đã chọn {selectedIds.size}
                </Typography>
                <Button
                  size="small"
                  variant="contained"
                  color="error"
                  onClick={() => setDeleteTargets(selectedUsers)}
                >
                  Xoá đã chọn
                </Button>
                <Button size="small" onClick={() => setSelectedIds(new Set())}>
                  Bỏ chọn
                </Button>
              </Stack>
            )}
            <Typography variant="body2" color="text.secondary" sx={{ ml: "auto" }}>
              {visibleUsers.length}/{users.length} người dùng
            </Typography>
          </Stack>
          <TableContainer
            component={Paper}
            variant="outlined"
            // Ten small rows (10 × 52px) under the sticky header (32px) plus the
            // outlined border: the body scrolls instead of the table growing past
            // ten visible rows.
            sx={{ maxWidth: isRoot ? 900 : 720, maxHeight: 554 }}
          >
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {isRoot && (
                    <TableCell padding="checkbox">
                      <Checkbox
                        size="small"
                        checked={allSelected}
                        indeterminate={!allSelected && someSelected}
                        onChange={toggleAllSelection}
                        disabled={selectableUsers.length === 0}
                      />
                    </TableCell>
                  )}
                  <TableCell>Tên đăng nhập</TableCell>
                  <TableCell>Vai trò</TableCell>
                  {isRoot && <TableCell>Quản lý</TableCell>}
                  <TableCell>Trạng thái</TableCell>
                  <TableCell align="right">Hành động</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {visibleUsers.map((user) => {
                  const manageable = canManage(user) && user.username !== myUsername;
                  return (
                    <TableRow
                      key={user.userId}
                      hover
                      selected={selectedIds.has(user.userId)}
                    >
                      {isRoot && (
                        <TableCell padding="checkbox">
                          <Checkbox
                            size="small"
                            checked={selectedIds.has(user.userId)}
                            disabled={!canRemove(user)}
                            onChange={() => toggleUserSelection(user.userId)}
                          />
                        </TableCell>
                      )}
                      <TableCell>{user.username}</TableCell>
                      <TableCell>
                        <TextField
                          select
                          size="small"
                          value={primaryRole(user)}
                          disabled={!manageable}
                          onChange={(e) => handleRoleChange(user, e.target.value)}
                          sx={{ minWidth: 150 }}
                        >
                          {roleOptions.map((role) => (
                            <MenuItem key={role} value={role}>
                              {vnLabel(role)}
                            </MenuItem>
                          ))}
                          {/* Existing root accounts keep their label visible but the
                              option itself is disabled, so Root can be removed only. */}
                          {!roleOptions.includes(primaryRole(user)) && (
                            <MenuItem value={primaryRole(user)} disabled>
                              {vnLabel(primaryRole(user))}
                            </MenuItem>
                          )}
                        </TextField>
                      </TableCell>
                      {isRoot && (
                        <TableCell>
                          {primaryRole(user) === "Coordinator" ? (
                            <TextField
                              select
                              size="small"
                              value={user.managerUserId ?? ""}
                              onChange={(e) => handleManagerChange(user, e.target.value)}
                              sx={{ minWidth: 170 }}
                            >
                              <MenuItem value="">— Chưa gán —</MenuItem>
                              {administrators.map((admin) => (
                                <MenuItem key={admin.userId} value={admin.userId}>
                                  {admin.username}
                                </MenuItem>
                              ))}
                            </TextField>
                          ) : (
                            "—"
                          )}
                        </TableCell>
                      )}
                      <TableCell>
                        <Chip
                          size="small"
                          color={user.active ? "success" : "default"}
                          label={user.active ? "Hoạt động" : "Vô hiệu"}
                        />
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          <Button
                            size="small"
                            variant="outlined"
                            color={user.active ? "dark" : "success"}
                            disabled={!manageable}
                            onClick={() => handleStatusChange(user)}
                          >
                            {user.active ? "Vô hiệu" : "Kích hoạt"}
                          </Button>
                          {/* Only root removes accounts, never itself and never
                              another root account (those are provisioned). */}
                          {canRemove(user) && (
                            <Button
                              size="small"
                              variant="outlined"
                              color="error"
                              onClick={() => setDeleteTargets([user])}
                            >
                              Xoá
                            </Button>
                          )}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  );
                })}
                {visibleUsers.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={isRoot ? 7 : 5} align="center" sx={{ py: 4 }}>
                      <Typography color="text.secondary">
                        {userSearch
                          ? `Không tìm thấy "${userSearch}".`
                          : "Chưa có người dùng nào."}
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Divider />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Typography variant="h6" gutterBottom>
            Thêm người dùng
          </Typography>
          <Stack spacing={2} sx={{ maxWidth: 420 }}>
            <TextField
              label="Tên đăng nhập"
              placeholder="Tên đăng nhập"
              value={reg.userName}
              onChange={(e) => setReg({ ...reg, userName: e.target.value })}
              fullWidth
            />
            <TextField
              label="Mật khẩu"
              type="password"
              placeholder="Mật khẩu"
              value={reg.password}
              onChange={(e) => setReg({ ...reg, password: e.target.value })}
              fullWidth
            />
            <TextField
              label="Nhập lại mật khẩu"
              type="password"
              placeholder="Nhập lại mật khẩu"
              value={reg.repeatPassword}
              onChange={(e) =>
                setReg({ ...reg, repeatPassword: e.target.value })
              }
              fullWidth
            />
            <TextField
              select
              label="Vai trò"
              value={reg.role}
              onChange={(e) => setReg({ ...reg, role: e.target.value })}
              fullWidth
            >
              {roleOptions.map((role) => (
                <MenuItem key={role} value={role}>
                  {vnLabel(role)}
                </MenuItem>
              ))}
            </TextField>
            <Stack direction="row" spacing={1}>
              <Button variant="contained" onClick={handleRegister}>
                Đăng ký
              </Button>
              <Button
                variant="outlined"
                onClick={() =>
                  setReg({ userName: "", password: "", repeatPassword: "", role: roleOptions[0] })
                }
              >
                Xoá
              </Button>
            </Stack>
          </Stack>
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Divider />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Typography variant="h6" gutterBottom sx={{ mt: 1 }}>
            Quên mật khẩu
          </Typography>
          <Stack spacing={2} sx={{ maxWidth: 420 }}>
            <TextField
              label="Tên đăng nhập"
              placeholder="Tên đăng nhập"
              value={forgot.userName}
              onChange={(e) => setForgot({ userName: e.target.value })}
              fullWidth
            />
            <Stack direction="row" spacing={1}>
              <Button variant="contained" color="warning" onClick={handleForgotPassword}>
                Quên mật khẩu
              </Button>
              <Button variant="outlined" onClick={() => setForgot({ userName: "" })}>
                Xoá
              </Button>
            </Stack>
          </Stack>
        </Grid>

        {resetToken && (
          <Grid size={{ xs: 12 }}>
            <Typography variant="h6" gutterBottom>
              Đặt lại mật khẩu cho: <strong>{resetUserName}</strong>
              <Typography
                component="span"
                variant="body2"
                color="text.secondary"
                sx={{ ml: 2 }}
              >
                (hết hạn sau {Math.floor(secondsLeft / 60)}:
                {String(secondsLeft % 60).padStart(2, "0")})
              </Typography>
            </Typography>
            <Stack spacing={2} sx={{ maxWidth: 420 }}>
              <TextField
                label="Mật khẩu mới"
                type="password"
                placeholder="Mật khẩu mới"
                value={resetPass.newPass}
                onChange={(e) =>
                  setResetPass({ ...resetPass, newPass: e.target.value })
                }
                fullWidth
              />
              <TextField
                label="Nhập lại mật khẩu mới"
                type="password"
                placeholder="Nhập lại mật khẩu mới"
                value={resetPass.repeatNewPass}
                onChange={(e) =>
                  setResetPass({ ...resetPass, repeatNewPass: e.target.value })
                }
                fullWidth
              />
              <Button variant="contained" color="error" onClick={handleResetPassword}>
                Đặt lại mật khẩu
              </Button>
            </Stack>
          </Grid>
        )}
      </Grid>
      <Dialog
        open={deleteTargets.length > 0}
        onClose={deleting ? undefined : () => setDeleteTargets([])}
        maxWidth="sm"
        fullWidth
        PaperComponent={DraggableResizablePaper}
      >
        <DialogTitle className={DIALOG_DRAG_HANDLE} sx={{ cursor: "move" }}>
          {deleteTargets.length === 1
            ? `Xoá người dùng ${deleteTargets[0].username}`
            : `Xoá ${deleteTargets.length} người dùng`}
        </DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary">
            {deleteTargets.length === 1
              ? "Tài khoản sẽ bị ẩn khỏi danh sách người dùng và không thể đăng nhập nữa."
              : "Các tài khoản sau sẽ bị ẩn khỏi danh sách người dùng và không thể đăng nhập nữa."}{" "}
            Hoá đơn, công nợ và các dữ liệu liên quan vẫn được giữ lại.
          </Typography>
          {deleteTargets.length > 1 && (
            <Box sx={{ mt: 1.5, maxHeight: 180, overflowY: "auto" }}>
              <Stack spacing={0.5}>
                {deleteTargets.map((user) => (
                  <Typography key={user.userId} variant="body2">
                    • {user.username}{" "}
                    <Typography component="span" variant="body2" color="text.secondary">
                      ({vnLabel(primaryRole(user))})
                    </Typography>
                  </Typography>
                ))}
              </Stack>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTargets([])} disabled={deleting}>
            Đóng
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleDeleteUsers}
            disabled={deleting}
          >
            {deleteTargets.length === 1
              ? "Xoá người dùng"
              : `Xoá ${deleteTargets.length} người dùng`}
          </Button>
        </DialogActions>
      </Dialog>
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
        onClose={(_, reason) => {
          if (reason === "clickaway") return;
          setSnackbar((s) => ({ ...s, open: false }));
        }}
      >
        <Alert
          severity="success"
          variant="filled"
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
          sx={{ width: "100%" }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}

export default SuperAdminPage;

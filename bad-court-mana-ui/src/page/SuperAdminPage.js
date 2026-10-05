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
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import api from "../api/index";
import { emitApiError } from "../api/errorBus";
import { encryptPassword, withKeyRetry } from "../api/rsaCrypto";
import { AuthContext } from "../context/AuthContext";
import { getUsername } from "../api/tokenStore";

const RESET_TOKEN_TTL = 3 * 60 * 1000;

function SuperAdminPage() {
  const { hasRole } = useContext(AuthContext);
  const isRoot = hasRole("ROOT");
  const myUsername = getUsername();

  const [users, setUsers] = useState([]);
  const [reg, setReg] = useState({
    userName: "",
    password: "",
    repeatPassword: "",
    role: isRoot ? "ADMINISTRATOR" : "COORDINATOR",
  });
  const [forgot, setForgot] = useState({ userName: "" });
  const [resetToken, setResetToken] = useState(null);
  const [resetUserName, setResetUserName] = useState("");
  const [resetPass, setResetPass] = useState({ newPass: "", repeatNewPass: "" });
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [snackbar, setSnackbar] = useState({ open: false, message: "" });
  const timerRef = useRef(null);
  const expireRef = useRef(null);

  const roleOptions = isRoot
    ? ["ADMINISTRATOR", "COORDINATOR", "ROOT"]
    : ["COORDINATOR"];

  const primaryRole = (user) => user.roles?.[0] || "COORDINATOR";

  const canManage = (user) =>
    isRoot || (user.roles || []).every((role) => role === "COORDINATOR");

  const loadUsers = useCallback(async () => {
    try {
      const res = await api.get("/api/v1/users");
      if (res?.data?.success) setUsers(res.data.data || []);
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
          <Table size="small" sx={{ maxWidth: 720 }}>
            <TableHead>
              <TableRow>
                <TableCell>Tên đăng nhập</TableCell>
                <TableCell>Vai trò</TableCell>
                <TableCell>Trạng thái</TableCell>
                <TableCell align="right">Hành động</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.map((user) => {
                const manageable = canManage(user) && user.username !== myUsername;
                return (
                  <TableRow key={user.userId}>
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
                            {role}
                          </MenuItem>
                        ))}
                        {!manageable && !roleOptions.includes(primaryRole(user)) && (
                          <MenuItem value={primaryRole(user)}>{primaryRole(user)}</MenuItem>
                        )}
                      </TextField>
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        color={user.active ? "success" : "default"}
                        label={user.active ? "Hoạt động" : "Vô hiệu"}
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        variant="outlined"
                        color={user.active ? "warning" : "success"}
                        disabled={!manageable}
                        onClick={() => handleStatusChange(user)}
                      >
                        {user.active ? "Vô hiệu" : "Kích hoạt"}
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
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
                  {role}
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

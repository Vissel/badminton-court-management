import { useState, useEffect, useRef } from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import api from "../api/index";

const RESET_TOKEN_TTL = 3 * 60 * 1000;

function SuperAdminPage() {
  const [reg, setReg] = useState({
    userName: "",
    password: "",
    repeatPassword: "",
  });
  const [forgot, setForgot] = useState({ userName: "" });
  const [resetToken, setResetToken] = useState(null);
  const [resetUserName, setResetUserName] = useState("");
  const [resetPass, setResetPass] = useState({ newPass: "", repeatNewPass: "" });
  const [secondsLeft, setSecondsLeft] = useState(0);
  const timerRef = useRef(null);
  const expireRef = useRef(null);

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
      alert("Vui lòng điền đầy đủ thông tin.");
      return;
    }
    if (reg.password !== reg.repeatPassword) {
      alert("Mật khẩu không khớp.");
      return;
    }
    try {
      const res = await api.post("/admin/internal/registerUser", {
        userName: reg.userName,
        password: reg.password,
      });
      if (res?.status === 200) {
        alert("Đăng ký admin thành công!");
        setReg({ userName: "", password: "", repeatPassword: "" });
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleResetPassword = async () => {
    if (!resetPass.newPass || !resetPass.repeatNewPass) {
      alert("Vui lòng điền đầy đủ mật khẩu mới.");
      return;
    }
    if (resetPass.newPass !== resetPass.repeatNewPass) {
      alert("Mật khẩu không khớp.");
      return;
    }
    try {
      const res = await api.post("/admin/internal/resetPassword", {
        userName: resetUserName,
        newPass: resetPass.newPass,
        repeatNewPass: resetPass.repeatNewPass,
        resetToken,
      });
      if (res?.status === 200) {
        alert("Đặt lại mật khẩu thành công!");
        clearResetState();
        setForgot({ userName: "" });
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleForgotPassword = async () => {
    if (!forgot.userName) {
      alert("Vui lòng nhập tên đăng nhập.");
      return;
    }
    try {
      const res = await api.get(
        `/admin/internal/forgotPassword?username=${forgot.userName}`,
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
    <Box sx={{ mt: 2, px: 2, maxWidth: 720 }}>
      <Grid container spacing={4}>
        <Grid size={{ xs: 12 }}>
          <Typography variant="h5" gutterBottom>
            Đăng ký Admin
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
            <Stack direction="row" spacing={1}>
              <Button variant="contained" onClick={handleRegister}>
                Đăng ký
              </Button>
              <Button
                variant="outlined"
                onClick={() =>
                  setReg({ userName: "", password: "", repeatPassword: "" })
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
          <Typography variant="h5" gutterBottom sx={{ mt: 1 }}>
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
    </Box>
  );
}

export default SuperAdminPage;

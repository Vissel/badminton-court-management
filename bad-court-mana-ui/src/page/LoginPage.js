import { useState, useContext } from "react";
import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import { useNavigate } from "react-router";
import { AuthContext } from "../context/AuthContext";
import api from "../api";
import { emitApiError } from "../api/errorBus";
import { encryptPassword, withKeyRetry } from "../api/rsaCrypto";

function LoginPage() {
  const { setAuthenticated, setLoading } = useContext(AuthContext);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const navigate = useNavigate();

  const handleLogin = async () => {
    try {
      const res = await withKeyRetry(async (cfg) => {
        const inputPassword = await encryptPassword(password);
        return api.post("/login", {
          inputUsername: username,
          inputPassword,
        }, cfg);
      });

      if (res?.status === 200) {
        sessionStorage.setItem("csrfToken", res.data.csrfToken);
        sessionStorage.setItem("username", res.data.username);
        setAuthenticated(true);
        navigate("/home");
      }
    } catch (err) {
      setError("Invalid credentials");
      emitApiError("Đăng nhập thất bại");
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (event) => {
    if (event.key === "Enter") {
      handleLogin();
    }
  };

  return (
    <Box sx={{ maxWidth: 480, mx: "auto", mt: 4, px: 2 }}>
      <Typography variant="h5" gutterBottom>
        Đăng nhập
      </Typography>
      <Stack spacing={2} component="form" noValidate autoComplete="off">
        <TextField
          label="Tên đăng nhập"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={handleKeyPress}
          placeholder="Username"
          required
          fullWidth
          autoComplete="username"
        />
        <TextField
          label="Mật khẩu"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={handleKeyPress}
          required
          fullWidth
          autoComplete="current-password"
        />
        {error ? (
          <Typography variant="body2" color="error">
            {error}
          </Typography>
        ) : null}
        <Button variant="contained" size="large" onClick={handleLogin}>
          Đăng nhập
        </Button>
      </Stack>
    </Box>
  );
}

export default LoginPage;

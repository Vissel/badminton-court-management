import React, { useContext, useState } from "react";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import MenuIcon from "@mui/icons-material/Menu";
import useMediaQuery from "@mui/material/useMediaQuery";
import { useTheme, darken } from "@mui/material/styles";
import { AuthContext } from "./context/AuthContext";
import { useNavigate } from "react-router";

function Header() {
  const { logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const theme = useTheme();
  const isNarrow = useMediaQuery(theme.breakpoints.down("md"));
  const isRoot = sessionStorage.getItem("username") === "rootuser";

  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const navItems = [
    { label: "Trang chủ", path: "/home" },
    { label: "Cài đặt", path: "/setup" },
    { label: "Thống kê", path: "/report" },
    ...(isRoot ? [{ label: "Super Admin", path: "/super-admin" }] : []),
    { label: "Đăng xuất", path: null, action: "logout" },
  ];

  const go = (item) => {
    setAnchorEl(null);
    if (item.action === "logout") {
      logout();
      return;
    }
    navigate(item.path);
  };

  return (
    <AppBar
      position="sticky"
      elevation={1}
      color="transparent"
      sx={(theme) => {
        /* Higher tonal offset: darken(info.main, 0.5) — coefficient 0–1 per MUI colorManipulator */
        const bg = darken(theme.palette.info.main, 0.5);
        return {
          bgcolor: bg,
          color: theme.palette.getContrastText(bg),
        };
      }}
    >
      <Toolbar sx={{ gap: 2, flexWrap: "wrap" }}>
        <Typography
          variant="h6"
          component="div"
          sx={{
            flexGrow: isNarrow ? 1 : 0,
            cursor: "pointer",
            fontWeight: 600,
            py: 0.5,
          }}
          onClick={() => navigate("/home")}
        >
          Quản lý sân cầu lông Tân Châu
        </Typography>

        {isNarrow ? (
          <>
            <IconButton
              color="inherit"
              edge="end"
              aria-label="menu"
              aria-controls={open ? "nav-menu" : undefined}
              aria-haspopup="true"
              aria-expanded={open ? "true" : undefined}
              onClick={(e) => setAnchorEl(e.currentTarget)}
            >
              <MenuIcon />
            </IconButton>
            <Menu
              id="nav-menu"
              anchorEl={anchorEl}
              open={open}
              onClose={() => setAnchorEl(null)}
              anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
              transformOrigin={{ vertical: "top", horizontal: "right" }}
            >
              {navItems.map((item) => (
                <MenuItem key={item.label} onClick={() => go(item)}>
                  {item.label}
                </MenuItem>
              ))}
            </Menu>
          </>
        ) : (
          <Box sx={{ ml: "auto", display: "flex", gap: 1, flexWrap: "wrap" }}>
            {navItems.map((item) => (
              <Button
                key={item.label}
                color="inherit"
                onClick={() => go(item)}
              >
                {item.label}
              </Button>
            ))}
          </Box>
        )}
      </Toolbar>
    </AppBar>
  );
}

export default Header;

import React, { useContext, useState } from "react";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import MenuIcon from "@mui/icons-material/Menu";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import HomeIcon from "@mui/icons-material/Home";
import SettingsIcon from "@mui/icons-material/Settings";
import AdminPanelSettingsIcon from "@mui/icons-material/AdminPanelSettings";
import ReceiptLongIcon from "@mui/icons-material/ReceiptLong";
import AssessmentIcon from "@mui/icons-material/Assessment";
import PaymentsIcon from "@mui/icons-material/Payments";
import Inventory2Icon from "@mui/icons-material/Inventory2";
import ManageAccountsIcon from "@mui/icons-material/ManageAccounts";
import LogoutIcon from "@mui/icons-material/Logout";
import useMediaQuery from "@mui/material/useMediaQuery";
import { useTheme, darken } from "@mui/material/styles";
import { AuthContext } from "./context/AuthContext";
import { useNavigate } from "react-router";

function Header() {
  const { authenticated, hasRole, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const theme = useTheme();
  const isNarrow = useMediaQuery(theme.breakpoints.down("md"));
  const isAdministrator = hasRole("ROOT", "ADMINISTRATOR");

  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);
  // Desktop "Admin" dropdown; mobile in-place expansion of the same group.
  const [subMenu, setSubMenu] = useState(null); // { anchorEl, items }
  const [mobileSubOpen, setMobileSubOpen] = useState(false);

  const navItems = authenticated
    ? [
      { label: "Trang chủ", path: "/home", icon: HomeIcon },
      ...(isAdministrator
        ? [
          { label: "Cài đặt", path: "/setup", icon: SettingsIcon },
          // { label: "Quản lý hàng", path: "/products" },
          {
            label: "Admin",
            icon: AdminPanelSettingsIcon,
            children: [
              { label: "Hoá đơn", path: "/bills", icon: ReceiptLongIcon },
              { label: "Thống kê", path: "/report", icon: AssessmentIcon },
              { label: "Quản lý nợ", path: "/debtManagement", icon: PaymentsIcon },
              { label: "Quản lý kho", path: "/inventory", icon: Inventory2Icon },
              { label: "Quản lý user", path: "/user-management", icon: ManageAccountsIcon },
            ],
          },
        ]
        : []),
      { label: "Đăng xuất", path: null, action: "logout", icon: LogoutIcon },
    ]
    : [];

  const go = (item) => {
    setAnchorEl(null);
    setSubMenu(null);
    setMobileSubOpen(false);
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
              {navItems.map((item) =>
                item.children ? (
                  <React.Fragment key={item.label}>
                    <MenuItem onClick={() => setMobileSubOpen((v) => !v)}>
                      {item.icon && (
                        <ListItemIcon>
                          <item.icon fontSize="small" />
                        </ListItemIcon>
                      )}
                      <ListItemText primary={item.label} />
                      {mobileSubOpen ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                    </MenuItem>
                    {mobileSubOpen &&
                      item.children.map((child) => (
                        <MenuItem key={child.label} onClick={() => go(child)} sx={{ pl: 2 }}>
                          {child.icon && (
                            <ListItemIcon sx={{ minWidth: 36 }}>
                              <child.icon fontSize="small" />
                            </ListItemIcon>
                          )}
                          <ListItemText primary={child.label} />
                        </MenuItem>
                      ))}
                  </React.Fragment>
                ) : (
                  <MenuItem key={item.label} onClick={() => go(item)}>
                    {item.icon && (
                      <ListItemIcon>
                        <item.icon fontSize="small" />
                      </ListItemIcon>
                    )}
                    <ListItemText primary={item.label} />
                  </MenuItem>
                )
              )}
            </Menu>
          </>
        ) : (
          <Box sx={{ ml: "auto", display: "flex", gap: 1, flexWrap: "wrap", alignItems: "center" }}>
            {navItems.map((item) =>
              item.children ? (
                <Button
                  key={item.label}
                  color="inherit"
                  startIcon={item.icon ? <item.icon /> : undefined}
                  endIcon={<ExpandMoreIcon />}
                  onClick={(e) => setSubMenu({ anchorEl: e.currentTarget, items: item.children })}
                >
                  {item.label}
                </Button>
              ) : (
                <Button
                  key={item.label}
                  color="inherit"
                  startIcon={item.icon ? <item.icon /> : undefined}
                  onClick={() => go(item)}
                >
                  {item.label}
                </Button>
              )
            )}
            <Menu
              open={Boolean(subMenu?.anchorEl)}
              anchorEl={subMenu?.anchorEl}
              onClose={() => setSubMenu(null)}
              anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
              transformOrigin={{ vertical: "top", horizontal: "right" }}
            >
              {subMenu?.items.map((child) => (
                <MenuItem key={child.label} onClick={() => go(child)}>
                  {child.icon && (
                    <ListItemIcon>
                      <child.icon fontSize="small" />
                    </ListItemIcon>
                  )}
                  <ListItemText primary={child.label} />
                </MenuItem>
              ))}
            </Menu>
          </Box>
        )}
      </Toolbar>
    </AppBar>
  );
}

export default Header;

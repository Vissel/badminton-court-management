import { createTheme } from "@mui/material/styles";

/** Aligns with the previous Bootstrap-heavy palette for a familiar look */
export const appTheme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#0d6efd" },
    secondary: { main: "#6c757d" },
    success: { main: "#198754" },
    error: { main: "#dc3545" },
    warning: { main: "#ffc107" },
    background: {
      default: "#fafafa",
      paper: "#ffffff",
    },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { textTransform: "none" },
      },
    },
  },
});

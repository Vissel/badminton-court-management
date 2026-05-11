import React, { useContext } from "react";
import { Navigate, Outlet } from "react-router-dom";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import Typography from "@mui/material/Typography";
import { AuthContext } from "./AuthContext";

const ProtectedRoute = ({ children }) => {
  const { authenticated, loading } = useContext(AuthContext);

  if (loading) {
    return (
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: 2,
          minHeight: "40vh",
        }}
      >
        <CircularProgress />
        <Typography color="text.secondary">Loading session...</Typography>
      </Box>
    );
  }
  if (!authenticated) {
    return <Navigate to="/login" replace />;
  }

  return children ? children : <Outlet />;
};

export default ProtectedRoute;

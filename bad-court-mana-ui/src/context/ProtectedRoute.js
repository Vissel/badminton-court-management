import React, { useContext } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { AuthContext } from "./AuthContext";

const ProtectedRoute = ({ children }) => {
  const { authenticated, loading } = useContext(AuthContext);

  //   if (loading) return <div>Loading...</div>;
  if (loading) {
    return <div>Loading session...</div>; // Wait until auth check finishes
  }
  if (!authenticated) {
    // Redirect to the login page if not authenticated
    return <Navigate to="/login" replace />;
  }

  // Render the children (the protected component) or Outlet for nested routes
  return children ? children : <Outlet />;
};

export default ProtectedRoute;

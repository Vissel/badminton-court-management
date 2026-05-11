import "./App.css";
import React from "react";
import { Route, HashRouter as Router, Routes } from "react-router";
import Box from "@mui/material/Box";
import LoginPage from "./page/LoginPage";

import HomePage from "./page/HomePage";
import SetupPage from "./page/SetupPage";
import ReportPage from "./page/ReportPage";
import Footer from "./Footer";
import Header from "./Header";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./context/ProtectedRoute";
import HomePage2 from "./page/HomePage_mess";
import HomePageError from "./page/HomePage_error";

import DateTimeBar from "./DateTimeBar";
import SuperAdminPage from "./page/SuperAdminPage";

function App() {
  return (
    <AuthProvider>
      <Router>
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            minHeight: "100vh",
            bgcolor: "background.default",
          }}
        >
          <Header />
          <Box
            component="main"
            sx={{
              flexGrow: 1,
              py: 1,
              px: { xs: 1, sm: 2 },
              pb: "calc(30px + 12px)",
            }}
          >
            <DateTimeBar />
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route
                path="/home"
                element={
                  <ProtectedRoute>
                    <HomePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/home-2"
                element={
                  <ProtectedRoute>
                    <HomePage2 />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/home-3"
                element={
                  <ProtectedRoute>
                    <HomePageError />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/setup"
                element={
                  <ProtectedRoute>
                    <SetupPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/report"
                element={
                  <ProtectedRoute>
                    <ReportPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/super-admin"
                element={
                  <ProtectedRoute>
                    <SuperAdminPage />
                  </ProtectedRoute>
                }
              />
              <Route path="*" element={<LoginPage />} />
            </Routes>
          </Box>
          <Footer />
        </Box>
      </Router>
    </AuthProvider>
  );
}

export default App;

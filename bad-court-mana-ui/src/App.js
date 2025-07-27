import "./App.css";
import React from "react";
import { Route, HashRouter as Router, Routes } from "react-router";
import LoginPage from "./page/LoginPage";

import HomePage from "./page/HomePage";
import SetupPage from "./page/SetupPage";
import Footer from "./Footer";
import Header from "./Header";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./context/ProtectedRoute";
import HomePage2 from "./page/HomePage_mess";
import HomePageError from "./page/HomePage_error";

function App() {
  return (
    <AuthProvider>
      <Router>
        <div>
          <Header />
          <main className="flex-grow-1 py-4 row-space">
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              {/* Protected routes */}
              {/* <Route element={<ProtectedRoute />}> */}
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
              {/* </Route > */}
              <Route path="*" element={<LoginPage />} />
            </Routes>
          </main>
          <Footer />
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;

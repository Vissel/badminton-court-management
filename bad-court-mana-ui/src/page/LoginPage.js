// src/LoginPage.js
import { useState, useContext } from "react";
import { Form, Row, Col, Button, Container } from "react-bootstrap";
import Cookies from "js-cookie";
import axios from "axios";
import { useNavigate } from "react-router";
import { AuthContext } from "../context/AuthContext";
import { setCsrfTokenGetter } from "../api";

function LoginPage() {
  const { setAuthenticated, csrfToken, setCsrfToken } = useContext(AuthContext);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const localHost = "http://localhost:9080";
  const context = "bad-court-management-dev";
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    // e.preventDefault();

    try {
      const token = await axios.get(`${localHost}/${context}/csrf`, {
        withCredentials: true,
      });
      if (token.status === 200) {
        const loginToken = token.data.token;
        // setCsrfToken(loginToken);
        const res = await axios.post(
          `${localHost}/${context}/login`,
  
          new URLSearchParams({
            username,
            password,
          }),
          {
            headers: {
              "Content-Type": "application/x-www-form-urlencoded",
              "X-XSRF-TOKEN": loginToken,
            },
            withCredentials: true,
          }
        );
        if (res.status === 200) {
          const token = res.data.csrfToken;
          setCsrfToken(token);
          Cookies.set("XSRF-TOKEN",token);
          
          setAuthenticated(true);
          navigate("/home");
        }
      } else {
        alert("Cannot login");
        return;
      }
    } catch (err) {
      setError("Invalid credentials");
      alert("Login failed");
    }
  };

  const handleKeyPress = (event) => {
    // look for the `Enter` keyCode
    if (event.keyCode === 13 || event.which === 13) {
      handleLogin();
    }
  };
  return (
    <Container fluid>
      <Form>
        <Form.Group as={Row} className="mb-3" controlId="formPlaintextEmail">
          <Form.Label column sm="2">
            Username
          </Form.Label>
          <Col sm="5">
            <Form.Control
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              onKeyDown={handleKeyPress}
              placeholder="Username"
              required
            />
          </Col>
        </Form.Group>

        <Form.Group as={Row} className="mb-3" controlId="formPlaintextPassword">
          <Form.Label column sm="2">
            Password
          </Form.Label>
          <Col sm="5">
            <Form.Control
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={handleKeyPress}
              required
            />
          </Col>
        </Form.Group>
        <Button variant="primary" onClick={handleLogin}>
          Login
        </Button>
      </Form>
    </Container>
  );
}

export default LoginPage;

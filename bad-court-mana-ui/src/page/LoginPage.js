// src/LoginPage.js
import { useState, useContext } from "react";
import { Form, Row, Col, Button, Container } from "react-bootstrap";

import { useNavigate } from "react-router";
import { AuthContext } from "../context/AuthContext";
import api from "../api";

function LoginPage() {
  const { setAuthenticated, setLoading } = useContext(AuthContext);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const navigate = useNavigate();

  const handleLogin = async (e) => {
    // e.preventDefault();

    try {
      const res = await api.post(
        `/login?${new URLSearchParams({
          username,
          password,
        })}`,
        {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        }
      );

      if (res.status === 200) {
        // const token = res.data.csrfToken;
        // setCsrfToken(token);
        // Cookies.set("XSRF-TOKEN",token);
        sessionStorage.setItem("csrfToken", res.data.csrfToken);
        sessionStorage.setItem("username", res.data.username);
        setAuthenticated(true);

        navigate("/home");
      }
      // } else {
      //   alert("Cannot login");
      //   return;
      // }
    } catch (err) {
      setError("Invalid credentials");
      alert("Đăng nhập thất bại");
    } finally {
      setLoading(false);
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
            Tên đăng nhập
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
            Mật khẩu
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
          Đăng nhập
        </Button>
      </Form>
    </Container>
  );
}

export default LoginPage;

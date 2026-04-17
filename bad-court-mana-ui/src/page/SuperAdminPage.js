import { useState, useEffect, useRef } from "react";
import { Container, Row, Col, Form, Button } from "react-bootstrap";
import api from "../api/index";

const RESET_TOKEN_TTL = 3 * 60 * 1000; // 3 minutes in ms

function SuperAdminPage() {
  const [reg, setReg] = useState({ userName: "", password: "", repeatPassword: "" });
  const [forgot, setForgot] = useState({ userName: "" });
  const [resetToken, setResetToken] = useState(null);
  const [resetUserName, setResetUserName] = useState("");
  const [resetPass, setResetPass] = useState({ newPass: "", repeatNewPass: "" });
  const [secondsLeft, setSecondsLeft] = useState(0);
  const timerRef = useRef(null);
  const expireRef = useRef(null);

  const clearResetState = () => {
    setResetToken(null);
    setResetUserName("");
    setResetPass({ newPass: "", repeatNewPass: "" });
    setSecondsLeft(0);
    clearInterval(timerRef.current);
    clearTimeout(expireRef.current);
  };

  useEffect(() => () => { clearInterval(timerRef.current); clearTimeout(expireRef.current); }, []);

  const handleRegister = async () => {
    if (!reg.userName || !reg.password || !reg.repeatPassword) {
      alert("Vui lòng điền đầy đủ thông tin.");
      return;
    }
    if (reg.password !== reg.repeatPassword) {
      alert("Mật khẩu không khớp.");
      return;
    }
    try {
      const res = await api.post("/admin/internal/registerUser", {
        userName: reg.userName,
        password: reg.password,
      });
      if (res?.status === 200) {
        alert("Đăng ký admin thành công!");
        setReg({ userName: "", password: "", repeatPassword: "" });
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleResetPassword = async () => {
    if (!resetPass.newPass || !resetPass.repeatNewPass) {
      alert("Vui lòng điền đầy đủ mật khẩu mới.");
      return;
    }
    if (resetPass.newPass !== resetPass.repeatNewPass) {
      alert("Mật khẩu không khớp.");
      return;
    }
    try {
      const res = await api.post("/admin/internal/resetPassword", {
        userName: resetUserName,
        newPass: resetPass.newPass,
        repeatNewPass: resetPass.repeatNewPass,
        resetToken,
      });
      if (res?.status === 200) {
        alert("Đặt lại mật khẩu thành công!");
        clearResetState();
        setForgot({ userName: "" });
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleForgotPassword = async () => {
    if (!forgot.userName) {
      alert("Vui lòng nhập tên đăng nhập.");
      return;
    }
    try {
      const res = await api.get(`/admin/internal/forgotPassword?username=${forgot.userName}`, {
        
      });
      if (res?.status === 200) {
        const token = res.data;
        setResetToken(token);
        setResetUserName(forgot.userName);
        setSecondsLeft(RESET_TOKEN_TTL / 1000);

        timerRef.current = setInterval(() => {
          setSecondsLeft((s) => {
            if (s <= 1) { clearInterval(timerRef.current); return 0; }
            return s - 1;
          });
        }, 1000);

        expireRef.current = setTimeout(clearResetState, RESET_TOKEN_TTL);
      }
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <Container className="mt-4">
      <Row className="mb-4">
        <Col sm={6}>
          <h4>Đăng ký Admin</h4>
          <Form>
            <Form.Group className="mb-2">
              <Form.Label>Tên đăng nhập</Form.Label>
              <Form.Control
                type="text"
                placeholder="Tên đăng nhập"
                value={reg.userName}
                onChange={(e) => setReg({ ...reg, userName: e.target.value })}
              />
            </Form.Group>
            <Form.Group className="mb-2">
              <Form.Label>Mật khẩu</Form.Label>
              <Form.Control
                type="password"
                placeholder="Mật khẩu"
                value={reg.password}
                onChange={(e) => setReg({ ...reg, password: e.target.value })}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Nhập lại mật khẩu</Form.Label>
              <Form.Control
                type="password"
                placeholder="Nhập lại mật khẩu"
                value={reg.repeatPassword}
                onChange={(e) => setReg({ ...reg, repeatPassword: e.target.value })}
              />
            </Form.Group>
            <Button variant="primary" onClick={handleRegister}>
              Đăng ký
            </Button>
            <Button variant="secondary" className="ms-2" onClick={() => setReg({ userName: "", password: "", repeatPassword: "" })}>
              Xoá
            </Button>
          </Form>
        </Col>
      </Row>

      <hr />

      <Row>
        <Col sm={6}>
          <h4>Quên mật khẩu</h4>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Tên đăng nhập</Form.Label>
              <Form.Control
                type="text"
                placeholder="Tên đăng nhập"
                value={forgot.userName}
                onChange={(e) => setForgot({ userName: e.target.value })}
              />
            </Form.Group>
            <Button variant="warning" onClick={handleForgotPassword}>
              Quên mật khẩu
            </Button>
            <Button variant="secondary" className="ms-2" onClick={() => setForgot({ userName: "" })}>
              Xoá
            </Button>
          </Form>
        </Col>
      </Row>

      {resetToken && (
        <Row className="mt-4">
          <Col sm={6}>
            <h4>
              Đặt lại mật khẩu cho: <strong>{resetUserName}</strong>
              <span className="ms-3 text-muted" style={{ fontSize: "0.85rem" }}>
                (hết hạn sau {Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, "0")})
              </span>
            </h4>
            <Form>
              <Form.Group className="mb-2">
                <Form.Label>Mật khẩu mới</Form.Label>
                <Form.Control
                  type="password"
                  placeholder="Mật khẩu mới"
                  value={resetPass.newPass}
                  onChange={(e) => setResetPass({ ...resetPass, newPass: e.target.value })}
                />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Nhập lại mật khẩu mới</Form.Label>
                <Form.Control
                  type="password"
                  placeholder="Nhập lại mật khẩu mới"
                  value={resetPass.repeatNewPass}
                  onChange={(e) => setResetPass({ ...resetPass, repeatNewPass: e.target.value })}
                />
              </Form.Group>
              <Button variant="danger" onClick={handleResetPassword}>
                Đặt lại mật khẩu
              </Button>
            </Form>
          </Col>
        </Row>
      )}
    </Container>
  );
}

export default SuperAdminPage;

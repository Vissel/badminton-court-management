import React from "react";
import "./index.css";
import { Container, Navbar, Nav } from "react-bootstrap";
import { useContext } from "react";
import { AuthContext } from "./context/AuthContext";
import { useNavigate } from "react-router";

function Header() {
  const { logout } = useContext(AuthContext);
  let navigate = useNavigate();
  const isRoot = sessionStorage.getItem("username") === "rootuser";

  return (
    <Navbar bg="dark" variant="dark" expand="lg">
      <Container>
        <Navbar.Brand onClick={() => navigate("/home")} style={{ cursor: "pointer" }}> Quản lý sân cầu lông Tân Châu </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="ms-auto" style={{ gap: "20px" }}>
            <Nav.Link onClick={() => navigate("/home")}>Trang chủ</Nav.Link>
            <Nav.Link onClick={() => navigate("/setup")}>Cài đặt</Nav.Link>
            <Nav.Link onClick={() => navigate("/report")}>Thống kê </Nav.Link>
            {isRoot && <Nav.Link onClick={() => navigate("/super-admin")}>Super Admin</Nav.Link>}
            <Nav.Link onClick={logout}>Đăng xuất</Nav.Link>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
export default Header;

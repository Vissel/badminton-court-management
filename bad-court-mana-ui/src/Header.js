import React from "react";
import "./index.css";
import { Container, Navbar, Nav } from "react-bootstrap";
import { useContext } from "react";
import { AuthContext } from "./context/AuthContext";
import { useNavigate } from "react-router";
function Header() {
  const {  logout } = useContext(AuthContext);
  let navigate = useNavigate();

  return (
    <Navbar bg="dark" variant="dark" expand="lg">
      <Container>
        <Navbar.Brand href="/"> Quản lý sân cầu lông Tân Châu </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="ms-auto">
            <Nav.Link href="/">Trang chủ</Nav.Link>
            <Nav.Link onClick={() => navigate("/setup")}>Cài đặt</Nav.Link>
            <Nav.Link onClick={logout}>Đăng xuất</Nav.Link>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
export default Header;

// Footer.js
import React from "react";
import { Container } from "react-bootstrap";

const Footer = () => {
  return (
    <footer className="bg-dark text-light mt-auto app-footer">
      <Container className="text-center">
        <p>© {new Date().getFullYear()} Nguyen Ngoc Thach. All rights reserved.</p>
      </Container>
    </footer>
  );
};
export default Footer;

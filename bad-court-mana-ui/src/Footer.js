// Footer.js
import React from "react";
import { Container } from "react-bootstrap";

const Footer = () => {
  return (
    <footer className="bg-dark text-light mt-auto app-footer">
      <Container className="text-center">
        <p>
          © {new Date().getFullYear()} Jade · Licensed under the{" "}
          <a
            href="https://opensource.org/licenses/MIT"
            className="link-light link-underline-opacity-75 link-underline-opacity-100-hover"
            target="_blank"
            rel="noopener noreferrer"
          >
            MIT License
          </a>
        </p>
      </Container>
    </footer>
  );
};
export default Footer;

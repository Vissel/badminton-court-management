import React from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Link from "@mui/material/Link";
import Container from "@mui/material/Container";

const Footer = () => {
  return (
    <Box
      component="footer"
      sx={{
        position: "fixed",
        left: 0,
        bottom: 0,
        width: "100%",
        height: 30,
        display: "flex",
        alignItems: "center",
        bgcolor: "grey.900",
        color: "grey.300",
        overflow: "hidden",
        zIndex: (theme) => theme.zIndex.drawer + 1,
      }}
    >
      <Container maxWidth={false} sx={{ textAlign: "center", py: 0 }}>
        <Typography variant="caption" component="p" sx={{ m: 0, lineHeight: 1.2 }}>
          © {new Date().getFullYear()} Jade · Licensed under the{" "}
          <Link
            href="https://opensource.org/licenses/MIT"
            target="_blank"
            rel="noopener noreferrer"
            color="inherit"
            underline="hover"
            sx={{ opacity: 0.9 }}
          >
            MIT License
          </Link>
        </Typography>
      </Container>
    </Box>
  );
};

export default Footer;

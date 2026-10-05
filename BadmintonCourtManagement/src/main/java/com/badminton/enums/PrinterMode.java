package com.badminton.enums;

/**
 * How the bill reaches the receipt printer.
 */
public enum PrinterMode {
    /** Frontend renders the receipt and uses the OS print dialog (USB printers). */
    BROWSER,
    /** Backend pushes an ESC/POS raster job to the printer's LAN IP:port. */
    NETWORK
}

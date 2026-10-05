package com.badminton.core.billing.print;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Printer-neutral receipt layout — a flat list of styled lines. Rendered by
 * the ESC/POS raster printer and the PDF exporter, and mirrored by the FE
 * receipt view.
 */
public class ReceiptDocument {

    public enum Style {
        NORMAL, BOLD, TITLE, CENTER, CENTER_BOLD, SEPARATOR, SMALL
    }

    @Data
    @Builder
    public static class Line {
        /** Left column text (or the whole line for CENTER / TITLE / SEPARATOR). */
        private String left;
        /** Right-aligned column (amounts). */
        private String right;
        /** Sub-line shown indented under left (e.g. "2 x 150.000"). */
        private String sub;
        @Builder.Default
        private Style style = Style.NORMAL;
    }

    private final List<Line> lines = new ArrayList<>();

    public void add(Line line) {
        lines.add(line);
    }

    public void addSeparator() {
        add(Line.builder().style(Style.SEPARATOR).build());
    }

    public List<Line> getLines() {
        return lines;
    }
}

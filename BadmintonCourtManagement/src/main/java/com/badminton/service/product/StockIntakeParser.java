package com.badminton.service.product;

import com.badminton.enums.ImportAction;
import com.badminton.model.inventory.StockIntakeRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the {@code StockIntake} sheet used for bulk wholesale intake.
 * Columns: Tên | Số lượng | Giá nhập | Ngày nhập | Đơn vị (both trailing
 * columns optional - blank date defaults to today, blank unit defaults to the
 * item's package unit).
 * Invalid rows are returned with action {@link ImportAction#ERROR};
 * item resolution is left to the service layer.
 */
@Slf4j
@Component
public class StockIntakeParser {

    public static final String SHEET_NAME = "StockIntake";

    static final int HEADER_ROW_INDEX = 0;
    static final int MAX_ROWS = 1000;
    static final int MAX_NAME_LENGTH = 250;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    public List<StockIntakeRow> parse(InputStream inputStream) {
        Workbook workbook;
        try {
            workbook = WorkbookFactory.create(inputStream);
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to open stock intake workbook", e);
            throw new IllegalArgumentException("File không hợp lệ, chỉ hỗ trợ định dạng .xlsx");
        }

        List<StockIntakeRow> rows = new ArrayList<>();
        try (workbook) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                rows.add(errorRow(0, null, "Sheet '" + SHEET_NAME + "' không tồn tại trong file"));
                return rows;
            }
            parseSheet(sheet, workbook.getCreationHelper().createFormulaEvaluator(),
                    new DataFormatter(), rows);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không đọc được file Excel");
        }
        return rows;
    }

    private void parseSheet(Sheet sheet, FormulaEvaluator evaluator,
                            DataFormatter formatter, List<StockIntakeRow> rows) {
        int dataRowCount = 0;
        for (int i = HEADER_ROW_INDEX + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String name = readText(row.getCell(0), formatter, evaluator);
            String qtyText = readText(row.getCell(1), formatter, evaluator);
            String costText = readText(row.getCell(2), formatter, evaluator);
            String dateText = readText(row.getCell(3), formatter, evaluator);
            String unitText = readText(row.getCell(4), formatter, evaluator);
            int rowNumber = i + 1;

            if (isBlank(name) && isBlank(qtyText) && isBlank(costText)
                    && isBlank(dateText) && isBlank(unitText)) {
                continue;
            }
            if (dataRowCount >= MAX_ROWS) {
                rows.add(errorRow(rowNumber, name, "Sheet vượt quá " + MAX_ROWS + " dòng"));
                continue;
            }
            dataRowCount++;

            if (isBlank(name)) {
                rows.add(errorRow(rowNumber, null, "Tên sản phẩm trống"));
                continue;
            }
            name = name.trim();
            if (name.length() > MAX_NAME_LENGTH) {
                rows.add(errorRow(rowNumber, name, "Tên sản phẩm vượt quá " + MAX_NAME_LENGTH + " ký tự"));
                continue;
            }

            Integer quantity = readQuantity(row.getCell(1), evaluator);
            if (quantity == null || quantity <= 0) {
                rows.add(errorRow(rowNumber, name, "Số lượng không hợp lệ"));
                continue;
            }
            BigDecimal unitCost = readUnitCost(row.getCell(2), evaluator);
            if (unitCost == null || unitCost.signum() < 0) {
                rows.add(errorRow(rowNumber, name, "Giá nhập không hợp lệ"));
                continue;
            }

            LocalDate purchaseDate = readDate(row.getCell(3), evaluator);
            if (!isBlank(dateText) && purchaseDate == null) {
                rows.add(errorRow(rowNumber, name, "Ngày nhập không hợp lệ"));
                continue;
            }

            rows.add(StockIntakeRow.builder()
                    .rowNumber(rowNumber)
                    .name(name)
                    .quantity(quantity)
                    .unitCost(unitCost)
                    .purchaseDate(purchaseDate != null ? purchaseDate : LocalDate.now())
                    .unit(isBlank(unitText) ? null : unitText.trim())
                    .build());
        }
    }

    private String readText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        return formatter.formatCellValue(cell, evaluator);
    }

    private Integer readQuantity(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        try {
            return switch (evaluatedType(cell, evaluator)) {
                case NUMERIC -> (int) Math.round(cell.getNumericCellValue());
                case STRING -> Integer.valueOf(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    private BigDecimal readUnitCost(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        try {
            return switch (evaluatedType(cell, evaluator)) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> parseCostText(cell.getStringCellValue());
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    private LocalDate readDate(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        try {
            if (evaluatedType(cell, evaluator) == org.apache.poi.ss.usermodel.CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            }
            String text = cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                    ? cell.getStringCellValue()
                    : new DataFormatter().formatCellValue(cell, evaluator);
            if (text == null || text.isBlank()) {
                return null;
            }
            text = text.trim();
            for (DateTimeFormatter format : DATE_FORMATS) {
                try {
                    return LocalDate.parse(text, format);
                } catch (DateTimeParseException ignored) {
                }
            }
        } catch (RuntimeException e) {
            log.debug("Unparseable intake date cell at {}", cell.getAddress());
        }
        return null;
    }

    private org.apache.poi.ss.usermodel.CellType evaluatedType(Cell cell, FormulaEvaluator evaluator) {
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            try {
                return evaluator.evaluateFormulaCell(cell);
            } catch (RuntimeException e) {
                return org.apache.poi.ss.usermodel.CellType._NONE;
            }
        }
        return cell.getCellType();
    }

    /**
     * Accepts plain numbers and grouped values such as "25.000" or "25,000".
     */
    private BigDecimal parseCostText(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().replaceAll("\\s+", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ignored) {
        }
        try {
            return new BigDecimal(text.replace(".", "").replace(",", ""));
        } catch (NumberFormatException ignored) {
        }
        try {
            return new BigDecimal(text.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private StockIntakeRow errorRow(int rowNumber, String name, String message) {
        return StockIntakeRow.builder()
                .rowNumber(rowNumber)
                .name(name)
                .action(ImportAction.ERROR)
                .message(message)
                .build();
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}

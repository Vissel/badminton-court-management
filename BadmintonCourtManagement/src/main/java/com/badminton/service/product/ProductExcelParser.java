package com.badminton.service.product;

import com.badminton.enums.ImportAction;
import com.badminton.enums.ProductSheet;
import com.badminton.model.product.ProductImportRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses the product import workbook ({@code ShuttleBall} + {@code Service} sheets,
 * {@code Tên | Giá} columns) into validated rows.
 * Rows that fail parsing/validation are returned with action {@link ImportAction#ERROR};
 * business classification (ADD/UPDATE/REACTIVATE/SKIP) is left to the service layer.
 */
@Slf4j
@Component
public class ProductExcelParser {

    static final int HEADER_ROW_INDEX = 0;
    static final int MAX_ROWS_PER_SHEET = 1000;
    static final int MAX_NAME_LENGTH = 250;

    public List<ProductImportRow> parse(InputStream inputStream) {
        Workbook workbook;
        try {
            workbook = WorkbookFactory.create(inputStream);
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to open product import workbook", e);
            throw new IllegalArgumentException("File không hợp lệ, chỉ hỗ trợ định dạng .xlsx");
        }

        List<ProductImportRow> rows = new ArrayList<>();
        try (workbook) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (ProductSheet productSheet : ProductSheet.values()) {
                Sheet sheet = workbook.getSheet(productSheet.getSheetName());
                if (sheet == null) {
                    rows.add(errorRow(productSheet, 0, null,
                            "Sheet '" + productSheet.getSheetName() + "' không tồn tại trong file"));
                    continue;
                }
                parseSheet(productSheet, sheet, formatter, evaluator, rows);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Không đọc được file Excel");
        }
        return rows;
    }

    private void parseSheet(ProductSheet productSheet, Sheet sheet, DataFormatter formatter,
                            FormulaEvaluator evaluator, List<ProductImportRow> rows) {
        Set<String> seenNames = new HashSet<>();
        int dataRowCount = 0;

        for (int i = HEADER_ROW_INDEX + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String name = readText(row.getCell(0), formatter, evaluator);
            String costText = readText(row.getCell(1), formatter, evaluator);
            Float cost = readCost(row.getCell(1), formatter, evaluator);
            int rowNumber = i + 1; // 1-based for the response

            if (dataRowCount >= MAX_ROWS_PER_SHEET) {
                rows.add(errorRow(productSheet, rowNumber, name,
                        "Sheet vượt quá " + MAX_ROWS_PER_SHEET + " dòng"));
                continue;
            }

            // completely blank line - ignore silently
            if (isBlank(name) && isBlank(costText)) {
                continue;
            }
            dataRowCount++;

            if (isBlank(name)) {
                rows.add(errorRow(productSheet, rowNumber, null, "Tên sản phẩm trống"));
                continue;
            }
            name = name.trim();
            if (name.length() > MAX_NAME_LENGTH) {
                rows.add(errorRow(productSheet, rowNumber, name,
                        "Tên sản phẩm vượt quá " + MAX_NAME_LENGTH + " ký tự"));
                continue;
            }
            if (cost == null) {
                rows.add(errorRow(productSheet, rowNumber, name, "Giá không hợp lệ"));
                continue;
            }
            if (cost < 0) {
                rows.add(errorRow(productSheet, rowNumber, name, "Giá không được âm"));
                continue;
            }
            if (!seenNames.add(normalize(name))) {
                rows.add(errorRow(productSheet, rowNumber, name, "Tên bị trùng trong file"));
                continue;
            }

            rows.add(ProductImportRow.builder()
                    .sheet(productSheet)
                    .rowNumber(rowNumber)
                    .name(name)
                    .cost(cost)
                    .build());
        }
    }

    private String readText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        return formatter.formatCellValue(cell, evaluator);
    }

    private Float readCost(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> (float) cell.getNumericCellValue();
            case STRING -> parseCostText(cell.getStringCellValue());
            case FORMULA -> readFormulaCost(cell, formatter, evaluator);
            default -> null;
        };
    }

    private Float readFormulaCost(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        try {
            return switch (evaluator.evaluateFormulaCell(cell)) {
                case NUMERIC -> (float) cell.getNumericCellValue();
                case STRING -> parseCostText(formatter.formatCellValue(cell, evaluator));
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Accepts plain numbers and grouped values such as "25.000" or "25,000".
     */
    private Float parseCostText(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().replaceAll("\\s+", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Float.parseFloat(text.replace(".", "").replace(",", ""));
        } catch (NumberFormatException ignored) {
        }
        try {
            return Float.parseFloat(text.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ProductImportRow errorRow(ProductSheet sheet, int rowNumber, String name, String message) {
        return ProductImportRow.builder()
                .sheet(sheet)
                .rowNumber(rowNumber)
                .name(name)
                .action(ImportAction.ERROR)
                .message(message)
                .build();
    }

    public static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}

package com.badminton.service.impl;

import com.badminton.entity.ShuttleBall;
import com.badminton.enums.ImportAction;
import com.badminton.enums.ProductImportMode;
import com.badminton.enums.ProductSheet;
import com.badminton.model.product.ProductImportPlan;
import com.badminton.model.product.ProductImportRow;
import com.badminton.repository.ServiceRepository;
import com.badminton.repository.ShuttleBallRepository;
import com.badminton.response.product.ProductImportCounts;
import com.badminton.response.product.ProductImportPreviewResponse;
import com.badminton.response.product.ProductImportRowResponse;
import com.badminton.response.result.Result;
import com.badminton.service.ProductImportExportService;
import com.badminton.service.product.ProductExcelParser;
import com.badminton.service.product.ProductImportApplier;
import com.badminton.service.report.ProductExcelWriter;
import com.badminton.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProductImportExportServiceImpl implements ProductImportExportService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String EXPORT_FILE_PREFIX = "products_";
    private static final String XLSX_EXTENSION = ".xlsx";
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long IMPORT_TOKEN_TTL_MS = 30L * 60 * 1000;

    /**
     * Parsed import plans awaiting commit. Same in-memory cache pattern
     * as {@code ManagerController.exportCache}.
     */
    private final Map<String, ProductImportPlan> importCache = new ConcurrentHashMap<>();

    @Autowired
    private ShuttleBallRepository shuttleRepo;

    @Autowired
    private ServiceRepository serviceRepo;

    @Autowired
    private ProductExcelWriter productExcelWriter;

    @Autowired
    private ProductExcelParser productExcelParser;

    @Autowired
    private ProductImportApplier productImportApplier;

    @Override
    public void exportProducts(OutputStream outputStream) {
        List<ProductExcelWriter.ProductLine> shuttleLines = shuttleRepo.findAllByIsActive(true).stream()
                .map(b -> new ProductExcelWriter.ProductLine(b.getShuttleName(), b.getCost()))
                .toList();
        List<ProductExcelWriter.ProductLine> serviceLines = serviceRepo.findAllByIsActive(true).stream()
                .filter(s -> !ProductImportApplier.PROTECTED_SERVICE_NAMES.contains(s.getSerName()))
                .map(s -> new ProductExcelWriter.ProductLine(s.getSerName(), s.getCost()))
                .toList();
        productExcelWriter.write(shuttleLines, serviceLines, outputStream);
    }

    @Override
    public void exportTemplate(OutputStream outputStream) {
        productExcelWriter.write(List.of(), List.of(), outputStream);
    }

    @Override
    public String buildExportFileName() {
        return EXPORT_FILE_PREFIX + LocalDateTime.now(APP_ZONE).format(FILE_TIMESTAMP_FORMATTER) + XLSX_EXTENSION;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ProductImportPreviewResponse> previewImport(MultipartFile file, String mode) {
        Result<ProductImportPreviewResponse> result = new Result<>();
        try {
            ProductImportMode importMode = parseMode(mode);
            validateFile(file);

            List<ProductImportRow> rows;
            try (var inputStream = file.getInputStream()) {
                rows = productExcelParser.parse(inputStream);
            } catch (IOException e) {
                throw new IllegalArgumentException("Không đọc được file Excel");
            }

            classify(rows);

            String token = UUID.randomUUID().toString();
            importCache.put(token, new ProductImportPlan(
                    importMode,
                    rows,
                    collectNames(rows, ProductSheet.SHUTTLE_BALL),
                    collectNames(rows, ProductSheet.SERVICE),
                    System.currentTimeMillis()));

            result.setSuccess(true);
            result.setData(new ProductImportPreviewResponse(token, toRowResponses(rows), toCounts(rows)));
        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorCode(400);
            result.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Product import preview failed", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Server error.");
        }
        return result;
    }

    @Override
    public Result<Boolean> commitImport(String importToken, String mode) {
        Result<Boolean> result = new Result<>();
        try {
            if (importToken == null || importToken.isBlank()) {
                throw new IllegalArgumentException("importToken is required");
            }
            ProductImportPlan plan = importCache.get(importToken);
            if (plan == null) {
                throw new IllegalArgumentException("Import token không hợp lệ hoặc đã được sử dụng");
            }
            if (System.currentTimeMillis() - plan.getCreatedAt() > IMPORT_TOKEN_TTL_MS) {
                importCache.remove(importToken);
                throw new IllegalArgumentException("Import token đã hết hạn, vui lòng tải file lên lại");
            }
            if (mode != null && !mode.isBlank()) {
                plan.setMode(parseMode(mode));
            }
            // single-use token
            importCache.remove(importToken);
            productImportApplier.apply(plan);
            result.setSuccess(true);
            result.setData(Boolean.TRUE);
        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorCode(400);
            result.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            // applier transaction has already rolled back
            log.error("Product import commit failed", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Import thất bại, toàn bộ thay đổi đã được hủy");
        }
        return result;
    }

    // ------------------------------------------------------------------
    // classification
    // ------------------------------------------------------------------

    private void classify(List<ProductImportRow> rows) {
        Map<String, ShuttleBall> activeShuttles = new HashMap<>();
        Map<String, ShuttleBall> inactiveShuttles = new HashMap<>();
        for (ShuttleBall ball : shuttleRepo.findAll()) {
            (ball.isActive() ? activeShuttles : inactiveShuttles)
                    .putIfAbsent(ProductExcelParser.normalize(ball.getShuttleName()), ball);
        }

        Map<String, com.badminton.entity.Service> activeServices = new HashMap<>();
        Map<String, com.badminton.entity.Service> inactiveServices = new HashMap<>();
        for (com.badminton.entity.Service service : serviceRepo.findAll()) {
            (service.isActive() ? activeServices : inactiveServices)
                    .putIfAbsent(ProductExcelParser.normalize(service.getSerName()), service);
        }

        for (ProductImportRow row : rows) {
            if (row.getAction() == ImportAction.ERROR) {
                continue;
            }
            classifyRow(row, activeShuttles, inactiveShuttles, activeServices, inactiveServices);
        }
    }

    private void classifyRow(ProductImportRow row,
                             Map<String, ShuttleBall> activeShuttles,
                             Map<String, ShuttleBall> inactiveShuttles,
                             Map<String, com.badminton.entity.Service> activeServices,
                             Map<String, com.badminton.entity.Service> inactiveServices) {
        String key = ProductExcelParser.normalize(row.getName());

        if (row.getSheet() == ProductSheet.SERVICE
                && ProductImportApplier.PROTECTED_SERVICE_NAMES.stream()
                .anyMatch(p -> p.equalsIgnoreCase(row.getName()))) {
            row.setAction(ImportAction.ERROR);
            row.setMessage("Dịch vụ hệ thống, không được import");
            return;
        }

        Object active = row.getSheet() == ProductSheet.SHUTTLE_BALL
                ? activeShuttles.get(key) : activeServices.get(key);
        if (active != null) {
            float existingCost = active instanceof ShuttleBall ball
                    ? ball.getCost() : ((com.badminton.entity.Service) active).getCost();
            if (Float.compare(existingCost, row.getCost()) == 0) {
                row.setAction(ImportAction.SKIP);
                row.setMessage("Không thay đổi");
            } else {
                row.setAction(ImportAction.UPDATE);
                row.setMessage(MoneyUtils.formatToVND(existingCost) + " -> " + MoneyUtils.formatToVND(row.getCost()));
                row.setEntityRef(active);
            }
            return;
        }

        Object inactive = row.getSheet() == ProductSheet.SHUTTLE_BALL
                ? inactiveShuttles.get(key) : inactiveServices.get(key);
        if (inactive != null) {
            float existingCost = inactive instanceof ShuttleBall ball
                    ? ball.getCost() : ((com.badminton.entity.Service) inactive).getCost();
            row.setAction(ImportAction.REACTIVATE);
            row.setMessage(Float.compare(existingCost, row.getCost()) == 0
                    ? "Kích hoạt lại"
                    : "Kích hoạt lại, " + MoneyUtils.formatToVND(existingCost) + " -> " + MoneyUtils.formatToVND(row.getCost()));
            row.setEntityRef(inactive);
            return;
        }

        row.setAction(ImportAction.ADD);
        row.setMessage("Thêm mới");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import trống");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(XLSX_EXTENSION)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file .xlsx");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File vượt quá 5MB");
        }
    }

    private ProductImportMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return ProductImportMode.MERGE;
        }
        try {
            return ProductImportMode.valueOf(mode.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("mode không hợp lệ: " + mode + ". Giá trị cho phép: MERGE, REPLACE");
        }
    }

    private Set<String> collectNames(List<ProductImportRow> rows, ProductSheet sheet) {
        Set<String> names = new HashSet<>();
        for (ProductImportRow row : rows) {
            if (row.getSheet() == sheet && row.getAction() != ImportAction.ERROR) {
                names.add(ProductExcelParser.normalize(row.getName()));
            }
        }
        return names;
    }

    private List<ProductImportRowResponse> toRowResponses(List<ProductImportRow> rows) {
        List<ProductImportRowResponse> responses = new ArrayList<>(rows.size());
        for (ProductImportRow row : rows) {
            responses.add(new ProductImportRowResponse(
                    row.getRowNumber(),
                    row.getSheet().getSheetName(),
                    row.getName(),
                    toBigDecimal(row.getCost()),
                    row.getAction() == null ? null : row.getAction().name(),
                    row.getMessage()));
        }
        return responses;
    }

    private BigDecimal toBigDecimal(float cost) {
        if (cost == Math.floor(cost) && !Float.isInfinite(cost)) {
            return BigDecimal.valueOf((long) cost);
        }
        return BigDecimal.valueOf(cost);
    }

    private ProductImportCounts toCounts(List<ProductImportRow> rows) {
        ProductImportCounts counts = new ProductImportCounts();
        for (ProductImportRow row : rows) {
            if (row.getAction() == null) {
                continue;
            }
            switch (row.getAction()) {
                case ADD -> counts.setAdded(counts.getAdded() + 1);
                case UPDATE -> counts.setUpdated(counts.getUpdated() + 1);
                case REACTIVATE -> counts.setReactivated(counts.getReactivated() + 1);
                case SKIP -> counts.setSkipped(counts.getSkipped() + 1);
                case ERROR -> counts.setErrors(counts.getErrors() + 1);
            }
        }
        return counts;
    }
}

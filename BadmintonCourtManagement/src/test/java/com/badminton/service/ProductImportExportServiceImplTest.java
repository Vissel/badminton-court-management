package com.badminton.service;

import com.badminton.entity.ShuttleBall;
import com.badminton.enums.ProductImportMode;
import com.badminton.model.product.ProductImportPlan;
import com.badminton.repository.ServiceRepositoty;
import com.badminton.repository.ShuttleBallRepositoty;
import com.badminton.response.product.ProductImportPreviewResponse;
import com.badminton.response.product.ProductImportRowResponse;
import com.badminton.response.result.Result;
import com.badminton.service.impl.ProductImportExportServiceImpl;
import com.badminton.service.product.ProductExcelParser;
import com.badminton.service.product.ProductImportApplier;
import com.badminton.service.report.ProductExcelWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImportExportServiceImplTest {

    @Mock
    private ShuttleBallRepositoty shuttleRepo;

    @Mock
    private ServiceRepositoty serviceRepo;

    @Mock
    private ProductImportApplier applier;

    private ProductImportExportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductImportExportServiceImpl();
        ReflectionTestUtils.setField(service, "shuttleRepo", shuttleRepo);
        ReflectionTestUtils.setField(service, "serviceRepo", serviceRepo);
        ReflectionTestUtils.setField(service, "productExcelWriter", new ProductExcelWriter());
        ReflectionTestUtils.setField(service, "productExcelParser", new ProductExcelParser());
        ReflectionTestUtils.setField(service, "productImportApplier", applier);
    }

    // ------------------------------------------------------------------
    // preview
    // ------------------------------------------------------------------

    @Test
    void previewClassifiesAllActions() throws IOException {
        ShuttleBall activeSame = new ShuttleBall("SameCost", 25000);
        ShuttleBall activeDiff = new ShuttleBall("OldPrice", 25000);
        ShuttleBall inactive = new ShuttleBall("Inactive", 20000);
        inactive.setActive(false);
        when(shuttleRepo.findAll()).thenReturn(List.of(activeSame, activeDiff, inactive));

        com.badminton.entity.Service protectedService =
                new com.badminton.entity.Service("costInPerson", 30000);
        when(serviceRepo.findAll()).thenReturn(List.of(protectedService));

        byte[] xlsx = workbook(
                Map.of("ShuttleBall", List.of(
                        new String[]{"SameCost", "25000"},
                        new String[]{"OldPrice", "27000"},
                        new String[]{"Inactive", "22000"},
                        new String[]{"BrandNew", "30000"},
                        new String[]{"", "1000"},
                        new String[]{"Neg", "-5"},
                        new String[]{"Dup", "1000"},
                        new String[]{"Dup", "2000"}),
                        "Service", List.of(
                        new String[]{"costInPerson", "30000"},
                        new String[]{"Nước suối", "10000"})));
        MockMultipartFile file = new MockMultipartFile("file", "products.xlsx", null, xlsx);

        Result<ProductImportPreviewResponse> result = service.previewImport(file, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData().getImportToken());

        Map<String, ProductImportRowResponse> byName = result.getData().getRows().stream()
                .filter(r -> r.getName() != null)
                .collect(Collectors.toMap(ProductImportRowResponse::getName, r -> r, (a, b) -> a));

        assertEquals("SKIP", byName.get("SameCost").getAction());
        assertEquals("UPDATE", byName.get("OldPrice").getAction());
        assertTrue(byName.get("OldPrice").getMessage().contains("->"));
        assertEquals("REACTIVATE", byName.get("Inactive").getAction());
        assertEquals("ADD", byName.get("BrandNew").getAction());
        assertEquals("ERROR", byName.get("Neg").getAction());
        // "Dup" appears twice: first row classifies normally, second is a duplicate error
        List<String> dupActions = result.getData().getRows().stream()
                .filter(r -> "Dup".equals(r.getName()))
                .map(ProductImportRowResponse::getAction)
                .toList();
        assertEquals(List.of("ADD", "ERROR"), dupActions);
        assertEquals("ERROR", byName.get("costInPerson").getAction());
        assertEquals("ADD", byName.get("Nước suối").getAction());

        assertEquals(3, result.getData().getCounts().getAdded());
        assertEquals(1, result.getData().getCounts().getUpdated());
        assertEquals(1, result.getData().getCounts().getReactivated());
        assertEquals(1, result.getData().getCounts().getSkipped());
        assertEquals(4, result.getData().getCounts().getErrors());
    }

    @Test
    void previewRejectsNonXlsx() {
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", null, "a,b".getBytes());
        Result<ProductImportPreviewResponse> result = service.previewImport(file, null);
        assertFalse(result.isSuccess());
        assertEquals(400, result.getErrorCode());
    }

    @Test
    void previewRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "products.xlsx", null, new byte[0]);
        Result<ProductImportPreviewResponse> result = service.previewImport(file, null);
        assertFalse(result.isSuccess());
        assertEquals(400, result.getErrorCode());
    }

    @Test
    void previewRejectsInvalidMode() throws IOException {
        lenient().when(shuttleRepo.findAll()).thenReturn(List.of());
        lenient().when(serviceRepo.findAll()).thenReturn(List.of());
        byte[] xlsx = workbook(Map.of("ShuttleBall", List.of(), "Service", List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "products.xlsx", null, xlsx);

        Result<ProductImportPreviewResponse> result = service.previewImport(file, "BOGUS");
        assertFalse(result.isSuccess());
        assertEquals(400, result.getErrorCode());
    }

    // ------------------------------------------------------------------
    // commit
    // ------------------------------------------------------------------

    @Test
    void commitAppliesCachedPlanAndTokenIsSingleUse() throws IOException {
        when(shuttleRepo.findAll()).thenReturn(List.of());
        when(serviceRepo.findAll()).thenReturn(List.of());
        byte[] xlsx = workbook(Map.of(
                "ShuttleBall", List.<String[]>of(new String[]{"New", "1000"}),
                "Service", List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "products.xlsx", null, xlsx);
        String token = service.previewImport(file, null).getData().getImportToken();

        Result<Boolean> commit = service.commitImport(token, null);
        assertTrue(commit.isSuccess());
        assertEquals(Boolean.TRUE, commit.getData());

        ArgumentCaptor<ProductImportPlan> captor = ArgumentCaptor.forClass(ProductImportPlan.class);
        verify(applier).apply(captor.capture());
        assertEquals(ProductImportMode.MERGE, captor.getValue().getMode());

        // second commit with the same token must fail
        Result<Boolean> second = service.commitImport(token, null);
        assertFalse(second.isSuccess());
    }

    @Test
    void commitFailsWithUnknownToken() {
        Result<Boolean> result = service.commitImport("no-such-token", null);
        assertFalse(result.isSuccess());
        assertEquals(400, result.getErrorCode());
        verifyNoInteractions(applier);
    }

    @Test
    void commitHonoursModeOverride() throws IOException {
        when(shuttleRepo.findAll()).thenReturn(List.of());
        when(serviceRepo.findAll()).thenReturn(List.of());
        byte[] xlsx = workbook(Map.of(
                "ShuttleBall", List.<String[]>of(new String[]{"New", "1000"}),
                "Service", List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "products.xlsx", null, xlsx);
        String token = service.previewImport(file, null).getData().getImportToken();

        service.commitImport(token, "replace");

        ArgumentCaptor<ProductImportPlan> captor = ArgumentCaptor.forClass(ProductImportPlan.class);
        verify(applier).apply(captor.capture());
        assertEquals(ProductImportMode.REPLACE, captor.getValue().getMode());
    }

    // ------------------------------------------------------------------
    // export
    // ------------------------------------------------------------------

    @Test
    void exportProducesValidXlsxAndExcludesProtectedServices() {
        when(shuttleRepo.findAllByIsActive(true)).thenReturn(List.of(new ShuttleBall("VinaStar", 27000)));
        com.badminton.entity.Service drink = new com.badminton.entity.Service("Nước suối", 10000);
        com.badminton.entity.Service system = new com.badminton.entity.Service("rentByTime", 50000);
        when(serviceRepo.findAllByIsActive(true)).thenReturn(List.of(drink, system));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportProducts(out);
        byte[] bytes = out.toByteArray();

        assertTrue(bytes.length > 4);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }

    @Test
    void exportFileNameMatchesContract() {
        String name = service.buildExportFileName();
        assertTrue(name.matches("products_\\d{8}_\\d{6}\\.xlsx"), name);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Builds a real .xlsx workbook with the given sheets and data rows
     * (header row "Tên|Giá" is added automatically).
     */
    private byte[] workbook(Map<String, List<String[]>> sheets) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Map.Entry<String, List<String[]>> entry : sheets.entrySet()) {
                Sheet sheet = wb.createSheet(entry.getKey());
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Tên");
                header.createCell(1).setCellValue("Giá");
                int i = 1;
                for (String[] values : entry.getValue()) {
                    Row row = sheet.createRow(i++);
                    row.createCell(0).setCellValue(values[0]);
                    row.createCell(1).setCellValue(values[1]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}

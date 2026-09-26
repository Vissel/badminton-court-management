package com.badminton.service.impl;

import com.badminton.core.debit.CoreDebitReportService;
import com.badminton.enums.DebitReportMode;
import com.badminton.enums.DebitReportScope;
import com.badminton.enums.DebitReportSortDirection;
import com.badminton.enums.DebitReportSortField;
import com.badminton.model.report.DebitReportData;
import com.badminton.requestmodel.debit.DebitReportExportRequest;
import com.badminton.service.DebitReportService;
import com.badminton.service.report.DebitExcelReportWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DebitReportServiceImpl implements DebitReportService {

    private static final Set<String> ALLOWED_TIME_ZONES = Set.of(
            "Asia/Ho_Chi_Minh",
            "Asia/Hanoi",
            "Asia/Saigon",
            "Asia/Bangkok",
            "Asia/Singapore",
            "Asia/Tokyo",
            "Asia/Seoul",
            "Europe/London",
            "Europe/Paris",
            "America/New_York",
            "America/Los_Angeles",
            "UTC",
            "GMT",
            "GMT+7",
            "GMT+8"
    );

    @Autowired
    private CoreDebitReportService coreDebitReportService;

    @Autowired
    private DebitExcelReportWriter debitExcelReportWriter;

    @Override
    public String buildContentDisposition(DebitReportExportRequest request) {
        ExportContext ctx = parseAndValidate(request);
        return debitExcelReportWriter.buildContentDisposition(ctx.mode, java.time.Instant.now(), ctx.zoneId);
    }

    @Override
    public void exportToStream(DebitReportExportRequest request, OutputStream outputStream) {
        ExportContext ctx = parseAndValidate(request);
        DebitReportData data = coreDebitReportService.loadReportData(
                request, ctx.mode, ctx.scope, ctx.sortField, ctx.sortDirection, ctx.zoneId);

        debitExcelReportWriter.write(data, request, ctx.zoneId, outputStream);
    }

    private ExportContext parseAndValidate(DebitReportExportRequest request) {
        validateRequest(request);
        DebitReportMode mode = parseEnum(request.getMode(), DebitReportMode.class, "mode");
        DebitReportScope scope = parseEnum(request.getScope(), DebitReportScope.class, "scope");
        DebitReportSortField sortField = parseEnum(request.getSortField(), DebitReportSortField.class, "sortField");
        DebitReportSortDirection sortDirection = parseEnum(request.getSortDirection(), DebitReportSortDirection.class, "sortDirection");
        ZoneId zoneId = validateAndParseTimeZone(request.getTimeZone());
        validateScope(scope, request.getPlayerName());

        return new ExportContext(mode, scope, sortField, sortDirection, zoneId);
    }

    private void validateRequest(DebitReportExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (request.getMode() == null || request.getMode().isBlank()) {
            throw new IllegalArgumentException("mode is required");
        }
        if (request.getScope() == null || request.getScope().isBlank()) {
            throw new IllegalArgumentException("scope is required");
        }
        if (request.getSortField() == null || request.getSortField().isBlank()) {
            throw new IllegalArgumentException("sortField is required");
        }
        if (request.getSortDirection() == null || request.getSortDirection().isBlank()) {
            throw new IllegalArgumentException("sortDirection is required");
        }
        if (request.getFrom() == null || request.getFrom().isBlank()) {
            throw new IllegalArgumentException("from is required");
        }
        if (request.getTo() == null || request.getTo().isBlank()) {
            throw new IllegalArgumentException("to is required");
        }
    }

    private void validateScope(DebitReportScope scope, String playerName) {
        if (scope == DebitReportScope.PLAYER) {
            if (playerName == null || playerName.isBlank()) {
                throw new IllegalArgumentException("playerName is required when scope is PLAYER");
            }
        }
    }

    private ZoneId validateAndParseTimeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            throw new IllegalArgumentException("timeZone is required");
        }
        if (!ALLOWED_TIME_ZONES.contains(timeZone)) {
            throw new IllegalArgumentException("Unsupported timeZone: " + timeZone);
        }
        try {
            return ZoneId.of(timeZone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timeZone: " + timeZone);
        }
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            String allowed = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value + ". Allowed values: " + allowed);
        }
    }

    private record ExportContext(DebitReportMode mode,
                                 DebitReportScope scope,
                                 DebitReportSortField sortField,
                                 DebitReportSortDirection sortDirection,
                                 ZoneId zoneId) {
    }
}

package com.badminton.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.ByteArrayResource;

@Data
@AllArgsConstructor
public class ExportReportResult<R extends ByteArrayResource> {
    private R resource;
    private String fileName;
}

package com.badminton.service;

import com.badminton.requestmodel.debit.DebitReportExportRequest;

import java.io.OutputStream;

public interface DebitReportService {

    String buildContentDisposition(DebitReportExportRequest request);

    void exportToStream(DebitReportExportRequest request, OutputStream outputStream);
}

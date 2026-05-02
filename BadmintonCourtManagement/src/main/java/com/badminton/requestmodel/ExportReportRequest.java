package com.badminton.requestmodel;

import lombok.Data;

import java.util.List;

@Data
public class ExportReportRequest {
    List<Integer> sessionIds;
}

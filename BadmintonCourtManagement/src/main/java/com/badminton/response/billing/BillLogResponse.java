package com.badminton.response.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of the internal-control trail ({@code /bills/{id}/logs}).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillLogResponse {
    private Long logId;
    private String action;
    private String actor;
    private String ip;
    private String detail;
    private String error;
    private String createdAt;
}

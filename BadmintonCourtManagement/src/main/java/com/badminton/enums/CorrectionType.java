package com.badminton.enums;

/**
 * How a correction bill fixes an already-issued e-invoice (NĐ 254/2026):
 * an issued coded invoice is never "huỷ" — errors are repaired by a
 * correction bill that references the original.
 */
public enum CorrectionType {
    /** Điều chỉnh: the delta only; the original stays issued. */
    DIEU_CHINH,
    /** Thay thế: a full replacement; the original is voided. */
    THAY_THE
}

package com.badminton.model.product;

import com.badminton.enums.ImportAction;
import com.badminton.enums.ProductSheet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal representation of one parsed and classified product row.
 * For UPDATE / REACTIVATE actions {@link #entityRef} holds the managed entity to mutate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportRow {

    private ProductSheet sheet;

    /**
     * 1-based row number inside the sheet.
     */
    private int rowNumber;

    private String name;

    private float cost;

    private ImportAction action;

    private String message;

    /**
     * Existing entity to touch on commit: {@link com.badminton.entity.ShuttleBall}
     * or {@link com.badminton.entity.Service}. Null for ADD / SKIP / ERROR.
     */
    private Object entityRef;
}

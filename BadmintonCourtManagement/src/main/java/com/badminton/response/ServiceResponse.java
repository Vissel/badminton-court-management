package com.badminton.response;

import com.badminton.entity.InventoryItem;
import com.badminton.entity.Service;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.requestmodel.ResponseDTO;
import com.badminton.util.MoneyUtils;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ServiceResponse extends ResponseDTO {
    private String serviceName;
    private float cost;
    private String costFormat;
    private final String currency = MoneyUtils.CURRENCY_VN;

    /** Catalog row state: false → Ngừng bán. */
    private boolean active = true;

    /** Linked stock item id; null for pure pricing rows. */
    private Integer itemId;

    /** Current stock in base units; null when this is not a stockable item. */
    private Long stockOnHand;

    /** True when stockOnHand is non-null and <= 10. */
    private Boolean lowStock;

    /** True when the item is sold out (stockOnHand <= 0). */
    private boolean outOfStock;

    /**
     * Stock expressed with the package unit, e.g. "5 ống + 3 quả";
     * null when the item has no package config.
     */
    private String stockBreakdown;

    /** Base unit from Quản lý kho (quả, chai...); null when unlinked. */
    private String unit;

    private String packageUnit;

    private Integer unitsPerPackage;

    /** Sell price owned by Quản lý kho; null → "Thiếu giá bán". */
    private BigDecimal retailPrice;

    private String retailPriceFormat;

    public ServiceResponse(Service serviceEntity) {
        this.serviceName = serviceEntity.getSerName();
        this.cost = serviceEntity.getCost();
        this.costFormat = MoneyUtils.formatToVND(serviceEntity.getCost());
        this.active = serviceEntity.isActive();
        this.itemId = serviceEntity.getItem() != null ? serviceEntity.getItem().getItemId() : null;
        applyItem(serviceEntity.getItem());
    }

    public ServiceResponse(ServiceDTO serviceDTO) {
        this.serviceName = serviceDTO.getServiceName();
        this.cost = serviceDTO.getCost();
        this.costFormat = MoneyUtils.formatToVND(serviceDTO.getCost());
        this.itemId = serviceDTO.getItemId();
    }

    private void applyItem(InventoryItem item) {
        if (item == null) {
            return;
        }
        this.unit = item.getUnit();
        this.packageUnit = item.getPackageUnit();
        this.unitsPerPackage = item.getUnitsPerPackage();
        this.retailPrice = item.getRetailPrice();
        if (item.getRetailPrice() != null) {
            this.retailPriceFormat = MoneyUtils.formatToVND(item.getRetailPrice().doubleValue());
        }
    }
}

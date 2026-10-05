package com.badminton.service.impl;

import com.badminton.core.billing.CoreBillingService;
import com.badminton.exception.BusinessException;
import com.badminton.requestmodel.billing.BillConfigRequest;
import com.badminton.requestmodel.billing.BillListRequest;
import com.badminton.requestmodel.billing.VoidBillRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.billing.BillConfigResponse;
import com.badminton.response.billing.BillResponse;
import com.badminton.response.billing.ReceiptResponse;
import com.badminton.response.result.Result;
import com.badminton.service.BillingService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class BillingServiceImpl implements BillingService {

    @Autowired
    private ServiceTemplate serviceTemple;

    @Autowired
    private CoreBillingService coreBillingService;

    @Override
    public Result<PageResponse<BillResponse>> listBills(BillListRequest request) {
        return serviceTemple.execute(new ProcessCallback<BillListRequest, PageResponse<BillResponse>>() {
            @Override
            public BillListRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(BillListRequest request) {
                Assert.notNull(request, "Request must not be null");
            }

            @Override
            public PageResponse<BillResponse> process() {
                return coreBillingService.searchBills(getRequest());
            }
        });
    }

    @Override
    public Result<BillResponse> getBill(Long billId) {
        return serviceTemple.execute(new ProcessCallback<Long, BillResponse>() {
            @Override
            public Long getRequest() {
                return billId;
            }

            @Override
            public void preProcess(Long request) {
                Assert.notNull(request, "Bill id must not be null");
            }

            @Override
            public BillResponse process() throws BusinessException {
                return coreBillingService.getBill(getRequest());
            }
        });
    }

    @Override
    public Result<ReceiptResponse> getReceipt(Long billId) {
        return serviceTemple.execute(new ProcessCallback<Long, ReceiptResponse>() {
            @Override
            public Long getRequest() {
                return billId;
            }

            @Override
            public void preProcess(Long request) {
                Assert.notNull(request, "Bill id must not be null");
            }

            @Override
            public ReceiptResponse process() throws BusinessException {
                return coreBillingService.getReceipt(getRequest());
            }
        });
    }

    @Override
    public Result<BillResponse> voidBill(Long billId, VoidBillRequest request) {
        return serviceTemple.execute(new ProcessCallback<VoidBillRequest, BillResponse>() {
            @Override
            public VoidBillRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(VoidBillRequest request) {
                Assert.notNull(request, "Request must not be null");
                Assert.isTrue(StringUtils.isNotBlank(request.getReason()), "Void reason must not be blank");
            }

            @Override
            public BillResponse process() throws BusinessException {
                return coreBillingService.voidBill(billId, getRequest().getReason());
            }
        });
    }

    @Override
    public Result<BillResponse> printBill(Long billId, String channel) {
        return serviceTemple.execute(new ProcessCallback<Long, BillResponse>() {
            @Override
            public Long getRequest() {
                return billId;
            }

            @Override
            public void preProcess(Long request) {
                Assert.notNull(request, "Bill id must not be null");
            }

            @Override
            public BillResponse process() throws BusinessException {
                return coreBillingService.printBill(getRequest(), channel);
            }
        });
    }

    @Override
    public Result<Void> testPrinter() {
        return serviceTemple.execute(new ProcessCallback<Void, Void>() {
            @Override
            public Void getRequest() {
                return null;
            }

            @Override
            public void preProcess(Void request) {
            }

            @Override
            public Void process() throws BusinessException {
                coreBillingService.testPrinter();
                return null;
            }
        });
    }

    @Override
    public Result<BillConfigResponse> getConfig() {
        return serviceTemple.execute(new ProcessCallback<Void, BillConfigResponse>() {
            @Override
            public Void getRequest() {
                return null;
            }

            @Override
            public void preProcess(Void request) {
            }

            @Override
            public BillConfigResponse process() {
                return coreBillingService.getConfig();
            }
        });
    }

    @Override
    public Result<BillConfigResponse> updateConfig(BillConfigRequest request) {
        return serviceTemple.execute(new ProcessCallback<BillConfigRequest, BillConfigResponse>() {
            @Override
            public BillConfigRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(BillConfigRequest request) {
                Assert.notNull(request, "Request must not be null");
            }

            @Override
            public BillConfigResponse process() {
                return coreBillingService.updateConfig(getRequest());
            }
        });
    }
}

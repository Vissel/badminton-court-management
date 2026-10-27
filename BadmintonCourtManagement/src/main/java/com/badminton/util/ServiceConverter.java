package com.badminton.util;

import com.badminton.model.dto.ServiceDTO;
import com.badminton.requestmodel.ServiceRequest;

public class ServiceConverter {
    public static ServiceDTO convertRequestToDTO(ServiceRequest request) {
        ServiceDTO dto = new ServiceDTO();
        dto.setServiceName(request.getServiceName());
        dto.setCost(Float.valueOf(request.getCost()));
        dto.setItemId(request.getItemId());
        dto.setQuantity(request.getQuantity());
        return dto;
    }

    public static ServiceRequest buildServiceRequest(String serName, float cost) {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setServiceName(serName);
        serviceRequest.setCost(String.valueOf(cost));
        return serviceRequest;
    }
}

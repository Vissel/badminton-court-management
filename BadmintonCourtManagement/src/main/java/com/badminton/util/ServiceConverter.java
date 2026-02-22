package com.badminton.util;

import com.badminton.model.dto.ServiceDTO;
import com.badminton.requestmodel.ServiceRequest;

public class ServiceConverter {
    public static ServiceDTO convertRequestToDTO(ServiceRequest request) {
        ServiceDTO dto = new ServiceDTO();
        dto.setServiceName(request.getServiceName());
        dto.setCost(Float.valueOf(request.getCost()));
        return dto;
    }
}

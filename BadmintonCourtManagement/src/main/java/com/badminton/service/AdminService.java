package com.badminton.service;

import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.SetUpServiceRequest;
import com.badminton.response.SetUpServiceResponse;

public interface AdminService {

    public SetUpServiceResponse getSetUpService();

    boolean setUpService(SetUpServiceDTO setupServiceDTO);

    boolean updateSetUpService(SetUpServiceRequest setupServiceDTO);

    public boolean deleteService(ServiceDTO serviceDTO);

    public boolean deleteShuttleBall(ShuttleBallDTO shuttleBallDTO);
}

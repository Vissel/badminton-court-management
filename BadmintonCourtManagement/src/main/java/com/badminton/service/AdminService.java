package com.badminton.service;

import com.badminton.requestmodel.ServiceDTO;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.SetUpServiceRequest;
import com.badminton.requestmodel.ShuttleBallDTO;
import com.badminton.response.SetUpServiceResponse;

public interface AdminService {

    public SetUpServiceResponse getSetUpService();

    boolean setUpService(SetUpServiceDTO setupServiceDTO);

    boolean updateSetUpService(SetUpServiceRequest setupServiceDTO);

    public boolean deleteService(ServiceDTO serviceDTO);

    public boolean deleteShuttleBall(ShuttleBallDTO shuttleBallDTO);
}

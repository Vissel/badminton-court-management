package com.badminton.service;

import com.badminton.requestmodel.ServiceDTO;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.ShuttleBallDTO;

public interface AdminService {

	public SetUpServiceDTO getSetUpService();

	public boolean setUpService(SetUpServiceDTO setupServiceDTO);

	public boolean deleteService(ServiceDTO serviceDTO);

	public boolean deleteShuttleBall(ShuttleBallDTO shuttleBallDTO);
}

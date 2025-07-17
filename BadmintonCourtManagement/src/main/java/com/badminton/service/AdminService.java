package com.badminton.service;

import com.badminton.requestmodel.SetUpServiceDTO;

public interface AdminService {

	public SetUpServiceDTO getSetUpService();

	public boolean setUpService(SetUpServiceDTO setupServiceDTO);
}

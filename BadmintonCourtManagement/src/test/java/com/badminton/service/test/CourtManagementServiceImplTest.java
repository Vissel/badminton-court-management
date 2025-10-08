package com.badminton.service.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.badminton.requestmodel.CourtManagementDTO;
import com.badminton.service.CourtServicesServiceImpl;

@SpringBootTest
public class CourtManagementServiceImplTest {

	@Autowired
	private CourtServicesServiceImpl service;

	@Test
	public void testGetCourtManagement() {
		CourtManagementDTO res = service.getCourtManagement();
		Assertions.assertTrue(res != null);
//		Assertions.assertTrue(!res.getGames().isEmpty());
//		Assertions.assertTrue(!res.getRemainPlayers().isEmpty());

	}
}

package com.badminton.service.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import com.badminton.entity.Session;
import com.badminton.service.SessionServiceImpl;

@SpringBootTest
public class SessionServiceImplTest {

	@Autowired
	SessionServiceImpl sessionService;

	@Test
	public void testFindListCurrentSession() {
		List<Session> listSession = sessionService.findListCurrentSession();
		assertThat(!listSession.isEmpty());

	}

	@Test
	public void testCheckAvailableSession() {
		Assert.isTrue(sessionService.checkAvailableSession(), "Check available must be true");
	}
}

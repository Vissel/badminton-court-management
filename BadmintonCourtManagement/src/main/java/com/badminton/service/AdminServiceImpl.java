package com.badminton.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badminton.constant.CommonConstant;
import com.badminton.entity.Court;
import com.badminton.entity.ShuttleBall;
import com.badminton.repository.CourtRepositoty;
import com.badminton.repository.ServiceRepositoty;
import com.badminton.repository.ShuttleBallRepositoty;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.ShuttleBallDTO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

	@Autowired
	private CourtRepositoty courtRepo;
	@Autowired
	private ShuttleBallRepositoty shuttleRepo;
	@Autowired
	private ServiceRepositoty serviceRepo;

	private static final String COURT_STR = "Court ";

	@Override
	public boolean setUpService(SetUpServiceDTO setupServiceDTO) {
		try {

			checkAndCreateCourt(setupServiceDTO.getTotalCourt());

			// save shuttle infor
			if (!setupServiceDTO.getShuttleBalls().isEmpty()) {
				ShuttleBallDTO ballDTO = setupServiceDTO.getShuttleBalls().get(0);
				shuttleRepo.save(new ShuttleBall(ballDTO.getShuttleName(), ballDTO.getShuttleCost()));
			}

			if (!setupServiceDTO.getServices().isEmpty()) {
				// save services
				List<com.badminton.entity.Service> listServices = setupServiceDTO.getServices().stream()
						.map(serDTO -> new com.badminton.entity.Service(serDTO.getServiceName(), serDTO.getCost()))
						.collect(Collectors.toList());
				serviceRepo.saveAll(listServices);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void checkAndCreateCourt(int totalCourt) {
		List<Court> listCourt = courtRepo.findAll();
		int differentNumber = CommonConstant.INT_ZERO;
		if (listCourt.size() > totalCourt) {
			// remove
			differentNumber = listCourt.size() - totalCourt;
			int num = 0;
			int i = listCourt.size() - 1;
			while (num < differentNumber) {
				listCourt.get(i).setActive(false);
				num++;
			}
			courtRepo.saveAll(listCourt);
		} else if (listCourt.size() < totalCourt) {
			differentNumber = totalCourt - listCourt.size();
			addOrRemoveCourts(differentNumber);
		}
		log.info("Saving {} court.", differentNumber);
	}

	private void addOrRemoveCourts(int differentNumber) {
		// create courts
		List<Court> courts = new ArrayList<>();
		for (int i = 0; i < differentNumber; i++) {
			courts.add(new Court(COURT_STR + String.valueOf(i + 1)));
		}
		courtRepo.saveAll(courts);
	}

	@Override
	public SetUpServiceDTO getSetUpService() {
		SetUpServiceDTO result = null;
		int totalCourt = courtRepo.findAllByIsActive(true).size();
		List<ShuttleBall> listShuttleBall = shuttleRepo.findAllByIsActive(true);
		List<com.badminton.entity.Service> listService = serviceRepo.findAll();
		if (totalCourt != 0 || listShuttleBall.size() != 0 || listService.size() != 0) {
			result = new SetUpServiceDTO();
			result.convertToDTO(totalCourt, listShuttleBall, listService);
		}
		return result;
	}

}

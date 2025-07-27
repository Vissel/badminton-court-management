package com.badminton.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.badminton.constant.ApiConstant;
import com.badminton.constant.CommonConstant;
import com.badminton.entity.Court;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.repository.CourtRepositoty;
import com.badminton.repository.ServiceRepositoty;
import com.badminton.repository.ShuttleBallRepositoty;
import com.badminton.requestmodel.ServiceDTO;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.ShuttleBallDTO;

import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
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
				if (!ballDTO.getShuttleName().isBlank() && ballDTO.getShuttleCost() != 0.0f) {
					shuttleRepo.save(new ShuttleBall(ballDTO.getShuttleName(), ballDTO.getShuttleCost()));
				}
			}

			if (!setupServiceDTO.getServices().isEmpty()) {
				// save services
				List<Service> listServices = setupServiceDTO.getServices().stream()
						.filter(s -> !s.getServiceName().isBlank() && s.getCost() != 0.0f)
						.map(serDTO -> new Service(serDTO.getServiceName(), serDTO.getCost()))
						.collect(Collectors.toList());
				serviceRepo.saveAll(listServices);
			}
			if (setupServiceDTO.getCostInPerson() != 0) {
				Optional<Service> optSer = serviceRepo.findBySerName(ApiConstant.COST_IN_PERNSON);
//				listServices.add(new (ApiConstant.COST_IN_PERNSON,
//						setupServiceDTO.getCostInPerson()));
				Service savedService;
				if (optSer.isPresent()) {
					savedService = optSer.get();
					savedService.setCost(setupServiceDTO.getCostInPerson());
				} else {
					savedService = new Service(ApiConstant.COST_IN_PERNSON, setupServiceDTO.getCostInPerson());
				}
				serviceRepo.save(savedService);
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
		List<com.badminton.entity.Service> listService = serviceRepo.findAllByIsActive(true);
		if (totalCourt != 0 || listShuttleBall.size() != 0 || listService.size() != 0) {
			result = new SetUpServiceDTO();
			result.convertToDTO(totalCourt, listShuttleBall, listService);
		}
		return result;
	}

	@Override
	public boolean deleteService(ServiceDTO serviceDTO) {
		Optional<Service> optSer = serviceRepo.findBySerName(serviceDTO.getServiceName());
		if (optSer.isPresent()) {
			optSer.get().setActive(false);
			return !serviceRepo.save(optSer.get()).isActive();
		}
		return false;
	}

	@Override
	public boolean deleteShuttleBall(ShuttleBallDTO shuttleBallDTO) {
		Optional<ShuttleBall> optBall = shuttleRepo.findByShuttleNameAndIsActive(shuttleBallDTO.getShuttleName(), true);
		if (optBall.isPresent()) {
			optBall.get().setActive(false);
			return !shuttleRepo.save(optBall.get()).isActive();
		}
		return false;
	}
}

package com.badminton.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.badminton.SecurityConfig;
import com.badminton.constant.ApiConstant;
import com.badminton.constant.CommonConstant;
import com.badminton.entity.Court;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.exception.GlobalExceptionHandler;
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

	private final SecurityConfig securityConfig;

	private final UrlBasedCorsConfigurationSource corsConfigurationSource;

	private final GlobalExceptionHandler globalExceptionHandler;

	@Autowired
	private CourtRepositoty courtRepo;
	@Autowired
	private ShuttleBallRepositoty shuttleRepo;
	@Autowired
	private ServiceRepositoty serviceRepo;

	private static final String COURT_STR = "Court ";

	AdminServiceImpl(GlobalExceptionHandler globalExceptionHandler,
			UrlBasedCorsConfigurationSource corsConfigurationSource, SecurityConfig securityConfig) {
		this.globalExceptionHandler = globalExceptionHandler;
		this.corsConfigurationSource = corsConfigurationSource;
		this.securityConfig = securityConfig;
	}

	@Override
	public boolean setUpService(SetUpServiceDTO setupServiceDTO) {
		try {

			checkAndCreateCourt(setupServiceDTO.getTotalCourt());

			// save shuttle infor
			if (!setupServiceDTO.getShuttleBalls().isEmpty()) {
				ShuttleBallDTO ballDTO = setupServiceDTO.getShuttleBalls().get(0);
				if (!ballDTO.getShuttleName().isBlank()) {
					ShuttleBall savedBall = findAddedShuttleBall(ballDTO.getShuttleName(), ballDTO.getShuttleCost());
					if (savedBall != null) {
						shuttleRepo.save(savedBall);
					}
				}
			}

			if (!setupServiceDTO.getServices().isEmpty()) {

				// saved services
				List<Service> listServices = setupServiceDTO.getServices().stream()
						.map(serDTO -> findAddedService(serDTO.getServiceName(), serDTO.getCost()))
						.filter(Objects::nonNull).collect(Collectors.toList());
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

	private ShuttleBall findAddedShuttleBall(String shuttleName, float shuttleCost) {
		List<ShuttleBall> shuttleBalls = shuttleRepo.findAllByShuttleName(shuttleName);
		// not existed => create new one => return
		if (shuttleBalls.isEmpty()) {
			return new ShuttleBall(shuttleName, shuttleCost);
		}

		List<ShuttleBall> list = shuttleBalls.stream().filter(b -> !b.isActive()).collect(Collectors.toList());
		// new cost => return
		if (!list.isEmpty()) {
			list.get(0).setShuttleName(shuttleName);
			list.get(0).setCost(shuttleCost);
			list.get(0).setActive(true);
			return list.get(0);
		}
		// existing shuttle ball with the same name and cost => return null
		return new ShuttleBall(shuttleName, shuttleCost);
	}

	public Service findAddedService(String serviceName, float serviceCost) {
		if (!serviceName.isBlank()) {
			List<Service> dbService = serviceRepo.findAllBySerName(serviceName);
			if (dbService.isEmpty()) {
				return new Service(serviceName, serviceCost);
			}
			// Reuse service by getting the same name but inactive, including equals and not
			// equals cost
			List<Service> list = dbService.stream().filter(s -> !s.isActive()).collect(Collectors.toList());

			if (!list.isEmpty()) {
				list.get(0).setSerName(serviceName);
				list.get(0).setCost(serviceCost);
				list.get(0).setActive(true);
				return list.get(0);
			}
			return new Service(serviceName, serviceCost);
		}
		return null;
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
		List<Service> services = serviceRepo.findAllBySerName(serviceDTO.getServiceName());
		if (!services.isEmpty()) {
			Service equService = services.stream().filter(s -> s.getSerName().equals(serviceDTO.getServiceName()))
					.findFirst().orElse(null);
			if (equService != null) {
				equService.setActive(false);
				return !serviceRepo.save(equService).isActive();
			}
		}
		return false;
	}

	@Override
	public boolean deleteShuttleBall(ShuttleBallDTO shuttleBallDTO) {
		List<ShuttleBall> balls = shuttleRepo.findAllByShuttleName(shuttleBallDTO.getShuttleName());
		if (!balls.isEmpty()) {
			ShuttleBall equBall = balls.stream().filter(b -> b.isActive()).findFirst().orElse(null);
			if (equBall != null) {
				equBall.setActive(false);
				return !shuttleRepo.save(equBall).isActive();
			}
		}
		return false;
	}
}
package com.badminton.service;

import com.badminton.SecurityConfig;
import com.badminton.constant.ApiConstant;
import com.badminton.constant.CommonConstant;
import com.badminton.entity.Court;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.exception.GlobalExceptionHandler;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.CourtRepositoty;
import com.badminton.repository.ServiceRepositoty;
import com.badminton.repository.ShuttleBallRepositoty;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.SetUpServiceRequest;
import com.badminton.response.SetUpServiceResponse;
import com.badminton.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Collectors;

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
            if (setupServiceDTO.getCostInPerson() != MoneyUtils.DEFAULT) {
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

    @Override
    public boolean updateSetUpService(SetUpServiceRequest setupServiceRequest) {
        try {

//            checkAndCreateCourt(setupServiceRequest.getTotalCourt());

            // save shuttle infor
            updateShuttleBalls(setupServiceRequest);
            updateServices(setupServiceRequest);

            if (setupServiceRequest.getCostInPerson() != MoneyUtils.DEFAULT) {
                Optional<Service> optSer = serviceRepo.findBySerName(ApiConstant.COST_IN_PERNSON);
                Service savedService;
                if (optSer.isPresent()) {
                    savedService = optSer.get();
                    savedService.setCost(setupServiceRequest.getCostInPerson());
                } else {
                    savedService = new Service(ApiConstant.COST_IN_PERNSON, setupServiceRequest.getCostInPerson());
                }
                serviceRepo.save(savedService);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateServices(SetUpServiceRequest setupServiceRequest) {
        List<Service> actives = serviceRepo.findAllByIsActive(true);
        List<Service> deActives = serviceRepo.findAllByIsActive(false);

        Set<Service> existedList = new LinkedHashSet<>();
        List<Service> newList = new ArrayList<>();

        Service addedSer = null;
        // add => find the same one in DB deactive list, the remaining will be created
        for (ServiceDTO dto : setupServiceRequest.getAddedServices()) {
            addedSer = deActives.stream()
                    .filter(s -> s.isTheSame(dto)).map(Service::setActiveService).findFirst().orElse(null);
            existedList.add(addedSer);
            if (addedSer == null) {
                newList.add(new Service(dto.getServiceName(), dto.getCost()));
            }
        }
        // delete => find same one in DB active list, remaining will do nothing.
        for (ServiceDTO dto : setupServiceRequest.getDeletedServices()) {
            existedList.add(actives.stream().filter(s -> s.isTheSame(dto))
                    .map(Service::setDeActiveService).findFirst().orElse(null));
        }
        existedList.remove(null);
        // save new list
        serviceRepo.saveAll(newList);
        // save existed list
        serviceRepo.saveAll(existedList);
    }

    private void updateShuttleBalls(SetUpServiceRequest setupServiceRequest) {
        List<ShuttleBall> actives = shuttleRepo.findAllByIsActive(true);
        List<ShuttleBall> deActives = shuttleRepo.findAllByIsActive(false);

        Set<ShuttleBall> existedList = new LinkedHashSet<>();
        List<ShuttleBall> newList = new ArrayList<>();
        ShuttleBall addedBall = null;
        // add => find the same one in DB deactive list, the remaining will be created
        for (ShuttleBallDTO dto : setupServiceRequest.getAddedShuttleBalls()) {
            addedBall = deActives.stream()
                    .filter(b -> b.theSameDTO(dto)).map(ShuttleBall::setActiveBall).findFirst().orElse(null);
            existedList.add(addedBall);
            if (addedBall == null) {
                newList.add(new ShuttleBall(dto.getShuttleName(), dto.getShuttleCost()));
            }
        }
        // delete => find same one in DB active list, remaining will do nothing.
        for (ShuttleBallDTO dto : setupServiceRequest.getDeletedShuttleBalls()) {
            existedList.add(actives.stream().filter(b -> b.theSameDTO(dto))
                    .map(ShuttleBall::setDeActiveBall).findFirst().orElse(null));
        }
        existedList.remove(null);
        // save new list
        shuttleRepo.saveAll(newList);
        // save existed list
        shuttleRepo.saveAll(existedList);
    }

    private List<ShuttleBallDTO> findTheSameBallList(List<ShuttleBallDTO> originList, List<ShuttleBall> findList) {
        return originList.stream().filter(dto ->
                        findList.stream().anyMatch(b -> b.theSameDTO(dto)))
                .collect(Collectors.toList());
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
    public SetUpServiceResponse getSetUpService() {
        SetUpServiceResponse result = null;
        int totalCourt = courtRepo.findAllByIsActive(true).size();
        List<ShuttleBall> listShuttleBall = shuttleRepo.findAllByIsActive(true);
        List<com.badminton.entity.Service> listService = serviceRepo.findAllByIsActive(true);
        if (totalCourt != 0 || listShuttleBall.size() != 0 || listService.size() != 0) {
            result = new SetUpServiceResponse(totalCourt, listShuttleBall, listService);
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
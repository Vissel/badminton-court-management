package com.badminton.controller;

import com.badminton.requestmodel.ServiceDTO;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.SetUpServiceRequest;
import com.badminton.requestmodel.ShuttleBallDTO;
import com.badminton.response.SetUpServiceResponse;
import com.badminton.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SettingsController {

    @Autowired
    AdminService adminService;

    @GetMapping(value = "/getSetupServices")
    public ResponseEntity<SetUpServiceResponse> getSetupService() {
        SetUpServiceResponse setupServiceRes = adminService.getSetUpService();
        return ResponseEntity.ok().body(setupServiceRes);

    }

    @PostMapping(value = "/addSetupService")
    public ResponseEntity<?> addSetupService(@RequestBody SetUpServiceDTO setUpServiceDTO) {
        if (adminService.setUpService(setUpServiceDTO)) {
            return ResponseEntity.ok().body("");

        }
        return ResponseEntity.badRequest().body("");
    }

    @PostMapping(value = "/updateSetupService")
    public ResponseEntity<?> updateSetupService(@RequestBody SetUpServiceRequest setUpServiceRequest) {
        if (adminService.updateSetUpService(setUpServiceRequest)) {
            return ResponseEntity.ok().body("");

        }
        return ResponseEntity.badRequest().body("");
    }

    @PutMapping(value = "/deleteService")
    public ResponseEntity<String> deleteService(@RequestBody ServiceDTO serviceDTO) {
        if (adminService.deleteService(serviceDTO)) {
            return ResponseEntity.ok().body("");

        }
        return ResponseEntity.badRequest().body("");
    }

    @PutMapping(value = "/deleteShuttleBall")
    public ResponseEntity<String> deleteShuttleBall(@RequestBody ShuttleBallDTO shuttleBallDTO) {
        if (adminService.deleteShuttleBall(shuttleBallDTO)) {
            return ResponseEntity.ok().body("");

        }
        return ResponseEntity.badRequest().body("");
    }
}

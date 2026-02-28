package com.badminton.controller;

import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.response.result.Result;
import com.badminton.service.CourtServicesServiceImpl;
import com.badminton.service.UserService;
import com.badminton.util.ResponseConvertor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/internal")
public class SuperAdminController {

    @Autowired
    UserService userService;
    @Autowired
    CourtServicesServiceImpl courtService;

    @PostMapping("/registerUser")
    public ResponseEntity<String> registerUser(@RequestBody RegisterUserDTO userDTO) {

        if (userService.saveAdminUser(userDTO)) {
            return ResponseEntity.ok(new String("Register user " + userDTO.getUserName() + " successfully."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new String("Registering got failure!!!"));
    }

    @PostMapping(value = "/removeAvaPlayerOutSession")
    public ResponseEntity<Result<Boolean>> removeAvaPlayerOutSession(@RequestBody String playerName) {
        Result<Boolean> res = courtService
                .removeAvaPlayerOutSession(playerName);
        return ResponseConvertor.convert(res);
    }

    @PostMapping("/createAdminUser")
    public ResponseEntity<Result<?>> createUser(@RequestBody RegisterUserDTO requestCreateUser) {
        boolean created = userService.createAdminUser(requestCreateUser);
        if (created) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.status(410).body(null);
    }
}

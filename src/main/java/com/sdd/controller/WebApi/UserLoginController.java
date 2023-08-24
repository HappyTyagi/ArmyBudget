package com.sdd.controller.WebApi;


import com.sdd.request.LoginRequest;
import com.sdd.response.ApiResponse;
import com.sdd.response.LoginResponse;
import com.sdd.service.UserAccountServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/user")
public class UserLoginController {


    @Autowired
    private UserAccountServices userAccountServices;



    @PostMapping("/loginWebApi")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest){
        return new  ResponseEntity<>(userAccountServices.login(loginRequest),HttpStatus.OK);
    }




}

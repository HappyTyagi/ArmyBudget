package com.sdd.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sdd.entities.HrData;
import com.sdd.entities.repository.HrDataRepository;
import com.sdd.exception.SDDException;
import com.sdd.jwt.HeaderUtils;
import com.sdd.jwt.JwtUtils;
import com.sdd.request.LoginRequest;
import com.sdd.response.ApiResponse;
import com.sdd.response.LoginResponse;
import com.sdd.service.UserAccountServices;
import com.sdd.utils.ConverterUtils;
import com.sdd.utils.HelperUtils;
import com.sdd.utils.ResponseUtils;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;



@Service
@AllArgsConstructor
public class UserAccountServiceImpl implements UserAccountServices {


    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private HrDataRepository hrDataRepository;

    @Autowired
    private HeaderUtils headerUtils;


    @Override
    public ApiResponse<LoginResponse> login(LoginRequest loginRequest) {


        if (loginRequest.getUserId() == null || loginRequest.getUserId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "THAT'S NOT THE RIGHT USERNAME OR PASSWORD. PLEASE TRY AGAIN");
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "THAT'S NOT THE RIGHT USERNAME OR PASSWORD. PLEASE TRY AGAIN");
        }
        HrData user = hrDataRepository.findByUserName(loginRequest.getUserId());


        if (user == null) {
            throw new SDDException(HttpStatus.NOT_FOUND.value(), "THAT'S NOT THE RIGHT USERNAME OR PASSWORD. PLEASE TRY AGAIN");
        }

        String encrypt = ConverterUtils.encrypt("NAPR", loginRequest.getPassword());
        System.out.println("encrypt password:-  " + encrypt);
        if (!(encrypt.equalsIgnoreCase(user.getPassword()))) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "THAT'S NOT THE RIGHT USERNAME OR PASSWORD. PLEASE TRY AGAIN");
        }
//        if (!(user.get(0).getUserType().equalsIgnoreCase("AD") || user.getUserType().equalsIgnoreCase("SD"))) {
//            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "YOU ARE NOT AUTHORIZED FOR LOGIN");
//        }
//        if ((user.get(0).getAccount_expired() == true && user.get(0).getAccount_locked() == true)) {
//            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "YOU ARE NOT AUTHORIZED FOR LOGIN BECAUSE YOUR ACCOUNT IS LOCKED");
//        }


        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUserId(user.getUserName());
        loginResponse.setMailId(user.getMobileNo());
        loginResponse.setUserType(user.getUnitId());
        loginResponse.setUserName(user.getUserName());
        loginResponse.setSetToken(jwtUtils.generateJwtToken(user.getUserName(), user.getPid(), user.getPid()));

        return ResponseUtils.createSuccessResponse(loginResponse, new TypeReference<LoginResponse>() {
        });
    }


}



package com.sdd.service;


import com.sdd.request.LoginRequest;
import com.sdd.response.ApiResponse;
import com.sdd.response.LoginResponse;

import java.util.List;

public interface UserAccountServices {

    ApiResponse<LoginResponse> login(LoginRequest loginRequest);

}

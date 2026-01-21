package com.datalabeling.datalabelingsupportsystem.service.User;

import com.datalabeling.datalabelingsupportsystem.dto.request.User.CreateUserRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.UserResponse;

import java.util.List;

public interface IUserService {

    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getAllUsers();
}

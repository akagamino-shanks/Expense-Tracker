package com.expensetracker.service;

import com.expensetracker.dto.AuthRequest;
import com.expensetracker.dto.AuthResponse;
import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.dto.UserResponse;

/**
 * Service interface for User registration and authentication operations.
 */
public interface UserService {

    UserResponse registerUser(RegisterRequest request);

    AuthResponse login(AuthRequest request);
}

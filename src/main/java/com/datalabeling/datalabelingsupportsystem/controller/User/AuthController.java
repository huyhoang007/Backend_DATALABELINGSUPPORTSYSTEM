package com.datalabeling.datalabelingsupportsystem.controller.User;

import com.datalabeling.datalabelingsupportsystem.config.JWT.JwtService;
import com.datalabeling.datalabelingsupportsystem.dto.request.User.LoginRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.User.RegisterRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.JwtResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.service.User.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Operation(
            summary = "Đăng ký",
            description = "Đăng ký không thành công, trả về JWT"
    )
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("Register successfully");
    }

    @Operation(
            summary = "Đăng nhập",
            description = "Đăng nhập bằng username và password, trả về JWT"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "401", description = "Sai username hoặc password")
    })
    @PostMapping("/login")
    @PermitAll   // ⭐ QUAN TRỌNG
    public ResponseEntity<JwtResponse> login(
            @RequestBody LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

        // 👇 LẤY USER TỪ AUTHENTICATION
        User user = (User) authentication.getPrincipal();

        // 👇 TRUYỀN USER (UserDetails)
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new JwtResponse(token));
    }
}



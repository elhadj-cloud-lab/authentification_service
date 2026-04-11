package com.bestech.authentification_service.Controllers;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.service.UserService;
import com.bestech.authentification_service.service.register.RegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping("all")
    public List<MyUser> findAllUsers() {
        return userService.findAllUsers();
    }

    @PostMapping("/register")
    public MyUser register(@RequestBody RegistrationRequest request) {
        return userService.registerUser(request);
    }

    @GetMapping("/verifyEmail/{token}")
    public MyUser verifyEmail(@PathVariable("token") String token) {
        return userService.validateToken(token);
    }
}

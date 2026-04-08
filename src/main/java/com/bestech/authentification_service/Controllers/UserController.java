package com.bestech.authentification_service.Controllers;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

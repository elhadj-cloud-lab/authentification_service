package com.bestech.authentification_service.service;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.model.Role;

import java.util.List;

public interface UserService {

    MyUser saveUser(MyUser user);
    MyUser findUserByUsername (String username);
    Role addRole(Role role);
    MyUser addRoleToUser(String username, String rolename);
    List<MyUser> findAllUsers();
}

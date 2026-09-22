package com.example.gearrent.service;

import org.springframework.stereotype.Service;

@Service
public class LoginService {

    public void login(String username, String password) {
            try {
                if(!username.isEmpty() && !password.isEmpty()){
                    if(username.equals("admin") && password.equals("admin")){
                        System.out.println("Login successful");
                    }
                }
            } catch (Exception e) {
                System.out.println("Login failed");
            }
        }
    }


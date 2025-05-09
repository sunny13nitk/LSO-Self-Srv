package com.sap.cap.esmapi.ui.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/logout")
public class LogoutController
{

    @Value("${logout}")
    private String logoutDestination;

    @GetMapping("/")
    public String showLogout()
    {

        return "redirect:" + logoutDestination;
    }

}

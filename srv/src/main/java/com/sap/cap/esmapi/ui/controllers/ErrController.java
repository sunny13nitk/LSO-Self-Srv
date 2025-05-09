package com.sap.cap.esmapi.ui.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/err")
@Slf4j
public class ErrController
{

    @GetMapping("/access-denied")
    public ModelAndView showAccessDenied()
    {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("error");
        mv.addObject("formError",
                "Invalid Token! Access to app not possible. Try clearing browser history and cookies and reaccessing the app. You can also try logging in via a private/Incognito window.");
        log.error(
                "Invalid Token! Access to app not possible. Try clearing browser history and cookies and reaccessing the app. You can also try logging in via a private/Incognito window. ");

        return mv;
    }
}

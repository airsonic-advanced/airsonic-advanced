package org.airsonic.player.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/ratingJsp")
public class RatingJspController {
    @GetMapping
    public ModelAndView get(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("rating", request.getParameterMap());
    }
}

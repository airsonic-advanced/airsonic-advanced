package org.airsonic.player.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

@Controller
@RequestMapping("/bookmarks")
public class BookmarksController {

    @GetMapping
    public ModelAndView doGet(HttpServletRequest request) {
        return new ModelAndView("bookmarks", "model",
                Map.of("initialPaginationSize", 20));
    }

}

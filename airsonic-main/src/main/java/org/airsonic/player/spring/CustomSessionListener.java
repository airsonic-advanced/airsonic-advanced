package org.airsonic.player.spring;

import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@Configuration
public class CustomSessionListener implements HttpSessionListener {

    @Autowired
    private SettingsService settingsService;

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        int minutesToSeconds = settingsService.getSessionTimeout() * 60;

        event.getSession().setMaxInactiveInterval(minutesToSeconds);
    }
}
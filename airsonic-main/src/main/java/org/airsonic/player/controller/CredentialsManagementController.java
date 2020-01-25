package org.airsonic.player.controller;

import com.google.common.collect.ImmutableMap;

import org.airsonic.player.command.CredentialsManagementCommand;
import org.airsonic.player.command.CredentialsManagementCommand.CredentialsCommand;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.security.GlobalSecurityConfig;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/credentialsSettings")
public class CredentialsManagementController {
    private static final Logger LOG = LoggerFactory.getLogger(CredentialsManagementController.class);

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SettingsService settingsService;

    private static final Map<String, Object> APPS_CREDS_SETTINGS = ImmutableMap.of(
            "airsonic",
            ImmutableMap.of("usernameRequired", false, "nonDecodableEncodersAllowed", true),
            "last.fm",
            ImmutableMap.of("usernameRequired", true, "nonDecodableEncodersAllowed", false),
            "listenbrainz",
            ImmutableMap.of("usernameRequired", false, "nonDecodableEncodersAllowed", false));

    private static final Map<String, Object> ENCODER_ALIASES = ImmutableMap.of("noop", "plaintext");

    @GetMapping
    protected ModelAndView displayForm(Authentication user, ModelMap map) {
        List<CredentialsCommand> creds = securityService.getCredentials(user.getName(), null)
                .parallelStream()
                .map(CredentialsCommand::fromUserCredential)
                .sorted(Comparator.comparing(CredentialsCommand::getCreated))
                .collect(Collectors.toList());

        creds = new ArrayList<>(creds);
        creds.add(new CredentialsCommand("bla", "noop", "airsonic", null, null, null, null, null));
        creds.add(new CredentialsCommand("bla", "noop", "last.fm", null, null, null, null, null));
        creds.add(new CredentialsCommand("bla3", "noop", "last.fm", null, null, Instant.now(), null, null));
        creds.add(new CredentialsCommand("bla3", "noop", "airsonic", null, null,
                Instant.now().plusSeconds(86400), null, null));
        creds.add(new CredentialsCommand("bla3", "bcrypt", "airsonic", null, null, Instant.now().plusSeconds(86400),
                null, null));
        creds.add(new CredentialsCommand("bla3", "scrypt", "airsonic", null, null, Instant.now().plusSeconds(86400),
                null, null));
        creds.add(new CredentialsCommand("bla3", "bcrypt", "last.fm", null, null, Instant.now().plusSeconds(86400),
                null, null));
        creds.add(new CredentialsCommand("bla3", "scrypt", "last.fm", null, null, Instant.now().plusSeconds(86400),
                null, null));
        creds.add(new CredentialsCommand("bla3", "legacyhex", "last.fm", null, null, Instant.now().plusSeconds(86400),
                null, null));

        creds.parallelStream().forEach(c -> {
            if (c.getType().startsWith("legacy")) {
                c.addDisplayComment("migratecred");
            }

            if (GlobalSecurityConfig.OPENTEXT_ENCODERS.contains(c.getType())) {
                c.addDisplayComment("opentextcred");
            }

            if (GlobalSecurityConfig.DECODABLE_ENCODERS.contains(c.getType())) {
                c.addDisplayComment("decodablecred");
            } else {
                c.addDisplayComment("nondecodablecred");
            }
        });

        // for updates/deletes/read
        map.addAttribute("command", new CredentialsManagementCommand(creds));
        // for new creds
        map.addAttribute("newCreds", new CredentialsCommand());

        map.addAttribute("apps", APPS_CREDS_SETTINGS.keySet());
        map.addAttribute("appsCredsSettingsJson", Util.toJson(APPS_CREDS_SETTINGS));

        map.addAttribute("decodableEncoders", GlobalSecurityConfig.NONLEGACY_DECODABLE_ENCODERS);
        map.addAttribute("decodableEncodersJson", Util.toJson(GlobalSecurityConfig.NONLEGACY_DECODABLE_ENCODERS));
        map.addAttribute("nonDecodableEncoders", GlobalSecurityConfig.NONLEGACY_NONDECODABLE_ENCODERS);
        map.addAttribute("nonDecodableEncodersJson", Util.toJson(GlobalSecurityConfig.NONLEGACY_NONDECODABLE_ENCODERS));
        map.addAttribute("encoderAliases", ENCODER_ALIASES);
        map.addAttribute("encoderAliasesJson", Util.toJson(ENCODER_ALIASES));
        map.addAttribute("defaultDecodableEncoder", "hex");
        map.addAttribute("defaultEncoder", settingsService.getAirsonicPasswordEncoder());

        map.addAttribute("adminRole", user.getAuthorities().parallelStream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")));

        return new ModelAndView("credentialsSettings", map);
    }

    @DeleteMapping
    public String deleteCred(Principal user, Map<String, Object> map, RedirectAttributes redirectAttributes) {
        List<UserCredential> creds = securityService.getCredentials(user.getName(), null);

        creds.parallelStream().filter(sc -> StringUtils.equals(String.valueOf(sc.hashCode()), "0")).findAny()
                .ifPresent(dbCreds -> {
                    if (!securityService.deleteCredential(dbCreds)) {
                        LOG.warn("Could not delete creds for user {}", dbCreds.getUsername());
                    }
                });

        redirectAttributes.addFlashAttribute("settings_reload", false);
        redirectAttributes.addFlashAttribute("settings_toast", true);

        return "redirect:credentialsSettings.view";
    }

    @PostMapping
    protected String createNewCreds(Principal user, @ModelAttribute CredentialsCommand cc, RedirectAttributes redirectAttributes) {
        UserCredential uc = new UserCredential(user.getName(), cc.getUsername(), cc.getCredential(), cc.getType(), cc.getLocation(), cc.getComment(), cc.getExpirationInstant());

        securityService.createCredential(uc);

        redirectAttributes.addFlashAttribute("settings_reload", false);
        redirectAttributes.addFlashAttribute("settings_toast", true);

        return "redirect:credentialsSettings.view";
    }

    @PutMapping
    protected String updateCreds(Principal user, @ModelAttribute CredentialsManagementCommand cmc, RedirectAttributes redirectAttributes) {
        List<UserCredential> creds = securityService.getCredentials(user.getName(), null);

        cmc.getCredentials().forEach(c -> {
            creds.parallelStream().filter(sc -> StringUtils.equals(String.valueOf(sc.hashCode()), c.getHash()))
                    .findAny().ifPresent(dbCreds -> {
                        if (c.getMarkedForDeletion()) {
                            if (!securityService.deleteCredential(dbCreds)) {
                                LOG.warn("Could not delete creds for user {}", dbCreds.getUsername());
                            }
                        } else {
                            UserCredential newCreds = new UserCredential(dbCreds);
                            newCreds.setType(c.getType());
                            newCreds.setExpiration(c.getExpirationInstant());

                            if (!securityService.updateCredentials(dbCreds, newCreds, "User updated", false)) {
                                LOG.warn("Could not update creds for user {}", dbCreds.getUsername());
                            }

                        }
                    });

        });

        redirectAttributes.addFlashAttribute("settings_reload", false);
        redirectAttributes.addFlashAttribute("settings_toast", true);

        return "redirect:credentialsSettings.view";
    }
}

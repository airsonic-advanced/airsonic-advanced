package org.airsonic.player.controller;

import com.google.common.collect.ImmutableMap;

import org.airsonic.player.command.CredentialsManagementCommand;
import org.airsonic.player.command.CredentialsManagementCommand.AdminControls;
import org.airsonic.player.command.CredentialsManagementCommand.AppCredSettings;
import org.airsonic.player.command.CredentialsManagementCommand.CredentialsCommand;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.security.GlobalSecurityConfig;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.Util;
import org.airsonic.player.validator.CredentialsManagementValidators.CredentialCreateChecks;
import org.airsonic.player.validator.CredentialsManagementValidators.CredentialUpdateChecks;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.groups.Default;

import java.security.Principal;
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

    public static final Map<App, AppCredSettings> APPS_CREDS_SETTINGS = ImmutableMap.of(
            App.AIRSONIC, new AppCredSettings(false, true),
            App.LASTFM, new AppCredSettings(true, false),
            App.LISTENBRAINZ, new AppCredSettings(false, false));

    private static final Map<String, String> ENCODER_ALIASES = ImmutableMap.of("noop", "plaintext", "legacynoop",
            "legacyplaintext (deprecated)", "legacyhex", "legacyhex (deprecated)");

    @GetMapping
    protected String displayForm() {
        return "credentialsSettings";
    }

    @ModelAttribute
    protected void displayForm(Authentication user, ModelMap map) {
        List<CredentialsCommand> creds = securityService.getCredentials(user.getName(), App.values())
                .parallelStream()
                .map(CredentialsCommand::fromUserCredential)
                .map(c -> {
                    if (c.getEncoder().startsWith("legacy")) {
                        c.addDisplayComment("migratecred");
                    }

                    if (GlobalSecurityConfig.OPENTEXT_ENCODERS.contains(c.getEncoder())) {
                        c.addDisplayComment("opentextcred");
                    }

                    if (GlobalSecurityConfig.DECODABLE_ENCODERS.contains(c.getEncoder())) {
                        c.addDisplayComment("decodablecred");
                    } else {
                        c.addDisplayComment("nondecodablecred");
                    }

                    return c;
                })
                .sorted(Comparator.comparing(CredentialsCommand::getCreated))
                .collect(Collectors.toList());

        User userInDb = securityService.getUserByName(user.getName());

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

        map.addAttribute("preferredEncoderNonDecodableAllowed", securityService.getPreferredPasswordEncoder(true));
        map.addAttribute("preferredEncoderDecodableOnly", securityService.getPreferredPasswordEncoder(false));

        map.addAttribute("ldapAuthEnabledForUser", userInDb.isLdapAuthenticated());
        map.addAttribute("adminRole", userInDb.isAdminRole());

        // admin restricted, installation-wide settings
        if (userInDb.isAdminRole()) {
            map.addAttribute("adminControls",
                    new AdminControls(
                            securityService.checkCredentialsStoredInLegacyTables(),
                            securityService.checkLegacyCredsPresent(),
                            securityService.checkOpenCredsPresent(),
                            securityService.checkDefaultAdminCredsPresent(),
                            settingsService.getJWTKey(),
                            settingsService.getEncryptionPassword(),
                            settingsService.getEncryptionSalt(),
                            settingsService.getNonDecodablePasswordEncoder(),
                            settingsService.getDecodablePasswordEncoder(),
                            settingsService.getPreferNonDecodablePasswords()
                            ));
        }
    }

    @PostMapping
    protected String createNewCreds(Principal user,
            @ModelAttribute("newCreds") @Validated(value = { Default.class, CredentialCreateChecks.class }) CredentialsCommand cc,
            BindingResult br, RedirectAttributes redirectAttributes, ModelMap map) {
        if (br.hasErrors()) {
            map.addAttribute("open_CreateCredsDialog", true);
            return "credentialsSettings";
        }

        UserCredential uc = new UserCredential(user.getName(), cc.getUsername(), cc.getCredential(), cc.getEncoder(), cc.getApp(), "Created by user", cc.getExpirationInstant());

        if (!APPS_CREDS_SETTINGS.get(uc.getApp()).getUsernameRequired()) {
            uc.setAppUsername(user.getName());
        }

        boolean success = true;
        if (!securityService.createCredential(uc)) {
            LOG.warn("Could not create creds for user {}", user.getName());
            success = false;
        }

        redirectAttributes.addFlashAttribute("settings_toast", success);

        return "redirect:credentialsSettings.view";
    }

    @PutMapping
    protected String updateCreds(Principal user,
            @ModelAttribute("command") @Validated(value = { Default.class, CredentialUpdateChecks.class }) CredentialsManagementCommand cmc,
            BindingResult br, RedirectAttributes redirectAttributes) {
        if (br.hasErrors()) {
            return "credentialsSettings";
        }

        List<Boolean> failures = new ArrayList<>();
        List<UserCredential> creds = securityService.getCredentials(user.getName(), App.values());

        cmc.getCredentials().forEach(c -> {
            creds.parallelStream().filter(sc -> StringUtils.equals(String.valueOf(sc.hashCode()), c.getHash()))
                    .findAny().ifPresent(dbCreds -> {
                        if (c.getMarkedForDeletion()) {
                            if (!securityService.deleteCredential(dbCreds)) {
                                LOG.warn("Could not delete creds for user {}", dbCreds.getUsername());
                                failures.add(true);
                            }
                        } else {
                            UserCredential newCreds = new UserCredential(dbCreds);
                            newCreds.setEncoder(c.getEncoder());
                            newCreds.setExpiration(c.getExpirationInstant());

                            if (!securityService.updateCredentials(dbCreds, newCreds, "User updated", false)) {
                                LOG.warn("Could not update creds for user {}", dbCreds.getUsername());
                                failures.add(true);
                            }
                        }
                    });
        });

        redirectAttributes.addFlashAttribute("settings_toast", failures.isEmpty());

        return "redirect:credentialsSettings.view";
    }

    @PostMapping(path = "/admin")
    protected String adminControls(Authentication user, @Validated @ModelAttribute("adminControls") AdminControls ac,
            BindingResult br, RedirectAttributes redirectAttributes, ModelMap map) {
        if (br.hasErrors()) {
            return "/credentialsSettings";
        }

        if (map.getAttribute("adminRole") == null || !((boolean) map.getAttribute("adminRole"))) {
            return "/credentialsSettings";
        }

        boolean success = true;
        if (ac.getPurgeCredsInLegacyTables()) {
            success = securityService.purgeCredentialsStoredInLegacyTables();
        }

        if (ac.getMigrateLegacyCredsToNonLegacyDefault()) {
            success = securityService.migrateLegacyCredsToNonLegacy(false);
        } else if (ac.getMigrateLegacyCredsToNonLegacyDecodableOnly()) {
            success = securityService.migrateLegacyCredsToNonLegacy(true);
        }

        boolean saveSettings = false;
        if (ac.getJwtKeyChanged()) {
            settingsService.setJWTKey(ac.getJwtKey());
            saveSettings = true;
        }

        if (ac.getEncryptionKeyChanged()) {
            settingsService.setEncryptionPassword(ac.getEncryptionKey());
            saveSettings = true;
        }

        if (ac.getNonDecodableEncoderChanged()) {
            settingsService.setNonDecodablePasswordEncoder(ac.getNonDecodableEncoder());
            saveSettings = true;
        }

        if (ac.getDecodableEncoderChanged()) {
            settingsService.setDecodablePasswordEncoder(ac.getDecodableEncoder());
            saveSettings = true;
        }

        if (ac.getNonDecodablePreferenceChanged()) {
            settingsService.setPreferNonDecodablePasswords(ac.getPreferNonDecodable());
            saveSettings = true;
        }

        if (saveSettings) {
            settingsService.save();
        }

        redirectAttributes.addFlashAttribute("settings_toast", success);

        return "redirect:/credentialsSettings.view";
    }
}

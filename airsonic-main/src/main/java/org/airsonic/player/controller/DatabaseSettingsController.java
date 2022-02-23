/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.command.DatabaseSettingsCommand;
import org.airsonic.player.command.DatabaseSettingsCommand.DataSourceConfigType;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.User;
import org.airsonic.player.io.PipeStreams;
import org.airsonic.player.service.DatabaseService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Principal;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Controller
@RequestMapping("/databaseSettings")
public class DatabaseSettingsController {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseSettingsController.class);

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SecurityService securityService;

    private static final UUID DB_CONTROLLER_IMPORT_CALLBACK_ID = UUID.randomUUID();

    @PostConstruct
    public void registerWithUploadController() {
        UploadController.registeredCallbacks.put(DB_CONTROLLER_IMPORT_CALLBACK_ID, this::importDB);
    }

    @GetMapping
    protected String displayForm() {
        return "databaseSettings";
    }

    protected void importDB(Path dir) {
        databaseService.importDB(dir);
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportDB(Principal p, ServletWebRequest swr) throws Exception {
        Path exportFile = databaseService.exportDB();

        if (exportFile == null) {
            return ResponseEntity.internalServerError().build();
        }

        User user = securityService.getUserByName(p.getName());
        Player transferPlayer = playerService.getPlayer(swr.getRequest(), swr.getResponse(), false, false);
        Supplier<TransferStatus> statusSupplier = () -> statusService.createDownloadStatus(transferPlayer);

        Consumer<TransferStatus> statusCloser = status -> {
            statusService.removeDownloadStatus(status);
            securityService.updateUserByteCounts(user, 0L, status.getBytesTransferred(), 0L);
            LOG.info("Transferred {} bytes to user: {}, player: {}", status.getBytesTransferred(), user.getUsername(), transferPlayer);
            databaseService.cleanup(status.getExternalFile());
        };
        Resource res = new FileSystemResource(exportFile);
        Resource monitoredRes = new PipeStreams.MonitoredResource(
                res,
                settingsService.getDownloadBitrateLimiter(),
                statusSupplier,
                statusCloser,
            (input, status) -> {
                status.setExternalFile(exportFile);
                status.setBytesTotal(FileUtil.size(exportFile));
            });

        String filename = StringUtil.fileSystemSafe(exportFile.getFileName().toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentType(MediaType.parseMediaType(StringUtil.getMimeType(FilenameUtils.getExtension(filename))));
        return ResponseEntity.ok().headers(headers).body(monitoredRes);
    }

    @GetMapping("/backup")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void backupDB() throws Exception {
        databaseService.backup();
    }

    @ModelAttribute
    protected void formBackingObject(Model model) throws Exception {
        DatabaseSettingsCommand command = new DatabaseSettingsCommand();
        command.setUrl(settingsService.getDatabaseUrl());
        command.setDriver(settingsService.getDatabaseDriver());
        command.setPassword(settingsService.getDatabasePassword());
        command.setUsername(settingsService.getDatabaseUsername());
        command.setJNDIName(settingsService.getDatabaseJNDIName());
        command.setMysqlVarcharMaxlength(settingsService.getDatabaseMysqlVarcharMaxlength());

        if (StringUtils.isNotBlank(command.getJNDIName())) {
            command.setConfigType(DataSourceConfigType.JNDI);
        } else if (StringUtils.equals(command.getUrl(), SettingsService.getDefaultJDBCUrl())
                && StringUtils.equals(command.getUsername(), SettingsService.getDefaultJDBCUsername())
                && StringUtils.equals(command.getPassword(), SettingsService.getDefaultJDBCPassword())) {
            command.setConfigType(DataSourceConfigType.BUILTIN);
        } else {
            command.setConfigType(DataSourceConfigType.EXTERNAL);
        }
        command.setCallback(DB_CONTROLLER_IMPORT_CALLBACK_ID.toString());
        command.setImportFolder(DatabaseService.getImportDBFolder().toString());
        command.setBackuppable(databaseService.backuppable());
        command.setDbBackupInterval(settingsService.getDbBackupInterval());
        command.setDbBackupRetentionCount(settingsService.getDbBackupRetentionCount());
        model.addAttribute("command", command);
    }

    @PostMapping
    protected String onSubmit(@ModelAttribute("command") @Validated DatabaseSettingsCommand command,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            settingsService.resetDatabaseToDefault();
            switch (command.getConfigType()) {
                case EXTERNAL:
                    settingsService.setDatabaseDriver(command.getDriver());
                    settingsService.setDatabasePassword(command.getPassword());
                    settingsService.setDatabaseUrl(command.getUrl());
                    settingsService.setDatabaseUsername(command.getUsername());
                    break;
                case JNDI:
                    settingsService.setDatabaseJNDIName(command.getJNDIName());
                    break;
                case BUILTIN:
                default:
                    break;
            }
            if (command.getConfigType() != DataSourceConfigType.BUILTIN) {
                settingsService.setDatabaseMysqlVarcharMaxlength(command.getMysqlVarcharMaxlength());
            }
            settingsService.setDbBackupInterval(command.getDbBackupInterval());
            settingsService.setDbBackupRetentionCount(command.getDbBackupRetentionCount());
            redirectAttributes.addFlashAttribute("settings_toast", true);
            settingsService.save();
            return "redirect:databaseSettings.view";
        } else {
            return "databaseSettings.view";
        }
    }

}

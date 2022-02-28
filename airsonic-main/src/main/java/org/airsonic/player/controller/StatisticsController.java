package org.airsonic.player.controller;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.TransferStatus.SampleHistory;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private StatusService statusService;

    @GetMapping("/users")
    public List<UserDataTransferStatistics> getUserStatistics() {
        return securityService.getAllUsers().stream().map(x -> new UserDataTransferStatistics(x.getUsername(), x.getBytesStreamed(), x.getBytesDownloaded(), x.getBytesUploaded())).collect(Collectors.toList());
    }

    @GetMapping("/transfers")
    public List<TransferStatusHolder> getTransferStatistics(HttpServletRequest request) {
        List<TransferStatus> streamStatuses = statusService.getAllStreamStatuses();
        List<TransferStatus> downloadStatuses = statusService.getAllDownloadStatuses();
        List<TransferStatus> uploadStatuses = statusService.getAllUploadStatuses();

        Locale locale = RequestContextUtils.getLocale(request);
        List<TransferStatusHolder> transferStatuses = new ArrayList<>();
        streamStatuses.stream()
                .filter(x -> ((x.getMillisSinceLastUpdate() / 1000L / 60L) < 60L))
                .map(x -> new TransferStatusHolder(x, TransferType.stream, locale))
                .forEach(transferStatuses::add);

        downloadStatuses.stream()
                .map(x -> new TransferStatusHolder(x, TransferType.download, locale))
                .forEach(transferStatuses::add);

        uploadStatuses.stream()
                .map(x -> new TransferStatusHolder(x, TransferType.upload, locale))
                .forEach(transferStatuses::add);

        return transferStatuses;
    }

    public static class UserDataTransferStatistics {
        private final String user;
        private final long streamed;
        private final long downloaded;
        private final long uploaded;
        public UserDataTransferStatistics(String user, long streamed, long downloaded, long uploaded) {
            this.user = user;
            this.streamed = streamed;
            this.downloaded = downloaded;
            this.uploaded = uploaded;
        }

        public String getUser() {
            return user;
        }
        public long getStreamed() {
            return streamed;
        }
        public long getDownloaded() {
            return downloaded;
        }
        public long getUploaded() {
            return uploaded;
        }
    }

    public static class TransferStatusHolder {
        private final TransferType transferType;
        private final String playerDescription;
        private final String playerType;
        private final String username;
        private final String path;
        private final String bytesTransferred;
        private final SampleHistory history;

        TransferStatusHolder(TransferStatus transferStatus, TransferType transferType, Locale locale) {
            this.transferType = transferType;
            this.playerDescription = Optional.ofNullable(transferStatus.getPlayer()).map(Player::toString).orElse(null);
            this.playerType = Optional.ofNullable(transferStatus.getPlayer()).map(Player::getType).orElse(null);
            this.username = Optional.ofNullable(transferStatus.getPlayer()).map(Player::getUsername).orElse(null);
            this.path = Optional.ofNullable(transferStatus.getMediaFile())
                    .map(MediaFile::getRelativePath)
                    .or(() -> Optional.ofNullable(transferStatus.getExternalFile()))
                    .map(FileUtil::getShortPath)
                    .orElse(null);
            this.bytesTransferred = StringUtil.formatBytes(transferStatus.getBytesTransferred(), locale);
            this.history = transferStatus.getHistory();
        }

        public TransferType getTransferType() {
            return transferType;
        }

        public String getPlayerDescription() {
            return playerDescription;
        }

        public String getPlayerType() {
            return playerType;
        }

        public String getUsername() {
            return username;
        }

        public String getPath() {
            return path;
        }

        public String getBytesTransferred() {
            return bytesTransferred;
        }

        public SampleHistory getHistory() {
            return history;
        }
    }

    public enum TransferType {
        stream, download, upload
    }
}

package org.airsonic.player.spring.migrations;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.UpdateStatement;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AddMediaFileIdToPodcastChannels implements CustomSqlChange {

    @Override
    public String getConfirmationMessage() {
        return "Added media file ids to podcast channels";
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        Map<Integer, Integer> idToMediaFile = new HashMap<>();
        Path folderPath = null;
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (PreparedStatement st1 = conn.prepareStatement("SELECT path FROM music_folder WHERE type='PODCAST';");
                    ResultSet rs1 = st1.executeQuery();
                    PreparedStatement st2 = conn.prepareStatement("SELECT id, title FROM podcast_channel;");
                    ResultSet rs2 = st2.executeQuery();
                    ) {
                while (rs1.next()) {
                    folderPath = Paths.get(rs1.getString("path"));
                }
                while (rs2.next()) {
                    String title = rs2.getString("title");
                    if (StringUtils.isNotEmpty(title)) {
                        Path channelFolder = folderPath.resolve(StringUtil.fileSystemSafe(title));
                        try (PreparedStatement st3 = conn.prepareStatement("SELECT id FROM media_file WHERE path=?;")) {
                            st3.setString(1, channelFolder.toString());
                            try (ResultSet rs3 = st3.executeQuery()) {
                                while (rs3.next()) {
                                    idToMediaFile.put(rs2.getInt("id"), rs3.getInt("id"));
                                }
                            }
                        }
                    }
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        return idToMediaFile.entrySet().stream()
                        .map(e -> new UpdateStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "podcast_channel")
                                .addNewColumnValue("media_file_id", e.getValue())
                                .addWhereColumnName("id")
                                .addWhereParameter(e.getKey()))
                        .toArray(SqlStatement[]::new);
    }
}

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
            try (Statement st = conn.createStatement();
                    ResultSet result = st.executeQuery("SELECT id, title FROM podcast_channel;");
                    ResultSet result2 = st.executeQuery("SELECT path FROM music_folder WHERE type='PODCAST';");) {

                while (result2.next()) {
                    folderPath = Paths.get(result2.getString("path"));
                }
                while (result.next()) {
                    String title = result.getString("title");
                    if (StringUtils.isNotEmpty(title)) {
                        Path channelFolder = folderPath.resolve(StringUtil.fileSystemSafe(title));
                        try (ResultSet result3 = st.executeQuery("SELECT id FROM media_file WHERE path='" + channelFolder.toString() + "';")) {
                            while (result3.next()) {
                                idToMediaFile.put(result.getInt("id"), result3.getInt("id"));
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

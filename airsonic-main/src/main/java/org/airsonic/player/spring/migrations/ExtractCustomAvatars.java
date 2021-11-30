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
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ExtractCustomAvatars implements CustomSqlChange {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractCustomAvatars.class);

    @Override
    public String getConfirmationMessage() {
        return "Extracted avatars from DB";
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
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (PreparedStatement st = conn.prepareStatement("select * from custom_avatar");
                    ResultSet result = st.executeQuery()) {

                while (result.next()) {
                    try {
                        Path folder = SettingsService.getAirsonicHome().resolve("avatars").resolve(result.getString("username"));
                        Files.createDirectories(folder);
                        Path filePath = folder.resolve(result.getString("name") + "." + StringUtils.substringAfter(result.getString("mime_type"), "/"));
                        Files.copy(new ByteArrayInputStream(result.getBytes("data")), filePath);
                    } catch (Exception e) {
                        LOG.warn("Exception while trying to extract avatar for user {}. Will skip this user.", result.getString("username"), e);
                    }
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        return new SqlStatement[0];
    }
}

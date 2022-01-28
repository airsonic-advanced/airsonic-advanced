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
import liquibase.statement.core.InsertStatement;
import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class AddPodcastMediaFolderUsers implements CustomSqlChange {
    @Override
    public String getConfirmationMessage() {
        return "Added podcast media folder users";
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
        Set<String> podcastUsers = new HashSet<>();
        Integer maxFolderId = null;
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (PreparedStatement st1 = conn.prepareStatement("SELECT MAX(id) AS maxid FROM music_folder WHERE type='PODCAST';");
                    ResultSet rs1 = st1.executeQuery();
                    PreparedStatement st2 = conn.prepareStatement("SELECT username, roles FROM users;");
                    ResultSet rs2 = st2.executeQuery();) {
                while (rs1.next()) {
                    maxFolderId = rs1.getInt("maxid");
                }
                while (rs2.next()) {
                    if (StringUtils.contains(rs2.getString("roles"), "PODCAST")) {
                        podcastUsers.add(rs2.getString("username"));
                    }
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        Integer folderId = maxFolderId;

        return podcastUsers.stream()
                        .map(u -> new InsertStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "music_folder_user")
                                .addColumnValue("music_folder_id", folderId)
                                .addColumnValue("username", u))
                        .toArray(SqlStatement[]::new);
    }
}

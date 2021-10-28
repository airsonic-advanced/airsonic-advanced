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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
            try (Statement st = conn.createStatement();
                    ResultSet result = st.executeQuery("SELECT username, roles FROM user;");
                    ResultSet result2 = st
                            .executeQuery("SELECT MAX(id) AS maxid FROM music_folder WHERE name='Podcasts';");) {

                while (result2.next()) {
                    maxFolderId = result2.getInt("maxid");
                }
                while (result.next()) {
                    if (StringUtils.contains(result.getString("roles"), "PODCAST")) {
                        podcastUsers.add(result.getString("username"));
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

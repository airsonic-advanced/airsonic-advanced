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

import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class AddUnknownMediaFolders implements CustomSqlChange {

    @Override
    public String getConfirmationMessage() {
        return "Added unknown music folders";
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
        Set<String> unknownMusicFolders = new HashSet<>();
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (Statement st = conn.createStatement();
                    ResultSet result = st.executeQuery(""
                            + "SELECT m.folder "
                            + "FROM media_file m "
                            + "LEFT OUTER JOIN music_folder f "
                            + "ON m.folder = f.path "
                            + "WHERE f.path IS NULL;");) {

                while (result.next()) {
                    unknownMusicFolders.add(result.getString("folder"));
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        return unknownMusicFolders.stream()
                .map(f -> new InsertStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "music_folder")
                        .addColumnValue("path", f)
                        .addColumnValue("name", Paths.get(f).getFileName().toString())
                        .addColumnValue("enabled", false))
                .toArray(SqlStatement[]::new);
    }
}

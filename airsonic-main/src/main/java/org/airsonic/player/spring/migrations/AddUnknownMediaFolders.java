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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        Integer maxFolderId = null;
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (PreparedStatement st1 = conn.prepareStatement("SELECT MAX(id) AS maxid FROM music_folder;");
                    ResultSet rs1 = st1.executeQuery();
                    PreparedStatement st2 = conn.prepareStatement(""
                        + "SELECT m.folder "
                        + "FROM media_file m "
                        + "LEFT OUTER JOIN music_folder f "
                        + "ON m.folder = f.path "
                        + "WHERE f.path IS NULL;");
                    ResultSet rs2 = st2.executeQuery();
                    ) {
                while (rs1.next()) {
                    maxFolderId = rs1.getInt("maxid");
                }
                while (rs2.next()) {
                    unknownMusicFolders.add(rs2.getString("folder"));
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }
        List<String> unknownMusicFoldersList = new ArrayList<>(unknownMusicFolders);
        int folderId = maxFolderId + 1;

        return IntStream.range(0, unknownMusicFoldersList.size())
                .mapToObj(i -> i)
                .flatMap(i -> Stream.of(
                        new InsertStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "music_folder")
                                .addColumnValue("id", folderId + i)
                                .addColumnValue("path", unknownMusicFoldersList.get(i))
                                .addColumnValue("name", Paths.get(unknownMusicFoldersList.get(i)).getFileName().toString())
                                .addColumnValue("enabled", false),
                        new InsertStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "music_folder_user")
                                .addColumnValue("music_folder_id", folderId + i)
                                .addColumnValue("username", "admin")))
                .toArray(SqlStatement[]::new);
    }
}

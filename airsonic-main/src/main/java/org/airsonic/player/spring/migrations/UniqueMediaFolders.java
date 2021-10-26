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
import liquibase.statement.core.DeleteStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class UniqueMediaFolders implements CustomSqlChange {
    private static final Logger LOG = LoggerFactory.getLogger(UniqueMediaFolders.class);

    @Override
    public String getConfirmationMessage() {
        return "Media folder uniqueness established by path";
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
        Set<Integer> duplicates = new HashSet<>();
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (Statement st = conn.createStatement();
                    ResultSet result = st.executeQuery(""
                            + "select m.* from music_folder m join "
                            + "(select path, count(*) from music_folder group by path having count(*) > 1) d "
                            + "on m.path = d.path "
                            + "order by m.id");) {

                Set<String> paths = new HashSet<>();
                while (result.next()) {
                    if (!paths.add(result.getString("path"))) {
                        //duplicates
                        duplicates.add(result.getInt("id"));
                        LOG.info("Duplicate media folder found (id: {}, name: {}) and will be deleted", result.getInt("id"), result.getString("name"));
                    }
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        return duplicates.stream()
                .flatMap(id -> Stream.of(
                        new DeleteStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "music_folder_user")
                                .addWhereColumnName("music_folder_id")
                                .addWhereParameter(id),
                        new DeleteStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "music_folder")
                                .addWhereColumnName("id")
                                .addWhereParameter(id)))
                .toArray(SqlStatement[]::new);
    }
}

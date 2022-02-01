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
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;
import org.airsonic.player.service.MediaFolderService;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        Set<Integer> deletionSet = new HashSet<>();
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (PreparedStatement st = conn.prepareStatement("select * from music_folder order by id");
                    ResultSet result = st.executeQuery();) {

                List<MusicFolder> folders = new ArrayList<>();
                int i = 0;
                while (result.next()) {
                    folders.add(MusicFolderDao.MUSICFOLDER_ROW_MAPPER.mapRow(result, i++));
                }
                Set<String> paths = new HashSet<>();
                // skip deleting podcast folder
                MusicFolder podcastFolder = folders.stream().filter(f -> f.getType() == Type.PODCAST).findFirst().orElse(null);
                if (podcastFolder == null) {
                    throw new RuntimeException("Cannot find podcast folder!");
                }
                paths.add(podcastFolder.getPath().toString());
                folders.forEach(f -> {
                    Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> overlap = MediaFolderService.getMusicFolderPathOverlaps(f, folders);
                    // duplicate
                    if (!overlap.getLeft().isEmpty() && !paths.add(f.getPath().toString()) && f.getType() != Type.PODCAST) {
                        deletionSet.add(f.getId());
                        LOG.info("Duplicate media folder found (id: {}, name: {}) and will be deleted", f.getId(), f.getName());
                    }
                });
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        return deletionSet.stream()
                .flatMap(id -> Stream.of(
                        new DeleteStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "music_folder_user")
                                .setWhere("music_folder_id=?")
                                .addWhereParameter(id),
                        new DeleteStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "music_folder")
                                .setWhere("id=?")
                                .addWhereParameter(id)))
                .toArray(SqlStatement[]::new);
    }
}

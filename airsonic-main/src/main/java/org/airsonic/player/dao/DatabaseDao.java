package org.airsonic.player.dao;

import org.airsonic.player.util.LambdaUtils.ThrowingBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.function.Consumer;

@Repository
public class DatabaseDao extends AbstractDao {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseDao.class);

    public boolean exportDB(Path tempWorkingDir, ThrowingBiFunction<Path, Connection, Boolean, Exception> exportFunction) throws Exception {
        try (Connection con = jdbcTemplate.getDataSource().getConnection()) {
            return exportFunction.apply(tempWorkingDir, con);
        }
    }

    public void importDB(Consumer<Connection> importFunction) {
        try (Connection con = jdbcTemplate.getDataSource().getConnection()) {
            importFunction.accept(con);
        } catch (Exception e) {
            LOG.info("DB Import failed!", e);
        }
    }
}

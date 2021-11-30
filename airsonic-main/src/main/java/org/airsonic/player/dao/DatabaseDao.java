package org.airsonic.player.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

@Repository
public class DatabaseDao extends AbstractDao {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseDao.class);

    public Path exportDB(Function<Connection, Path> exportFunction) {
        try (Connection con = jdbcTemplate.getDataSource().getConnection()) {
            return exportFunction.apply(con);
        } catch (Exception e) {
            LOG.info("DB Export failed!", e);
            return null;
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

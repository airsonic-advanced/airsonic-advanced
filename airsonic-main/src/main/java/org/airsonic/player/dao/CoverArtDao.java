package org.airsonic.player.dao;

import org.airsonic.player.domain.CoverArt;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

@Repository
public class CoverArtDao extends AbstractDao {

    private static final String COLUMNS = "entity_id, entity_type, path, folder_id, overridden, created, updated";

    private CoverArtRowMapper coverArtRowMapper = new CoverArtRowMapper();

    @PostConstruct
    public void register() throws Exception {
        registerInserts("cover_art", null, Arrays.asList(COLUMNS.split(", ")), CoverArt.class);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void upsert(CoverArt art) {
        int n = update(art);

        if (n == 0) {
            insert("cover_art", art);
        }
    }

    public int update(CoverArt art) {
        return update("update cover_art set path=?, folder_id=?, overridden=?, updated=? where entity_id=? and entity_type=?",
                art.getPath(), art.getFolderId(), art.getOverridden(), art.getUpdated(), art.getEntityId(), art.getEntityType().toString());
    }

    public CoverArt get(EntityType type, int id) {
        String sql = "select * from cover_art where entity_id=? and entity_type=?";
        return queryOne(sql, coverArtRowMapper, id, type);
    }

    public void delete(EntityType type, int id) {
        update("delete from cover_art where entity_id=? and entity_type=?", id, type);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void expunge() {
        update("delete from cover_art where entity_type='MEDIA_FILE' and entity_id in (select ca.entity_id from cover_art ca left join media_file m on ca.entity_id=m.id where m.id is null and ca.entity_type='MEDIA_FILE')");
        update("delete from cover_art where entity_type='ALBUM' and entity_id in (select ca.entity_id from cover_art ca left join album a on ca.entity_id=a.id where a.id is null and ca.entity_type='ALBUM')");
        update("delete from cover_art where entity_type='ARTTIST' and entity_id in (select ca.entity_id from cover_art ca left join artist a on ca.entity_id=a.id where a.id is null and ca.entity_type='ARTIST')");
    }

    private static class CoverArtRowMapper implements RowMapper<CoverArt> {
        @Override
        public CoverArt mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CoverArt(rs.getInt("entity_id"), EntityType.valueOf(rs.getString("entity_type")),
                    rs.getString("path"), (Integer) rs.getObject("folder_id"), rs.getBoolean("overridden"),
                    Optional.ofNullable(rs.getTimestamp("created")).map(x -> x.toInstant()).orElse(null),
                    Optional.ofNullable(rs.getTimestamp("updated")).map(x -> x.toInstant()).orElse(null));
        }
    }

}

package org.airsonic.player.dao;

import org.airsonic.player.domain.SonosLink;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@Transactional
public class SonosLinkDao extends AbstractDao {
    private static final String COLUMNS = "username, linkcode, householdid, initiator, initiated";

    private SonosLinkRowMapper rowMapper = new SonosLinkRowMapper();

    public List<SonosLink> getAll() {
        String sql = "select " + COLUMNS + " from sonoslink";
        return query(sql, rowMapper);
    }

    public SonosLink findByLinkcode(String linkcode) {
        String sql = "select " + COLUMNS + " from sonoslink where linkcode=?";
        return queryOne(sql, rowMapper, linkcode);
    }

    public void create(SonosLink sonosLink) {
        String sql = "insert into sonoslink (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ')';
        update(sql, sonosLink.getUsername(), sonosLink.getLinkcode(), sonosLink.getHouseholdId(), sonosLink.getInitiator(), sonosLink.getInitiated());
    }

    public void removeAll() {
        String sql = "delete from sonoslink;";
        update(sql);
    }

    private static class SonosLinkRowMapper implements RowMapper<SonosLink> {
        @Override
        public SonosLink mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SonosLink(rs.getString("username"), rs.getString("linkcode"), rs.getString("householdid"), rs.getString("initiator"), rs.getTimestamp("initiated").toInstant());
        }
    }
}

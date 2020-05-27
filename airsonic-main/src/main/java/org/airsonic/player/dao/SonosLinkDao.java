package org.airsonic.player.dao;

import org.airsonic.player.domain.SonosLink;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
@Transactional
public class SonosLinkDao extends AbstractDao {
    private static final String COLUMNS = "username, householdid, linkcode";

    private SonosLinkRowMapper rowMapper = new SonosLinkRowMapper();

    public SonosLink findByLinkcode(String linkcode) {
        String sql = "select " + COLUMNS + " from sonoslink where linkcode=?";
        return queryOne(sql, rowMapper, linkcode);
    }

    public void create(SonosLink sonosLink) {
        String sql = "insert into sonoslink (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ')';
        update(sql, sonosLink.getUsername(), sonosLink.getHouseholdId(), sonosLink.getLinkcode());
    }

    public void removeAll() {
        String sql = "delete from sonoslink;";
        update(sql);
    }

    private static class SonosLinkRowMapper implements RowMapper<SonosLink> {
        @Override
        public SonosLink mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SonosLink(rs.getString("username"), rs.getString("householdid"), rs.getString("linkcode"));
        }
    }
}

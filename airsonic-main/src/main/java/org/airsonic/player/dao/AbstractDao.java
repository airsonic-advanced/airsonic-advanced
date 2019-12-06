/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract superclass for all DAO's.
 *
 * @author Sindre Mehus
 */
public class AbstractDao {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDao.class);

    @Autowired
    private DaoHelper daoHelper;

    public JdbcTemplate getJdbcTemplate() {
        return daoHelper.getJdbcTemplate();
    }

    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return daoHelper.getNamedParameterJdbcTemplate();
    }

    protected static String questionMarks(String columns) {
        int numberOfColumns = StringUtils.countMatches(columns, ",") + 1;
        return StringUtils.repeat("?", ", ", numberOfColumns);
    }

    protected static String prefix(String columns, String prefix) {
        List<String> l = Arrays.asList(columns.split(", "));
        l.replaceAll(s -> prefix + "." + s);
        return String.join(", ", l);
    }

    protected static Object[] convertToDBTypes(Object[] args) {
        return args == null ? null : Stream.of(args)
                .map(x -> (Object) ((x instanceof Instant) ? Timestamp.from((Instant) x) : x))
                .collect(Collectors.toList())
                .toArray();
    }

    protected static Map<String, Object> convertToDBTypes(Map<String, Object> args) {
        return args == null ? null : args.entrySet()
                .stream()
                .map(x -> (x.getValue() instanceof Instant) ? Pair.of(x.getKey(), Timestamp.from((Instant) x.getValue())) : x)
                //can't use Collectors.toMap due to possible null value mappings
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
    }

    protected int update(String sql, Object... args) {
        long t = System.nanoTime();
        LOG.trace("Executing query: [{}]", sql);
        int result = getJdbcTemplate().update(sql, convertToDBTypes(args));
        LOG.trace("Updated {} rows", result);
        log(sql, t);
        return result;
    }

    protected int namedUpdate(String sql, Map<String, Object> args) {
        long t = System.nanoTime();
        LOG.trace("Executing query: [{}]", sql);
        int result = getNamedParameterJdbcTemplate().update(sql, convertToDBTypes(args));
        LOG.trace("Updated {} rows", result);
        log(sql, t);
        return result;
    }

    private void log(String sql, long startTimeNano) {
        long millis = (System.nanoTime() - startTimeNano) / 1000000L;

        // Log queries that take more than 2 seconds.
        if (millis > TimeUnit.SECONDS.toMillis(2L)) {
            LOG.debug(millis + " ms:  " + sql);
        }
    }

    protected <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        long t = System.nanoTime();
        List<T> result = getJdbcTemplate().query(sql, convertToDBTypes(args), rowMapper);
        log(sql, t);
        return result;
    }

    protected <T> List<T> namedQuery(String sql, RowMapper<T> rowMapper, Map<String, Object> args) {
        long t = System.nanoTime();
        List<T> result = getNamedParameterJdbcTemplate().query(sql, convertToDBTypes(args), rowMapper);
        log(sql, t);
        return result;
    }

    protected <T> List<T> queryForTypes(String sql, Class<T> type, Object... args) {
        long t = System.nanoTime();
        List<T> result = getJdbcTemplate().queryForList(sql, convertToDBTypes(args), type);
        log(sql, t);
        return result;
    }

    protected <T> List<T> namedQueryForTypes(String sql, Class<T> type, Map<String, Object> args) {
        long t = System.nanoTime();
        List<T> result = getNamedParameterJdbcTemplate().queryForList(sql, convertToDBTypes(args), type);
        log(sql, t);
        return result;
    }

    protected List<String> queryForStrings(String sql, Object... args) {
        return queryForTypes(sql, String.class, args);
    }

    protected List<Integer> queryForInts(String sql, Object... args) {
        return queryForTypes(sql, Integer.class, args);
    }

    protected List<String> namedQueryForStrings(String sql, Map<String, Object> args) {
        return namedQueryForTypes(sql, String.class, args);
    }

    protected Integer queryForInt(String sql, Integer defaultValue, Object... args) {
        return queryForTypes(sql, Integer.class, args).stream().filter(Objects::nonNull).findFirst().orElse(defaultValue);
    }

    protected Integer namedQueryForInt(String sql, Integer defaultValue, Map<String, Object> args) {
        return namedQueryForTypes(sql, Integer.class, args).stream().filter(Objects::nonNull).findFirst().orElse(defaultValue);
    }

    protected Instant queryForInstant(String sql, Instant defaultValue, Object... args) {
        return queryForTypes(sql, Timestamp.class, args).stream().filter(Objects::nonNull).findFirst().map(x -> x.toInstant()).orElse(defaultValue);
    }

    protected Long queryForLong(String sql, Long defaultValue, Object... args) {
        return queryForTypes(sql, Long.class, args).stream().filter(Objects::nonNull).findFirst().orElse(defaultValue);
    }

    protected Double queryForDouble(String sql, Double defaultValue, Object... args) {
        return queryForTypes(sql, Double.class, args).stream().filter(Objects::nonNull).findFirst().orElse(defaultValue);
    }

    protected <T> T queryOne(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> list = query(sql, rowMapper, args);
        return list.isEmpty() ? null : list.get(0);
    }

    protected <T> T namedQueryOne(String sql, RowMapper<T> rowMapper, Map<String, Object> args) {
        List<T> list = namedQuery(sql, rowMapper, args);
        return list.isEmpty() ? null : list.get(0);
    }

    public void setDaoHelper(DaoHelper daoHelper) {
        this.daoHelper = daoHelper;
    }

    public void checkpoint() {
        daoHelper.checkpoint();
    }

}

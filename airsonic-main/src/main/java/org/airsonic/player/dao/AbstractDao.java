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

import com.google.common.base.CaseFormat;
import org.airsonic.player.util.LambdaUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return namedParameterJdbcTemplate;
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
                .map(AbstractDao::convertToDBType)
                .collect(Collectors.toList())
                .toArray();
    }

    protected static Map<String, Object> convertToDBTypes(Map<String, Object> args) {
        return args == null ? null : args.entrySet()
                .stream()
                .map(x -> Pair.of(x.getKey(), convertToDBType(x.getValue())))
                //can't use Collectors.toMap or Collectors.toConcurrentMap due to possible null value mappings
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
    }

    protected static Object convertToDBType(Object x) {
        if (x instanceof Instant) {
            return Timestamp.from((Instant) x);
        }

        if (x instanceof Enum) {
            return ((Enum<?>) x).name();
        }

        if (x instanceof Path) {
            return ((Path) x).toString();
        }

        return x;
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

    protected int batchedUpdate(String sql, Collection<Object[]> batchArgs) {
        long t = System.nanoTime();
        // used to get around postgres's wire limit when sending a large number of params
        int batchSize = 30000 / batchArgs.stream().findAny().map(x -> x.length).orElse(1);
        LOG.trace("Executing query: [{}]", sql);
        int[][] result = getJdbcTemplate().batchUpdate(sql,
            batchArgs.parallelStream().map(AbstractDao::convertToDBTypes).collect(Collectors.toList()),
            batchSize,
            (ps, args) -> {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
            });
        int tally = Arrays.stream(result).flatMapToInt(Arrays::stream).sum();
        LOG.trace("Updated {} rows", tally);
        log(sql, t);
        return tally;
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

    public List<String> queryForStrings(String sql, Object... args) {
        return queryForTypes(sql, String.class, args);
    }

    protected List<Integer> queryForInts(String sql, Object... args) {
        return queryForTypes(sql, Integer.class, args);
    }

    protected List<String> namedQueryForStrings(String sql, Map<String, Object> args) {
        return namedQueryForTypes(sql, String.class, args);
    }

    public Integer queryForInt(String sql, Integer defaultValue, Object... args) {
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

    private static Map<String, SimpleJdbcInsert> insertTemplates = new HashMap<>();
    private static Map<String, Map<String, MethodHandle>> methods = new HashMap<>();
    private static MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static List<Function<String, String>> colNameTransforms = Arrays.asList(Function.identity(),
        c -> CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, c),
        c -> CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, c).toLowerCase());

    protected void registerInserts(String table, String generatedKey, List<String> columns, Class<?> klazz) throws Exception {
        var insert = new SimpleJdbcInsert(jdbcTemplate).withTableName(table);
        if (generatedKey != null) {
            insert.usingGeneratedKeyColumns(generatedKey);
        }
        insertTemplates.put(table, insert);

        // preprocess annotated fields
        var fields = new HashMap<String, Field>();
        for (Field cf : klazz.getDeclaredFields()) {
            fields.putIfAbsent(cf.getName(), cf);
            Column annotation = cf.getAnnotation(Column.class);
            if (annotation != null) {
                fields.putIfAbsent(annotation.value(), cf);
            }
        }

        MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(klazz, lookup);
        methods.put(table, columns.parallelStream().map(LambdaUtils.uncheckFunction(c -> {
            Field f = null;
            var alreadyLooked = new HashSet<String>();

            for (Function<String, String> colNameTransform : colNameTransforms) {
                String lookup = colNameTransform.apply(c);
                if (alreadyLooked.add(lookup)) {
                    f = fields.get(lookup);

                    if (f != null) {
                        LOG.debug("Found suitable field {} (as {}) in class {} for table {} column {}", f.getName(), lookup, klazz.getName(), table, c);
                        break;
                    }
                }
            }
            if (f == null) {
                LOG.error("Could not locate a suitable field in class {} for table {} column {}", klazz.getName(), table, c);
            }
            return Pair.of(c, privateLookup.unreflectGetter(f));
        })).collect(Collectors.toConcurrentMap(Pair::getLeft, Pair::getRight)));
    }

    protected static Integer insert(String table, Object obj) {
        Map<String, Object> args = methods.get(table).entrySet()
                .stream()
                .map(e -> {
                    try {
                        return Pair.of(e.getKey(), convertToDBType(e.getValue().invoke(obj)));
                    } catch (Throwable x) {
                        throw new RuntimeException(x);
                    }
                })
                //can't use Collectors.toMap or Collectors.toConcurrentMap due to possible null value mappings
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
        var template = insertTemplates.get(table);
        if (template.getGeneratedKeyNames() == null || template.getGeneratedKeyNames().length == 0) {
            template.execute(args);
            return null;
        } else {
            var keyHolder = insertTemplates.get(table).executeAndReturnKeyHolder(args);
            return Optional.ofNullable(keyHolder).map(k -> k.getKey()).map(Number::intValue).orElse(null);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Column {
        public String value();
    }
}

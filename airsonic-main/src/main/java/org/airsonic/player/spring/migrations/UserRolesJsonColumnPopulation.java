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
import liquibase.statement.core.UpdateStatement;
import org.airsonic.player.domain.User.Role;
import org.airsonic.player.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserRolesJsonColumnPopulation implements CustomSqlChange {
    private static final Logger LOG = LoggerFactory.getLogger(UserRolesJsonColumnPopulation.class);

    @Override
    public String getConfirmationMessage() {
        return "Column user.roles (for json) populated from user_role";
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
        Map<String, Set<Role>> roles = new HashMap<>();
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (Statement st = conn.createStatement();
                    ResultSet result = st.executeQuery("select ur.username, r.name from role r, user_role ur WHERE r.id=ur.role_id");) {

                while (result.next()) {
                    try {
                        Set<Role> userRoles = roles.computeIfAbsent(result.getString("username"), k -> new HashSet<Role>());
                        userRoles.add(Role.valueOf(result.getString("name").toUpperCase()));
                    } catch (Exception e) {
                        LOG.warn(
                                "Exception while trying to migrate roles for user {}, role {}. Will skip this role and continue",
                                result.getString("username"), result.getString("name"), e);
                    }
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        return roles.entrySet().parallelStream()
                .map(usr -> new UpdateStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "user")
                        .addNewColumnValue("roles", Util.toJson(usr.getValue()))
                        .setWhereClause("username=?")
                        .addWhereParameter(usr.getKey()))
                .toArray(SqlStatement[]::new);
    }

}

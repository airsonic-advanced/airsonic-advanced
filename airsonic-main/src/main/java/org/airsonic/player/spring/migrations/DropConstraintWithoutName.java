package org.airsonic.player.spring.migrations;

import liquibase.change.ColumnConfig;
import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.DropForeignKeyConstraintStatement;
import liquibase.statement.core.DropUniqueConstraintStatement;
import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class DropConstraintWithoutName implements CustomSqlChange {
    private String tableName;
    private String constraintType;
    private List<String> columns;

    public void setTableName(String tableName) {
        this.tableName = StringUtils.lowerCase(tableName);
    }

    public void setConstraintType(String constraintType) {
        this.constraintType = StringUtils.lowerCase(constraintType);
    }

    public void setColumns(String columns) {
        this.columns = Stream.of(StringUtils.split(columns, ",")).map(StringUtils::trimToNull).map(StringUtils::lowerCase).filter(Objects::nonNull).collect(toList());
    }

    @Override
    public String getConfirmationMessage() {
        return "Dropped constraint";
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors()
                .checkRequiredField("tableName", tableName)
                .checkRequiredField("constraintType", constraintType)
                .checkRequiredField("columns", columns);
    }

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        if (columns.isEmpty() || StringUtils.isBlank(constraintType) || StringUtils.isBlank(tableName)) {
            throw new CustomChangeException("One of the parameters is blank");
        }
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }
        String constraintName = null;
        if (conn != null) {
            try (PreparedStatement st1 = conn.prepareStatement(
                    "select * from information_schema.table_constraints tc, information_schema.key_column_usage cu WHERE cu.constraint_name=tc.constraint_name and lower(tc.table_name)=? and lower(tc.constraint_type)=? and lower(cu.column_name) in ("
                            + columns.stream().map(c -> "?").collect(joining("?")) + ")");) {
                st1.setString(1, tableName);
                st1.setString(2, constraintType);
                for (int i = 0; i < columns.size(); i++) {
                    st1.setString(i + 3, columns.get(i));
                }
                try (ResultSet rs1 = st1.executeQuery()) {
                    while (rs1.next()) {
                        constraintName = rs1.getString("constraint_name");
                    }
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        if (constraintName == null) {
            throw new CustomChangeException("No constraint found to drop");
        }

        switch (constraintType) {
            case "foreign key":
                return new SqlStatement[] { new DropForeignKeyConstraintStatement(null, null, tableName, constraintName) };
            case "unique":
                return new SqlStatement[] { new DropUniqueConstraintStatement(null, null, tableName, constraintName, ColumnConfig.arrayFromNames(columns.stream().collect(joining(",")))) };
        }

        return null;
    }

}

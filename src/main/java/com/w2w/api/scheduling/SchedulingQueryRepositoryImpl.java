package com.w2w.api.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Repository
public class SchedulingQueryRepositoryImpl implements SchedulingQueryRepository {
    private static final String QUERY_PATH = "classpath:sql/scheduling/find_employee_shifts_in_range.sql";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JavaType POSITION_LIST_TYPE = OBJECT_MAPPER.getTypeFactory()
            .constructCollectionType(List.class, PositionSummary.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String findEmployeeShiftsInRangeSql;

    public SchedulingQueryRepositoryImpl(
            NamedParameterJdbcTemplate jdbcTemplate,
            ResourceLoader resourceLoader
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.findEmployeeShiftsInRangeSql = loadSql(resourceLoader.getResource(QUERY_PATH));
    }

    @Override
    public List<EmployeeShiftProjection> findAllEmployeeShiftsInRange(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return findAllEmployeeShiftsInRange(companyId, startDate, endDate, List.of(), List.of());
    }

    @Override
    public List<EmployeeShiftProjection> findAllEmployeeShiftsInRange(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        return findAllEmployeeShiftsInRange(companyId, startDate, endDate, positionIds, categoryIds, null);
    }

    @Override
    public List<EmployeeShiftProjection> findAllEmployeeShiftsInRange(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds,
            Integer status
    ) {
        if (!isValidStatus(status)) {
            return List.of();
        }
        List<Integer> positionFilter = normalizeFilterIds(positionIds);
        List<Integer> categoryFilter = normalizeFilterIds(categoryIds);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate)
                .addValue("positionFilterEnabled", !positionFilter.isEmpty())
                .addValue("categoryFilterEnabled", !categoryFilter.isEmpty())
                .addValue("statusFilterEnabled", status != null)
                .addValue("status", status)
                .addValue("positionIds", positionFilter.isEmpty() ? List.of(-1) : positionFilter)
                .addValue("categoryIds", categoryFilter.isEmpty() ? List.of(-1) : categoryFilter);

        return jdbcTemplate.query(
                findEmployeeShiftsInRangeSql,
                parameters,
                new RowMapperImpl()
        );
    }

    private boolean isValidStatus(Integer status) {
        return status == null || (status >= 0 && status <= 5);
    }

    private List<Integer> normalizeFilterIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private final class RowMapperImpl implements RowMapper<EmployeeShiftProjection> {
        private Integer lastEmployeeId = null;
        private List<String> lastPhones = null;
        private List<PositionSummary> lastPositions = null;

        @Override
        public EmployeeShiftProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
            Integer employeeId = getNullableInteger(rs, "employeeId");
            List<String> phones;
            List<PositionSummary> positions;

            if (employeeId != null && employeeId.equals(lastEmployeeId)) {
                phones = lastPhones;
                positions = lastPositions;
            } else {
                phones = deserializeList(rs.getString("phones"));
                positions = deserializePositions(rs.getString("availablePositions"));
                lastEmployeeId = employeeId;
                lastPhones = phones;
                lastPositions = positions;
            }

            return new EmployeeShiftRow(
                    getNullableInteger(rs, "shiftId"),
                    employeeId,
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    getNullableInteger(rs, "empTypeId"),
                    rs.getObject("alertDate", LocalDate.class),
                    phones,
                    positions,
                    rs.getObject("weekCommencing", LocalDate.class),
                    rs.getObject("startTime", LocalTime.class),
                    rs.getObject("endTime", LocalTime.class),
                    getNullableBoolean(rs, "isOvernight"),
                    getNullableInteger(rs, "positionId"),
                    rs.getString("position"),
                    getNullableInteger(rs, "categoryId"),
                    rs.getString("category"),
                    rs.getString("categoryShortDescription"),
                    rs.getString("description"),
                    getNullableFloat(rs, "duration"),
                    getNullableBoolean(rs, "schedulePublished"),
                    getNullableShort(rs, "color")
            );
        }
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Boolean getNullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private Float getNullableFloat(ResultSet rs, String column) throws SQLException {
        float value = rs.getFloat(column);
        return rs.wasNull() ? null : value;
    }

    private Short getNullableShort(ResultSet rs, String column) throws SQLException {
        short value = rs.getShort(column);
        return rs.wasNull() ? null : value;
    }

    private List<String> deserializeList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    List<PositionSummary> deserializePositions(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        try {
            return OBJECT_MAPPER.readValue(value, POSITION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize available positions", exception);
        }
    }

    private String loadSql(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load SQL from " + QUERY_PATH, exception);
        }
    }

    private static final class EmployeeShiftRow implements EmployeeShiftProjection {
        private final Integer shiftId;
        private final Integer employeeId;
        private final String firstName;
        private final String lastName;
        private final Integer empTypeId;
        private final LocalDate alertDate;
        private final List<String> phones;
        private final List<PositionSummary> availablePositions;
        private final LocalDate weekCommencing;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final Boolean isOvernight;
        private final Integer positionId;
        private final String position;
        private final Integer categoryId;
        private final String category;
        private final String categoryShortDescription;
        private final String description;
        private final Float duration;
        private final Boolean schedulePublished;
        private final Short color;

        private EmployeeShiftRow(
                Integer shiftId,
                Integer employeeId,
                String firstName,
                String lastName,
                Integer empTypeId,
                LocalDate alertDate,
                List<String> phones,
                List<PositionSummary> availablePositions,
                LocalDate weekCommencing,
                LocalTime startTime,
                LocalTime endTime,
                Boolean isOvernight,
                Integer positionId,
                String position,
                Integer categoryId,
                String category,
                String categoryShortDescription,
                String description,
                Float duration,
                Boolean schedulePublished,
                Short color
        ) {
            this.shiftId = shiftId;
            this.employeeId = employeeId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.empTypeId = empTypeId;
            this.alertDate = alertDate;
            this.phones = phones;
            this.availablePositions = availablePositions;
            this.weekCommencing = weekCommencing;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isOvernight = isOvernight;
            this.positionId = positionId;
            this.position = position;
            this.categoryId = categoryId;
            this.category = category;
            this.categoryShortDescription = categoryShortDescription;
            this.description = description;
            this.duration = duration;
            this.schedulePublished = schedulePublished;
            this.color = color;
        }

        @Override
        public Integer getShiftId() {
            return shiftId;
        }

        @Override
        public Integer getEmployeeId() {
            return employeeId;
        }

        @Override
        public String getFirstName() {
            return firstName;
        }

        @Override
        public String getLastName() {
            return lastName;
        }

        @Override
        public Integer getEmpTypeId() {
            return empTypeId;
        }

        @Override
        public LocalDate getAlertDate() {
            return alertDate;
        }

        @Override
        public List<String> getPhones() {
            return phones;
        }

        @Override
        public List<PositionSummary> getAvailablePositions() {
            return availablePositions;
        }

        @Override
        public LocalDate getWeekCommencing() {
            return weekCommencing;
        }

        @Override
        public LocalTime getStartTime() {
            return startTime;
        }

        @Override
        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public Integer getPositionId() {
            return positionId;
        }

        @Override
        public String getPosition() {
            return position;
        }

        @Override
        public Integer getCategoryId() {
            return categoryId;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getCategoryShortDescription() {
            return categoryShortDescription;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Float getDuration() {
            return duration;
        }

        @Override
        public Boolean getIsOvernight() {
            return isOvernight;
        }

        @Override
        public Boolean getSchedulePublished() {
            return schedulePublished;
        }

        @Override
        public Short getColor() {
            return color;
        }
    }
}

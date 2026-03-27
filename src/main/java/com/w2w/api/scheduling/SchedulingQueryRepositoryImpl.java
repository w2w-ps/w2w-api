package com.w2w.api.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.w2w.api.position.dto.PositionDto;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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
import java.util.List;

@Repository
public class SchedulingQueryRepositoryImpl implements SchedulingQueryRepository {
    private static final String QUERY_PATH = "classpath:sql/scheduling/find_employee_shifts_in_range.sql";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JavaType POSITION_LIST_TYPE = OBJECT_MAPPER.getTypeFactory()
            .constructCollectionType(List.class, PositionDto.class);

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
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return jdbcTemplate.query(
                findEmployeeShiftsInRangeSql,
                parameters,
                (rs, rowNum) -> mapRow(rs)
        );
    }

    private EmployeeShiftProjection mapRow(ResultSet rs) throws SQLException {
        return new EmployeeShiftRow(
                rs.getInt("employeeId"),
                rs.getString("firstName"),
                rs.getString("lastName"),
                deserializeList(rs.getString("phones")),
                deserializePositions(rs.getString("availablePositions")),
                rs.getObject("weekCommencing", LocalDate.class),
                rs.getObject("startTime", LocalTime.class),
                rs.getObject("endTime", LocalTime.class),
                getNullableBoolean(rs, "isOvernight"),
                rs.getString("position"),
                rs.getString("category"),
                rs.getString("description"),
                getNullableFloat(rs, "duration")
        );
    }

    private Boolean getNullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private Float getNullableFloat(ResultSet rs, String column) throws SQLException {
        float value = rs.getFloat(column);
        return rs.wasNull() ? null : value;
    }

    private List<String> deserializeList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    List<PositionDto> deserializePositions(String value) {
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
        private final Integer employeeId;
        private final String firstName;
        private final String lastName;
        private final List<String> phones;
        private final List<PositionDto> availablePositions;
        private final LocalDate weekCommencing;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final Boolean isOvernight;
        private final String position;
        private final String category;
        private final String description;
        private final Float duration;

        private EmployeeShiftRow(
                Integer employeeId,
                String firstName,
                String lastName,
                List<String> phones,
                List<PositionDto> availablePositions,
                LocalDate weekCommencing,
                LocalTime startTime,
                LocalTime endTime,
                Boolean isOvernight,
                String position,
                String category,
                String description,
                Float duration
        ) {
            this.employeeId = employeeId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phones = phones;
            this.availablePositions = availablePositions;
            this.weekCommencing = weekCommencing;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isOvernight = isOvernight;
            this.position = position;
            this.category = category;
            this.description = description;
            this.duration = duration;
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
        public List<String> getPhones() {
            return phones;
        }

        @Override
        public List<PositionDto> getAvailablePositions() {
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
        public String getPosition() {
            return position;
        }

        @Override
        public String getCategory() {
            return category;
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
    }
}

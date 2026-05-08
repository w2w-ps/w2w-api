package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SchedulingQueryRepositoryImplTest {
    @Test
    @SuppressWarnings("unchecked")
    void mapsAvailablePositionsAsStructuredDtos() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        SchedulingQueryRepositoryImpl repository = new SchedulingQueryRepositoryImpl(
                jdbcTemplate,
                new DefaultResourceLoader()
        );

        when(jdbcTemplate.query(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(MapSqlParameterSource.class),
                ArgumentMatchers.any(RowMapper.class)
        )).thenAnswer(invocation -> {
            RowMapper<EmployeeShiftProjection> rowMapper = invocation.getArgument(2);
            ResultSet resultSet = mock(ResultSet.class);

            when(resultSet.getInt("shiftId")).thenReturn(1001);
            when(resultSet.getInt("employeeId")).thenReturn(101);
            when(resultSet.getString("firstName")).thenReturn("Ava");
            when(resultSet.getString("lastName")).thenReturn("Stone");
            when(resultSet.getString("phones")).thenReturn("111-222,333-444");
            when(resultSet.getString("availablePositions")).thenReturn(
                    "[{\"positionId\":12,\"description\":\"Bartender\"},{\"positionId\":19,\"description\":\"Server\"}]"
            );
            when(resultSet.getObject("weekCommencing", LocalDate.class)).thenReturn(LocalDate.of(2026, 3, 25));
            when(resultSet.getObject("startTime", LocalTime.class)).thenReturn(LocalTime.of(9, 0));
            when(resultSet.getObject("endTime", LocalTime.class)).thenReturn(LocalTime.of(17, 0));
            when(resultSet.getBoolean("isOvernight")).thenReturn(false);
            when(resultSet.wasNull()).thenReturn(false);
            when(resultSet.getInt("positionId")).thenReturn(12);
            when(resultSet.getString("position")).thenReturn("Bartender");
            when(resultSet.getInt("categoryId")).thenReturn(4);
            when(resultSet.getString("category")).thenReturn("Front");
            when(resultSet.getString("categoryShortDescription")).thenReturn("FRT");
            when(resultSet.getString("description")).thenReturn("Opening shift");
            when(resultSet.getFloat("duration")).thenReturn(8.0f);
            when(resultSet.getBoolean("schedulePublished")).thenReturn(true);
            when(resultSet.getShort("color")).thenReturn((short) 1);

            return List.of(rowMapper.mapRow(resultSet, 0));
        });

        List<EmployeeShiftProjection> result = repository.findAllEmployeeShiftsInRange(
                7,
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27)
        );

        assertEquals(1, result.size());
        assertEquals(1001, result.getFirst().getShiftId());
        assertEquals(null, result.getFirst().getEmploymentType());
        assertEquals(2, result.getFirst().getAvailablePositions().size());
        assertEquals(12, result.getFirst().getAvailablePositions().getFirst().positionId());
        assertEquals("Bartender", result.getFirst().getAvailablePositions().getFirst().description());
        assertEquals(12, result.getFirst().getPositionId());
        assertEquals(4, result.getFirst().getCategoryId());
        assertEquals("Front", result.getFirst().getCategory());
        assertEquals("FRT", result.getFirst().getCategoryShortDescription());
        assertEquals(true, result.getFirst().getSchedulePublished());
        assertEquals(List.of("111-222", "333-444"), result.getFirst().getPhones());
        assertEquals((short) 1, result.getFirst().getColor());
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsNullableCategoryAndColorForGroupedRows() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        SchedulingQueryRepositoryImpl repository = new SchedulingQueryRepositoryImpl(
                jdbcTemplate,
                new DefaultResourceLoader()
        );

        when(jdbcTemplate.query(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(MapSqlParameterSource.class),
                ArgumentMatchers.any(RowMapper.class)
        )).thenAnswer(invocation -> {
            RowMapper<EmployeeShiftProjection> rowMapper = invocation.getArgument(2);
            ResultSet resultSet = mock(ResultSet.class);

            when(resultSet.getInt("shiftId")).thenReturn(0);
            when(resultSet.getInt("employeeId")).thenReturn(101);
            when(resultSet.getString("firstName")).thenReturn("Ava");
            when(resultSet.getString("lastName")).thenReturn("Stone");
            when(resultSet.getString("phones")).thenReturn(null);
            when(resultSet.getString("availablePositions")).thenReturn(null);
            when(resultSet.getObject("weekCommencing", LocalDate.class)).thenReturn(LocalDate.of(2026, 3, 25));
            when(resultSet.getObject("startTime", LocalTime.class)).thenReturn(LocalTime.of(9, 0));
            when(resultSet.getObject("endTime", LocalTime.class)).thenReturn(LocalTime.of(17, 0));
            when(resultSet.getBoolean("isOvernight")).thenReturn(false);
            when(resultSet.getInt("positionId")).thenReturn(12);
            when(resultSet.getString("position")).thenReturn("Bartender");
            when(resultSet.getInt("categoryId")).thenReturn(0);
            when(resultSet.getString("category")).thenReturn("FRT");
            when(resultSet.getString("categoryShortDescription")).thenReturn(null);
            when(resultSet.getString("description")).thenReturn("Opening shift");
            when(resultSet.getFloat("duration")).thenReturn(8.0f);
            when(resultSet.getBoolean("schedulePublished")).thenReturn(false);
            when(resultSet.getShort("color")).thenReturn((short) 0);
            when(resultSet.wasNull()).thenReturn(false, false, false, false, false, false, false, false, true);

            return List.of(rowMapper.mapRow(resultSet, 0));
        });

        List<EmployeeShiftProjection> result = repository.findAllEmployeeShiftsInRange(
                7,
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27)
        );

        assertEquals(1, result.size());
        assertEquals(null, result.getFirst().getEmploymentType());
        assertEquals(null, result.getFirst().getColor());
        assertEquals(0, result.getFirst().getCategoryId());
        assertEquals(false, result.getFirst().getSchedulePublished());
    }

    @Test
    @SuppressWarnings("unchecked")
    void passesPositionAndCategoryFilterParametersToSql() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        SchedulingQueryRepositoryImpl repository = new SchedulingQueryRepositoryImpl(
                jdbcTemplate,
                new DefaultResourceLoader()
        );

        when(jdbcTemplate.query(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(MapSqlParameterSource.class),
                ArgumentMatchers.any(RowMapper.class)
        )).thenReturn(List.of());

        repository.findAllEmployeeShiftsInRange(
                7,
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27),
                List.of(12, 12, 19),
                List.of(4)
        );

        ArgumentCaptor<MapSqlParameterSource> parameters = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(
                ArgumentMatchers.anyString(),
                parameters.capture(),
                ArgumentMatchers.any(RowMapper.class)
        );
        MapSqlParameterSource captured = parameters.getValue();

        assertEquals(true, captured.getValue("positionFilterEnabled"));
        assertEquals(true, captured.getValue("categoryFilterEnabled"));
        assertEquals(List.of(12, 19), captured.getValue("positionIds"));
        assertEquals(List.of(4), captured.getValue("categoryIds"));
    }

    @Test
    void passesValidStatusFilterParametersToSql() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        SchedulingQueryRepositoryImpl repository = new SchedulingQueryRepositoryImpl(
                jdbcTemplate,
                new DefaultResourceLoader()
        );

        when(jdbcTemplate.query(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(MapSqlParameterSource.class),
                ArgumentMatchers.any(RowMapper.class)
        )).thenReturn(List.of());

        repository.findAllEmployeeShiftsInRange(
                7,
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27),
                List.of(),
                List.of(),
                5
        );

        ArgumentCaptor<MapSqlParameterSource> parameters = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(
                ArgumentMatchers.anyString(),
                parameters.capture(),
                ArgumentMatchers.any(RowMapper.class)
        );
        MapSqlParameterSource captured = parameters.getValue();

        assertEquals(true, captured.getValue("statusFilterEnabled"));
        assertEquals(5, captured.getValue("status"));
    }

    @Test
    void invalidStatusFiltersReturnEmptyRowsWithoutQueryingSql() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        SchedulingQueryRepositoryImpl repository = new SchedulingQueryRepositoryImpl(
                jdbcTemplate,
                new DefaultResourceLoader()
        );

        List<EmployeeShiftProjection> noTypeRows = repository.findAllEmployeeShiftsInRange(
                7,
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27),
                List.of(),
                List.of(),
                0
        );
        List<EmployeeShiftProjection> unsupportedRows = repository.findAllEmployeeShiftsInRange(
                7,
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27),
                List.of(),
                List.of(),
                99
        );

        assertEquals(List.of(), noTypeRows);
        assertEquals(List.of(), unsupportedRows);
        verifyNoInteractions(jdbcTemplate);
    }
}

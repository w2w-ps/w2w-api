    public Object getShiftsGrouped(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            ShiftGrouping grouping
    ) {
        return switch (grouping) {
            case POSITION -> getShiftsGroupedByDateAndPosition(companyId, startDate, endDate);
            case POSITION_SHIFT_TIMINGS -> new GroupedShiftsResponse(toGroupedDatesFromPositionTimingBuckets(
                    getShiftsGroupedByDayPositionAndTiming(companyId, startDate, endDate)
            ));
            case SHIFT_TIMINGS -> new GroupedShiftsResponse(toGroupedDatesFromDayTimingBuckets(
                    getShiftsGroupedByDayAndTiming(companyId, startDate, endDate)
            ));
            case CATEGORY_SHIFT_TIMINGS -> new GroupedShiftsResponse(toGroupedDatesFromCategoryTimingBuckets(
                    getShiftsGroupedByDayCategoryAndTiming(companyId, startDate, endDate)
            ));
            case CAT_SHIFT_TIMINGS -> new GroupedShiftsResponse(toGroupedDatesFromCategoryTimingBuckets(
                    getShiftsGroupedByDayCategoryShortNameAndTiming(companyId, startDate, endDate)
            ));
        };
    }
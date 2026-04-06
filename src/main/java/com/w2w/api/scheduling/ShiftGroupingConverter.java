package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ShiftGrouping;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ShiftGroupingConverter implements Converter<String, ShiftGrouping> {
    @Override
    public ShiftGrouping convert(String source) {
        return ShiftGrouping.fromApiValue(source);
    }
}

package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ConflictItem;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(40)
class WorkPreferencesRule implements SchedulingValidationRule {
    private static final int PREFERENCE_SLOTS_PER_DAY = 96;
    private static final int PREFERENCE_SLOT_MINUTES = 15;
    private static final String FIELD = "WORK_PREFERENCES";
    private static final String CANNOT_WORK_MESSAGE_TEMPLATE = "%s is set to CANNOT WORK at this time on %s.";
    private static final String DISLIKES_WORK_MESSAGE_TEMPLATE = "%s is set to DISLIKES WORK at this time on %s.";

    @Override
    public List<ConflictItem> validate(SchedulingValidationContext context) {
        if (!context.hasTimedShift()) {
            return List.of();
        }

        boolean dislikesWork = false;
        LocalDate dislikesWorkDate = null;
        for (PreferenceWindow window : context.preferenceWindows()) {
            String preferences = context.resolvedPreference(window.date());
            if (preferences == null || preferences.length() < PREFERENCE_SLOTS_PER_DAY) {
                continue;
            }

            int startSlot = window.startMinute() / PREFERENCE_SLOT_MINUTES;
            int endSlotExclusive = ceilDiv(window.endMinute(), PREFERENCE_SLOT_MINUTES);
            for (int slot = startSlot; slot < endSlotExclusive && slot < PREFERENCE_SLOTS_PER_DAY; slot++) {
                char preference = preferences.charAt(slot);
                if (preference == 'C') {
                    return List.of(new ConflictItem(
                            FIELD,
                            CANNOT_WORK_MESSAGE_TEMPLATE.formatted(
                                    context.employeeDisplayName(),
                                    context.weekdayName(window.date())
                            )
                    ));
                }
                if (preference == 'D') {
                    dislikesWork = true;
                    if (dislikesWorkDate == null) {
                        dislikesWorkDate = window.date();
                    }
                }
            }
        }

        if (dislikesWork) {
            return List.of(new ConflictItem(
                    FIELD,
                    DISLIKES_WORK_MESSAGE_TEMPLATE.formatted(
                            context.employeeDisplayName(),
                            context.weekdayName(dislikesWorkDate)
                    )
            ));
        }

        return List.of();
    }

    private int ceilDiv(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }
}

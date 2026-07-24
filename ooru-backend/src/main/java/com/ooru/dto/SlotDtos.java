package com.ooru.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class SlotDtos {

    public static class CreateSlotRequest {
        @NotNull public LocalDate date;
        @NotNull public LocalTime startTime;
        @NotNull public LocalTime endTime;
    }

    public static class SlotResponse {
        public Long id;
        public LocalDate date;
        public LocalTime startTime;
        public LocalTime endTime;

        public SlotResponse(Long id, LocalDate date, LocalTime startTime, LocalTime endTime) {
            this.id = id;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}

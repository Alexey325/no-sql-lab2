package com.example.nosqllab2.dto.response.schedule;

public record EditingNoticeDto(
        boolean editing,
        String message
) implements ScheduleResponseDto {

    public static EditingNoticeDto of() {
        return new EditingNoticeDto(true, "Ведутся правки в расписании");
    }
}

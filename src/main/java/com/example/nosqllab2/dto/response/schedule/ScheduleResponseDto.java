package com.example.nosqllab2.dto.response.schedule;

/**
 * GET /movies отдаёт либо само расписание, либо уведомление о правках, —
 * как и в лабораторной №1, где эндпоинт возвращал массив фильмов или объект { editing, message }.
 */
public sealed interface ScheduleResponseDto permits MovieScheduleDto, EditingNoticeDto {
}

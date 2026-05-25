package com.example.chat.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 日期时间工具 — 让 Agent 能获取准确的当前时间。
 */
public class DateTimeTool implements java.util.function.Function<DateTimeTool.Request, DateTimeTool.Response> {

    private static final Set<String> VALID_ZONES = Set.of(
            "Asia/Shanghai", "Asia/Tokyo", "America/New_York",
            "America/Los_Angeles", "Europe/London", "Europe/Paris",
            "UTC", "GMT"
    );

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    @Override
    public Response apply(Request request) {
        String zoneId = request.timezone();
        if (zoneId == null || zoneId.isBlank()) {
            zoneId = "Asia/Shanghai";
        }
        if (!VALID_ZONES.contains(zoneId)) {
            return new Response("不支持的时区: " + zoneId + "，支持: " + String.join(", ", VALID_ZONES));
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneId));
        return new Response(FMT.format(now));
    }

    public record Request(String timezone) {
        public Request() {
            this("Asia/Shanghai");
        }
    }

    public record Response(String datetime) {}
}

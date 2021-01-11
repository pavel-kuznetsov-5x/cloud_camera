package com.spqrta.camera2demo.utility.utils

import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter

object DatetimeUtils {
    fun datetimeFromTimestamp(timestamp: Long?, zoneId: ZoneId): LocalDateTime? {
        return timestamp?.let { datetimeFromTimestamp(it, zoneId) }
    }

    fun datetimeFromTimestamp(timestamp: Long, zoneId: ZoneId): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
    }
}

fun String.parseDate(dateTimeFormatter: DateTimeFormatter): LocalDate {
    return LocalDate.parse(this, dateTimeFormatter)
}

fun ZonedDateTime.toLocalDateTimeOnThisZone(): LocalDateTime {
    val zone = ZoneId.systemDefault()
    val zoned = this.withZoneSameInstant(zone)
    return zoned.toLocalDateTime()
}

fun LocalTime.toLocalTimeUtc(): LocalTime {
    val zone = ZoneId.systemDefault()
    val local = ZonedDateTime.ofLocal(
        LocalDateTime.of(LocalDate.now(), this),
        zone,
        null
    )
    val utc = local.withZoneSameInstant(ZoneId.ofOffset("", ZoneOffset.UTC))
    return utc.toLocalTime()
}

fun LocalTime.utcToLocalTime(): LocalTime {
    val local = ZonedDateTime.ofLocal(
        LocalDateTime.of(LocalDate.now(), this),
        ZoneId.ofOffset("", ZoneOffset.UTC),
        null
    )
    return local.toLocalDateTimeOnThisZone().toLocalTime()
}
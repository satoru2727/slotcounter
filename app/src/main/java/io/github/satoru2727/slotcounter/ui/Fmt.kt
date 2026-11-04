package io.github.satoru2727.slotcounter.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("M/d (E) HH:mm", Locale.JAPAN)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.JAPAN)
    private val dateFormatter = DateTimeFormatter.ofPattern("M/d (E)", Locale.JAPAN)

    fun grouped(value: Int): String = String.format(Locale.JAPAN, "%,d", value)

    fun dateTime(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

    fun time(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(timeFormatter)

    fun date(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(dateFormatter)
}

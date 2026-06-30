package com.zinema.app.core.data.db

import androidx.room.TypeConverter

/**
 * Room type converters (blueprint §7). Current entities use only primitives; this
 * List<String> converter is provided for forthcoming columns and to back the
 * `@TypeConverters(Converters::class)` declaration on [AppDatabase].
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String = value?.joinToString(SEPARATOR).orEmpty()

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.takeIf { it.isNotEmpty() }?.split(SEPARATOR) ?: emptyList()

    private companion object {
        // Newline separator — never appears within a genre string.
        const val SEPARATOR = "\n"
    }
}

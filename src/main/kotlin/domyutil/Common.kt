package domyutil

import java.text.DecimalFormat
import java.util.*

fun String.toFloat(format: DecimalFormat) = format.parse(this).toFloat()

/**
 * Creates a type 3 (name based) UUID based on the specified string. The string is converted to [ByteArray] with the
 * default charset - same as calling `UUID.nameUUIDFromBytes(name.toByteArray())`.
 */
fun nameUUIDFromString(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())
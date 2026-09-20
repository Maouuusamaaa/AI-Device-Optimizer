package com.maouuusama.ai.device.optimizer.policy

object DecisionLogFormatter {
    fun toJson(entry: DecisionLogEntry): String = buildString {
        append('{')
        appendNumber("timestampMs", entry.timestampMs)
        appendNumber("availableRamMb", entry.availableRamMb)
        appendNumber("totalRamMb", entry.totalRamMb)
        append(",\"batteryPercent\":")
        if (entry.batteryPercent == null) append("null") else append(entry.batteryPercent)
        append(",\"isCharging\":").append(entry.isCharging)
        append(",\"isGaming\":").append(entry.isGaming)
        append(",\"policyIds\":").appendStringArray(entry.policyIds)
        append(",\"proposedActionIds\":").appendStringArray(entry.proposedActionIds)
        append(",\"policyModes\":").appendStringArray(entry.policyModes)
        append(",\"safetyGateAllowed\":").append(entry.safetyGateAllowed)
        append(",\"safetyGateReasons\":").appendStringArray(entry.safetyGateReasons)
        append(",\"actionExecutionAllowed\":").append(entry.actionExecutionAllowed)
        append('}')
    }

    fun fromJson(json: String): DecisionLogEntry {
        return DecisionLogEntry(
            timestampMs = number(json, "timestampMs"),
            availableRamMb = number(json, "availableRamMb"),
            totalRamMb = number(json, "totalRamMb"),
            batteryPercent = nullableNumber(json, "batteryPercent")?.toInt(),
            isCharging = boolean(json, "isCharging"),
            isGaming = boolean(json, "isGaming"),
            policyIds = stringArray(json, "policyIds"),
            proposedActionIds = stringArray(json, "proposedActionIds"),
            policyModes = stringArray(json, "policyModes"),
            safetyGateAllowed = boolean(json, "safetyGateAllowed"),
            safetyGateReasons = stringArray(json, "safetyGateReasons"),
            actionExecutionAllowed = boolean(json, "actionExecutionAllowed")
        )
    }

    private fun StringBuilder.appendNumber(name: String, value: Long) {
        append(',').append('"').append(name).append("":").append(value)
    }

    private fun StringBuilder.appendStringArray(values: List<String>) {
        append('[')
        values.forEachIndexed { index, value ->
            if (index > 0) append(',')
            append('"').append(escape(value)).append('"')
        }
        append(']')
    }

    private fun escape(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) {
                    append("\\u").append(char.code.toString(16).padStart(4, '0'))
                } else append(char)
            }
        }
    }

    private fun number(json: String, name: String): Long =
        value(json, name).toLongOrNull() ?: error("Invalid numeric field: $name")

    private fun nullableNumber(json: String, name: String): Long? {
        val raw = value(json, name)
        return if (raw == "null") null else raw.toLongOrNull() ?: error("Invalid numeric field: $name")
    }

    private fun boolean(json: String, name: String): Boolean =
        value(json, name).toBooleanStrictOrNull() ?: error("Invalid boolean field: $name")

    private fun stringArray(json: String, name: String): List<String> {
        val raw = value(json, name)
        require(raw.startsWith('[') && raw.endsWith(']')) { "Invalid array field: $name" }
        val body = raw.substring(1, raw.length - 1).trim()
        if (body.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var index = 0
        while (index < body.length) {
            require(body[index] == '"') { "Invalid string array field: $name" }
            val end = findStringEnd(body, index + 1)
            result += unescape(body.substring(index + 1, end))
            index = end + 1
            if (index < body.length) {
                require(body[index] == ',') { "Invalid string array separator: $name" }
                index++
            }
        }
        return result
    }

    private fun value(json: String, name: String): String {
        val key = "\"$name\""
        val start = json.indexOf(key)
        require(start >= 0) { "Missing field: $name" }
        var index = start + key.length
        while (index < json.length && json[index].isWhitespace()) index++
        require(json.getOrNull(index) == ':') { "Missing colon: $name" }
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (json.getOrNull(index) == '"') {
            val end = findStringEnd(json, index + 1)
            return unescape(json.substring(index + 1, end))
        }
        if (json.getOrNull(index) == '[') {
            var depth = 0
            var inString = false
            var escaped = false
            for (i in index until json.length) {
                val c = json[i]
                if (inString) {
                    if (escaped) escaped = false
                    else if (c == '\\') escaped = true
                    else if (c == '"') inString = false
                } else {
                    when (c) {
                        '"' -> inString = true
                        '[' -> depth++
                        ']' -> {
                            depth--
                            if (depth == 0) return json.substring(index, i + 1)
                        }
                    }
                }
            }
            error("Unclosed array: $name")
        }
        val end = json.indexOf(',', index).let { comma ->
            val brace = json.indexOf('}', index)
            when {
                comma < 0 -> brace
                brace < 0 -> comma
                else -> minOf(comma, brace)
            }
        }
        require(end >= index) { "Unterminated field: $name" }
        return json.substring(index, end).trim()
    }

    private fun findStringEnd(value: String, start: Int): Int {
        var escaped = false
        for (i in start until value.length) {
            when {
                escaped -> escaped = false
                value[i] == '\\' -> escaped = true
                value[i] == '"' -> return i
            }
        }
        error("Unclosed JSON string")
    }

    private fun unescape(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            if (value[index] != '\\') {
                append(value[index++])
                continue
            }
            require(index + 1 < value.length) { "Invalid escape" }
            when (val escaped = value[++index]) {
                '\\' -> append('\\')
                '"' -> append('"')
                '/' -> append('/')
                'b' -> append('\b')
                'f' -> append('\u000C')
                'n' -> append('\n')
                'r' -> append('\r')
                't' -> append('\t')
                'u' -> {
                    require(index + 4 < value.length) { "Invalid unicode escape" }
                    append(value.substring(index + 1, index + 5).toInt(16).toChar())
                    index += 4
                }
                else -> error("Unsupported escape: $escaped")
            }
            index++
        }
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.util.TeamCity
import org.jetbrains.kotlin.idea.testFramework.suggestOsNeutralFileName
import java.io.BufferedWriter
import java.io.File

internal fun List<Metric>.writeCSV(name: String, header: Array<String>) {
    fun Metric.append(prefix: String, output: BufferedWriter) {
        val s = "$prefix ${this.name}".trim()
        output.appendLine("$s,${value ?: ""},")
        children.forEach {
            it.append(s, output)
        }
    }

    val statsFile = statsFile(name, "csv")
    statsFile.bufferedWriter().use { output ->
        output.appendLine(header.joinToString())
        forEach { it.append("$name:", output) }
        output.flush()
    }
}

internal fun Metric.writeTeamCityStats(name: String, rawMeasurementName: String = "rawMeasurements", rawMetrics: Boolean = false) {
    fun Metric.append(prefix: String) {
        val s = "${if (this.name == Stats.GEOM_MEAN) prefix else "$prefix:"} ${this.name}".trim()
        value?.let {
            TeamCity.statValue(s, it)
        }
        for (childIndex in children.withIndex()) {
            if (!rawMetrics && childrenName == rawMeasurementName && childIndex.index > 0) break
            childIndex.value.append(s)
        }
    }

    append(name)
}


internal fun List<Metric>.writeJson(name: String) {
    val statsFile = statsFile(name, "json")
    statsFile.bufferedWriter().use { output ->
        output.append(toJson(name))
        output.flush()
    }
}

private fun statsFile(name: String, extension: String) =
    File(pathToResource("stats${statFilePrefix(name)}.$extension")).absoluteFile

private fun List<Metric>.toJson(prefix: String) = joinToString(separator = ",\n", prefix = "{", postfix = "}") { it.toJson(prefix) }

private fun Metric.toJson(prefix: String): String =
    buildString {
        val s = "$prefix $name".trim()
        val nam = if (name.isEmpty()) "_value" else name
        val nm = nam.replace("\"", "\\\"")
        if (children.isNotEmpty()) {
            val cnm = childrenName.replace("\"", "\\\"")
            val list = listOf(Metric(if (nam == childrenName) "_value" else nam, value)) + children
            append("\"").append(cnm).append("\":").append(list.toJson(s))
        } else {
            append("\"").append(nm).append("\":").append("\"${value ?: error ?: ""}\"")
            append(",\"").append(nm).append("_legacy\":").append("\"${s.replace("\"", "\\\"")}\"")
        }
    }


internal fun pathToResource(resource: String) = "build/$resource"

internal fun statFilePrefix(name: String) = if (name.isNotEmpty()) "-${plainname(name)}" else ""

internal fun plainname(name: String) = suggestOsNeutralFileName(name)

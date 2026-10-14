package com.iheanyiekechukwu.irishsearch

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

data class AdvancedFilterDefinition(
    val field: String,
    val label: String,
    val options: List<CatalogOption>,
)

/** Maps the public PATH form controls to the criteria used by its own client. */
object PathAdvancedFilters {
    private val controls = listOf(
        Triple("camp", "Campus", "crit-camp"),
        Triple("divs_code", "Course type", "crit-divs_code"),
        Triple("session", "Part of term", "crit-session"),
        Triple("dept", "Department", "crit-dept"),
        Triple("coll", "College", "crit-coll"),
        Triple("overlap", "Meeting time", "crit-overlap"),
        Triple("instmode", "Instruction method", "crit-instmode"),
        Triple("hours", "Credit hours", "crit-hours"),
        Triple("ur_attr", "Core requirement", "crit-ur_attr"),
        Triple("attributes", "Attribute", "crit-attributes"),
        Triple("stat", "Class status", "crit-stat"),
    )

    fun definitions(html: String): List<AdvancedFilterDefinition> {
        val page = Jsoup.parse(html)
        return controls.mapNotNull { (field, label, id) ->
            val select = page.getElementById(id) ?: return@mapNotNull null
            val options = select.select("option").map { option ->
                CatalogOption(option.attr("value"), option.text().trim())
            }.filter { it.code.isNotBlank() }.distinctBy { it.code }
            if (options.isEmpty()) null else AdvancedFilterDefinition(field, label, options)
        }
    }

    fun addCriteria(
        criteria: JSONArray,
        selected: Map<String, CatalogOption>,
        instructor: String,
    ) {
        if (instructor.isNotBlank()) criteria.add("instructor", instructor.trim())
        selected.forEach { (field, option) ->
            val value = option.code
            if (value.isBlank()) return@forEach
            when (field) {
                "hours" -> {
                    val minimum = value.removeSuffix("+")
                    criteria.add("hours", ">=$minimum")
                    if (!value.endsWith("+")) criteria.add("hours_min", "<=$minimum")
                }
                "ur_attr", "attributes" -> {
                    val parts = value.split(":", limit = 2)
                    criteria.add(parts[0], parts.getOrElse(1) { "Y" })
                }
                else -> criteria.add(field, value)
            }
        }
    }

    private fun JSONArray.add(field: String, value: String) {
        put(JSONObject().put("field", field).put("value", value))
    }
}

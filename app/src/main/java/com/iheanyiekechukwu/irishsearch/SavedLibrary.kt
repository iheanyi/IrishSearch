package com.iheanyiekechukwu.irishsearch

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.libraryStore by preferencesDataStore(name = "saved_library")

data class SavedClass(
    val schoolId: String,
    val semesterCode: String,
    val semesterLabel: String,
    val section: CourseSection,
    val creditHours: String = "",
    val lastSeats: Int? = null,
    val watchSeats: Boolean = false,
) {
    val id: String get() = "$schoolId:$semesterCode:${section.crn}"
}

data class SchedulePlan(
    val id: String,
    val name: String,
    val schoolId: String,
    val semesterCode: String,
    val semesterLabel: String,
    val classIds: Set<String> = emptySet(),
)

data class SavedLibrary(
    val classes: List<SavedClass> = emptyList(),
    val plans: List<SchedulePlan> = emptyList(),
)

class LibraryRepository(private val context: Context) {
    private val key = stringPreferencesKey("library_v1")

    val library: Flow<SavedLibrary> = context.libraryStore.data.map { preferences ->
        decode(preferences[key])
    }

    suspend fun save(school: SchoolCatalog, semester: CatalogOption, section: CourseSection, details: CourseDetails?) {
        update { current ->
            val id = "${school.id}:${semester.code}:${section.crn}"
            val existing = current.classes.firstOrNull { it.id == id }
            val credits = details?.creditHours.orEmpty()
            if (existing != null) current.copy(classes = current.classes.map {
                if (it.id == id && it.creditHours.isBlank() && credits.isNotBlank())
                    it.copy(creditHours = credits) else it
            }) else current.copy(classes = current.classes + SavedClass(
                school.id, semester.code, semester.label, section,
                creditHours = credits,
                lastSeats = availableSeats(details?.seats.orEmpty()),
            ))
        }
    }

    suspend fun remove(id: String) {
        update { current ->
            current.copy(
                classes = current.classes.filterNot { it.id == id },
                plans = current.plans.map { it.copy(classIds = it.classIds - id) },
            )
        }
    }

    suspend fun restore(saved: SavedClass, planIds: Set<String>) {
        update { current ->
            current.copy(
                classes = if (current.classes.any { it.id == saved.id }) current.classes
                    else current.classes + saved,
                plans = current.plans.map { plan ->
                    if (plan.id in planIds && plan.schoolId == saved.schoolId &&
                        plan.semesterCode == saved.semesterCode) {
                        plan.copy(classIds = plan.classIds + saved.id)
                    } else plan
                },
            )
        }
    }

    suspend fun createPlan(name: String, school: SchoolCatalog, semester: CatalogOption): String? {
        val trimmed = name.trim().take(48)
        if (trimmed.isBlank()) return null
        val id = UUID.randomUUID().toString()
        update { current ->
            current.copy(plans = current.plans + SchedulePlan(
                id, trimmed, school.id, semester.code, semester.label,
            ))
        }
        return id
    }

    suspend fun addToPlan(
        planId: String,
        school: SchoolCatalog,
        semester: CatalogOption,
        section: CourseSection,
        details: CourseDetails?,
    ) {
        update { current ->
            val plan = current.plans.firstOrNull { it.id == planId } ?: return@update current
            if (plan.schoolId != school.id || plan.semesterCode != semester.code) return@update current
            val saved = SavedClass(school.id, semester.code, semester.label, section,
                creditHours = details?.creditHours.orEmpty(),
                lastSeats = availableSeats(details?.seats.orEmpty()))
            current.copy(
                classes = if (current.classes.any { it.id == saved.id }) current.classes.map {
                    if (it.id == saved.id && it.creditHours.isBlank())
                        it.copy(creditHours = saved.creditHours) else it
                }
                    else current.classes + saved,
                plans = current.plans.map {
                    if (it.id == planId) it.copy(classIds = it.classIds + saved.id) else it
                },
            )
        }
    }

    suspend fun createPlanWithClass(
        name: String,
        school: SchoolCatalog,
        semester: CatalogOption,
        section: CourseSection,
        details: CourseDetails?,
    ) {
        val trimmed = name.trim().take(48)
        if (trimmed.isBlank()) return
        update { current ->
            val saved = SavedClass(school.id, semester.code, semester.label, section,
                creditHours = details?.creditHours.orEmpty(),
                lastSeats = availableSeats(details?.seats.orEmpty()))
            current.copy(
                classes = if (current.classes.any { it.id == saved.id }) current.classes.map {
                    if (it.id == saved.id && it.creditHours.isBlank())
                        it.copy(creditHours = saved.creditHours) else it
                }
                    else current.classes + saved,
                plans = current.plans + SchedulePlan(
                    UUID.randomUUID().toString(), trimmed, school.id, semester.code,
                    semester.label, setOf(saved.id),
                ),
            )
        }
    }

    suspend fun removePlan(id: String) {
        update { it.copy(plans = it.plans.filterNot { plan -> plan.id == id }) }
    }

    suspend fun duplicatePlan(id: String): String? {
        val duplicateId = UUID.randomUUID().toString()
        var created = false
        update { current ->
            val original = current.plans.firstOrNull { it.id == id } ?: return@update current
            val usedNames = current.plans.filter { it.schoolId == original.schoolId &&
                it.semesterCode == original.semesterCode }.map { it.name.lowercase() }.toSet()
            val base = original.name.take(38)
            var name = "$base copy"
            var number = 2
            while (name.lowercase() in usedNames) {
                name = "$base copy $number"
                number++
            }
            created = true
            current.copy(plans = current.plans + original.copy(id = duplicateId, name = name))
        }
        return duplicateId.takeIf { created }
    }

    suspend fun toggleInPlan(planId: String, classId: String) {
        update { current ->
            val saved = current.classes.firstOrNull { it.id == classId } ?: return@update current
            current.copy(plans = current.plans.map { plan ->
                if (plan.id != planId || plan.schoolId != saved.schoolId ||
                    plan.semesterCode != saved.semesterCode) plan
                else plan.copy(classIds = if (classId in plan.classIds) {
                    plan.classIds - classId
                } else {
                    plan.classIds + classId
                })
            })
        }
    }

    suspend fun setWatch(id: String, enabled: Boolean) {
        update { current ->
            current.copy(classes = current.classes.map { saved ->
                if (saved.id == id) saved.copy(watchSeats = enabled) else saved
            })
        }
    }

    suspend fun updateSeats(id: String, seats: Int) {
        update { current ->
            current.copy(classes = current.classes.map { saved ->
                if (saved.id == id) saved.copy(lastSeats = seats) else saved
            })
        }
    }

    private suspend fun update(transform: (SavedLibrary) -> SavedLibrary) {
        context.libraryStore.edit { preferences ->
            preferences[key] = encode(transform(decode(preferences[key])))
        }
    }

    companion object {
        fun availableSeats(text: String): Int? =
            Regex("Seats Avail(?:able)?:\\s*(\\d+)", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.toIntOrNull()

        private fun decode(raw: String?): SavedLibrary = runCatching {
            if (raw.isNullOrBlank()) return SavedLibrary()
            val root = JSONObject(raw)
            val classes = root.optJSONArray("classes") ?: JSONArray()
            val plans = root.optJSONArray("plans") ?: JSONArray()
            SavedLibrary(
                classes = (0 until classes.length()).mapNotNull { index ->
                    val item = classes.optJSONObject(index) ?: return@mapNotNull null
                    val course = item.optJSONObject("section") ?: return@mapNotNull null
                    SavedClass(
                        schoolId = item.optString("schoolId").ifBlank { SchoolCatalogs.notreDame.id },
                        semesterCode = item.optString("semesterCode"),
                        semesterLabel = item.optString("semesterLabel"),
                        section = CourseSection(
                            key = course.optString("key"),
                            code = course.optString("code"),
                            title = course.optString("title"),
                            section = course.optString("section"),
                            crn = course.optString("crn"),
                            meetingTime = course.optString("meetingTime"),
                            instructor = course.optString("instructor"),
                            status = course.optString("status"),
                            starts = course.optString("starts"),
                            ends = course.optString("ends"),
                            meetingTimes = course.optString("meetingTimes"),
                        ),
                        creditHours = item.optString("creditHours"),
                        lastSeats = item.optInt("lastSeats").takeIf { item.has("lastSeats") },
                        watchSeats = item.optBoolean("watchSeats"),
                    )
                },
                plans = (0 until plans.length()).mapNotNull { index ->
                    val item = plans.optJSONObject(index) ?: return@mapNotNull null
                    val ids = item.optJSONArray("classIds") ?: JSONArray()
                    SchedulePlan(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        schoolId = item.optString("schoolId").ifBlank { SchoolCatalogs.notreDame.id },
                        semesterCode = item.optString("semesterCode"),
                        semesterLabel = item.optString("semesterLabel"),
                        classIds = (0 until ids.length()).map { index ->
                            ids.optString(index).let { id ->
                                if (id.count { it == ':' } == 1) "${SchoolCatalogs.notreDame.id}:$id" else id
                            }
                        }.toSet(),
                    )
                },
            )
        }.getOrDefault(SavedLibrary())

        private fun encode(library: SavedLibrary): String = JSONObject()
            .put("classes", JSONArray().apply {
                library.classes.forEach { saved ->
                    put(JSONObject()
                        .put("schoolId", saved.schoolId)
                        .put("semesterCode", saved.semesterCode)
                        .put("semesterLabel", saved.semesterLabel)
                        .put("section", JSONObject().apply {
                            val course = saved.section
                            put("key", course.key)
                            put("code", course.code)
                            put("title", course.title)
                            put("section", course.section)
                            put("crn", course.crn)
                            put("meetingTime", course.meetingTime)
                            put("instructor", course.instructor)
                            put("status", course.status)
                            put("starts", course.starts)
                            put("ends", course.ends)
                            put("meetingTimes", course.meetingTimes)
                        })
                        .put("creditHours", saved.creditHours)
                        .put("watchSeats", saved.watchSeats)
                        .apply { saved.lastSeats?.let { put("lastSeats", it) } })
                }
            })
            .put("plans", JSONArray().apply {
                library.plans.forEach { plan ->
                    put(JSONObject()
                        .put("id", plan.id)
                        .put("name", plan.name)
                        .put("schoolId", plan.schoolId)
                        .put("semesterCode", plan.semesterCode)
                        .put("semesterLabel", plan.semesterLabel)
                        .put("classIds", JSONArray(plan.classIds.toList())))
                }
            })
            .toString()
    }
}

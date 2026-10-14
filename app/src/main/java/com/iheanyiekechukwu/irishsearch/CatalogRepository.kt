package com.iheanyiekechukwu.irishsearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class CatalogOption(val code: String, val label: String)

data class CatalogFilters(
    val semesters: List<CatalogOption>,
    val subjects: List<CatalogOption>,
    val advanced: List<AdvancedFilterDefinition>,
)

data class CourseSection(
    val key: String,
    val code: String,
    val title: String,
    val section: String,
    val crn: String,
    val meetingTime: String,
    val instructor: String,
    val status: String,
    val starts: String,
    val ends: String,
    val meetingTimes: String,
)

data class SearchResults(val count: Int, val sections: List<CourseSection>)

data class CourseDetails(
    val description: String,
    val creditHours: String,
    val seats: String,
    val campus: String,
    val restrictions: String,
    val attributes: String,
    val bookUrl: String?,
    val requirements: CourseRequirements,
)

/** Public PATH class listings only. No login or student data is used. */
class CatalogRepository(private val school: SchoolCatalog = SchoolCatalogs.notreDame) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun loadFilters(): CatalogFilters = withContext(Dispatchers.IO) {
        val html = request(school.publicSite)
        val semesters = optionsIn(html, "crit-srcdb")
        val subjects = optionsIn(html, "crit-subject")
        require(semesters.isNotEmpty() && subjects.isNotEmpty()) {
            "The class search did not provide its current semesters and subjects."
        }
        CatalogFilters(semesters, subjects, PathAdvancedFilters.definitions(html))
    }

    suspend fun search(
        semester: String,
        subject: String,
        keyword: String,
        advanced: Map<String, CatalogOption> = emptyMap(),
        instructor: String = "",
    ): SearchResults =
        withContext(Dispatchers.IO) {
            require(semester.matches(Regex("[A-Za-z0-9_-]{1,12}"))) { "Choose a semester." }
            val criteria = JSONArray()
            val query = StringBuilder(school.api)
            if (subject.isNotBlank()) {
                criteria.put(JSONObject().put("field", "subject").put("value", subject))
                query.append("&subject=").append(encode(subject))
            }
            if (keyword.isNotBlank()) {
                criteria.put(JSONObject().put("field", "keyword").put("value", keyword))
                query.append("&keyword=").append(encode(keyword))
            }
            PathAdvancedFilters.addCriteria(criteria, advanced, instructor)
            val payload = JSONObject()
                .put("criteria", criteria)
                .put("other", JSONObject().put("srcdb", semester))
            // PATH's own client sends percent-encoded JSON with application/json.
            val response = JSONObject(request(query.toString(), encode(payload.toString())))
            response.optString("fatal").takeIf { it.isNotBlank() }?.let { error(it) }
            val results = response.optJSONArray("results") ?: JSONArray()
            val sections = buildList {
                for (index in 0 until results.length()) {
                    val row = results.optJSONObject(index) ?: continue
                    add(
                        CourseSection(
                            key = row.optString("key"),
                            code = row.optString("code"),
                            title = row.optString("title"),
                            section = row.optString("no"),
                            crn = row.optString("crn"),
                            meetingTime = row.optString("meets"),
                            instructor = row.optString("instr"),
                            status = row.optString("stat"),
                            starts = row.optString("start_date"),
                            ends = row.optString("end_date"),
                            meetingTimes = row.optString("meetingTimes"),
                        )
                    )
                }
            }
            SearchResults(response.optInt("count", sections.size), sections)
        }

    suspend fun details(semester: String, section: CourseSection): CourseDetails =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("group", "code:${section.code}")
                .put("key", "crn:${section.crn}")
                .put("srcdb", semester)
                .put("matched", "")
            val response = JSONObject(request(school.detailsApi, encode(payload.toString())))
            response.optString("fatal").takeIf { it.isNotBlank() }?.let { error(it) }
            CourseDetails(
                description = htmlText(response.optString("description")),
                creditHours = htmlText(response.optString("hours_html").ifBlank {
                    response.optString("hours")
                }),
                seats = htmlText(response.optString("seats")),
                campus = response.optString("campus").ifBlank {
                    htmlText(response.optString("camp_html")).ifBlank {
                        response.optString("campus_code")
                    }
                },
                restrictions = htmlText(response.optString("registration_restrictions")),
                attributes = htmlText(response.optString("attribute_description")),
                bookUrl = Jsoup.parse(response.optString("books_link_html"))
                    .selectFirst("a[href^=https://]")?.attr("href")
                    ?.takeIf { school.id == SchoolCatalogs.notreDame.id &&
                        it.startsWith("https://nd.bncollege.com/") },
                requirements = PathRequirements.from(response),
            )
        }

    private fun optionsIn(html: String, selectId: String): List<CatalogOption> {
        val select = Jsoup.parse(html).getElementById(selectId) ?: return emptyList()
        return select.select("option").map { option ->
            CatalogOption(
                code = option.attr("value"),
                label = option.text().trim(),
            )
        }.filter { it.code.isNotBlank() }.distinctBy { it.code }.toList()
    }

    private fun request(address: String, body: String? = null): String {
        val request = Request.Builder()
            .url(address)
            .apply {
                if (body != null) {
                    post(body.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                }
            }
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Class search returned HTTP ${response.code}.")
            response.body.string()
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun htmlText(value: String): String = Jsoup.parse(value).text().trim()
}

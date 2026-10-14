package com.iheanyiekechukwu.irishsearch

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class SearchUiState(
    val school: SchoolCatalog = SchoolCatalogs.notreDame,
    val semesters: List<CatalogOption> = emptyList(),
    val subjects: List<CatalogOption> = emptyList(),
    val advancedDefinitions: List<AdvancedFilterDefinition> = emptyList(),
    val advancedSelections: Map<String, CatalogOption> = emptyMap(),
    val instructor: String = "",
    val semester: CatalogOption? = null,
    val selectedSubjects: Set<String> = emptySet(),
    val selectedDays: Set<Int> = emptySet(),
    val selectedTimeSlots: Set<String> = emptySet(),
    val keyword: String = "",
    val loadingFilters: Boolean = true,
    val searching: Boolean = false,
    val error: String? = null,
    val results: SearchResults? = null,
    val selectedSection: CourseSection? = null,
    val details: CourseDetails? = null,
    val loadingDetails: Boolean = false,
    val detailError: String? = null,
    val selectedSemester: CatalogOption? = null,
    val selectedSchool: SchoolCatalog? = null,
    val sectionHistory: List<SectionHistory> = emptyList(),
    val relatedCourseCode: String? = null,
    val relatedSections: List<CourseSection> = emptyList(),
    val loadingRelated: Boolean = false,
    val relatedError: String? = null,
    val library: SavedLibrary = SavedLibrary(),
    val libraryLoaded: Boolean = false,
)

data class SectionHistory(
    val section: CourseSection,
    val semester: CatalogOption,
    val school: SchoolCatalog,
    val details: CourseDetails?,
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val libraryRepository = LibraryRepository(application)
    private var filtersJob: Job? = null
    private var searchJob: Job? = null
    private var detailsJob: Job? = null
    private var relatedJob: Job? = null
    private var creditsJob: Job? = null

    var state by mutableStateOf(SearchUiState())
        private set

    init {
        loadFilters()
        viewModelScope.launch {
            libraryRepository.library.collect { library ->
                state = state.copy(library = library, libraryLoaded = true)
                SeatWatchWorker.configure(getApplication(), library)
            }
        }
    }

    fun loadFilters() {
        filtersJob?.cancel()
        val school = state.school
        filtersJob = viewModelScope.launch {
            state = state.copy(loadingFilters = true, error = null)
            try {
                val filters = CatalogRepository(school).loadFilters()
                if (state.school.id != school.id) return@launch
                state = state.copy(
                    semesters = filters.semesters,
                    subjects = filters.subjects,
                    advancedDefinitions = filters.advanced,
                    advancedSelections = state.advancedSelections.filter { (field, option) ->
                        filters.advanced.any { definition ->
                            definition.field == field && definition.options.any { it.code == option.code }
                        }
                    },
                    semester = state.semester?.let { previous ->
                        filters.semesters.firstOrNull { it.code == previous.code }
                    } ?: filters.semesters.first(),
                    selectedSubjects = state.selectedSubjects.filterTo(mutableSetOf()) { selected ->
                        filters.subjects.any { it.code == selected }
                    },
                    loadingFilters = false,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (state.school.id != school.id) return@launch
                state = state.copy(loadingFilters = false, error = error.message ?: "Unable to load semesters.")
            }
        }
    }

    fun chooseSchool(school: SchoolCatalog) {
        if (school.id == state.school.id) return
        searchJob?.cancel()
        detailsJob?.cancel()
        relatedJob?.cancel()
        state = state.copy(
            school = school,
            semesters = emptyList(),
            subjects = emptyList(),
            advancedDefinitions = emptyList(),
            advancedSelections = emptyMap(),
            instructor = "",
            semester = null,
            selectedSubjects = emptySet(),
            selectedDays = emptySet(),
            selectedTimeSlots = emptySet(),
            keyword = "",
            results = null,
            searching = false,
            selectedSection = null,
            selectedSemester = null,
            selectedSchool = null,
            sectionHistory = emptyList(),
            details = null,
            error = null,
        )
        loadFilters()
    }

    fun chooseSemester(value: CatalogOption) {
        searchJob?.cancel()
        state = state.copy(semester = value, results = null, searching = false, error = null)
    }

    fun chooseSubject(value: CatalogOption?) {
        setSubjects(value?.let { setOf(it.code) }.orEmpty())
    }

    fun setSubjects(codes: Set<String>) {
        searchJob?.cancel()
        val valid = state.subjects.map { it.code }.toSet()
        state = state.copy(selectedSubjects = codes.intersect(valid),
            results = null, searching = false, error = null)
    }

    fun setDays(days: Set<Int>) {
        searchJob?.cancel()
        state = state.copy(selectedDays = days.filterTo(mutableSetOf()) { it in 0..6 }, results = null)
    }

    fun setTimeSlots(slots: Set<String>) {
        searchJob?.cancel()
        state = state.copy(selectedTimeSlots = slots.intersect(MeetingPreferences.slots.map { it.id }.toSet()),
            results = null)
    }

    fun setKeyword(value: String) {
        state = state.copy(keyword = value)
    }

    fun setAdvancedFilter(field: String, option: CatalogOption?) {
        searchJob?.cancel()
        val selected = state.advancedSelections.toMutableMap()
        if (option == null) selected.remove(field) else selected[field] = option
        state = state.copy(advancedSelections = selected, results = null)
    }

    fun setInstructor(value: String) {
        searchJob?.cancel()
        state = state.copy(instructor = value, results = null)
    }

    fun clearAdvancedFilters() {
        searchJob?.cancel()
        state = state.copy(advancedSelections = emptyMap(), instructor = "",
            selectedDays = emptySet(), selectedTimeSlots = emptySet(), results = null)
    }

    fun search() {
        val semester = state.semester ?: return
        val school = state.school
        val subjects = state.selectedSubjects.toList()
        val keyword = state.keyword.trim()
        val advanced = state.advancedSelections
        val instructor = state.instructor
        val days = state.selectedDays
        val slots = state.selectedTimeSlots
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            state = state.copy(searching = true, error = null, results = null)
            try {
                val repository = CatalogRepository(school)
                val results = when {
                    subjects.isEmpty() -> repository.search(semester.code, "", keyword, advanced, instructor)
                    subjects.size <= 4 -> coroutineScope {
                        val searches = subjects.map { subject -> async {
                            repository.search(semester.code, subject, keyword, advanced, instructor)
                        } }.awaitAll()
                        SearchResults(searches.sumOf { it.count },
                            searches.flatMap { it.sections }.distinctBy { it.crn })
                    }
                    else -> {
                        val all = repository.search(semester.code, "", keyword, advanced, instructor)
                        val selected = subjects.toSet()
                        val sections = all.sections.filter { it.code.substringBefore(' ') in selected }
                        SearchResults(sections.size, sections)
                    }
                }
                val filtered = if (days.isEmpty() && slots.isEmpty()) results else {
                    val sections = results.sections.filter { MeetingPreferences.matches(it, days, slots) }
                    SearchResults(sections.size, sections)
                }
                if (state.school.id == school.id && state.semester?.code == semester.code) {
                    state = state.copy(searching = false, results = filtered)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (state.school.id == school.id) {
                    state = state.copy(searching = false, error = error.message ?: "Search failed.")
                }
            }
        }
    }

    fun openSection(section: CourseSection, semester: CatalogOption? = null, schoolId: String? = null) {
        val selectedTerm = semester ?: state.semester ?: return
        val school = schoolId?.let(SchoolCatalogs::byId) ?: state.school
        val previous = state.selectedSection?.let { current ->
            val currentTerm = state.selectedSemester ?: return@let null
            val currentSchool = state.selectedSchool ?: return@let null
            SectionHistory(current, currentTerm, currentSchool, state.details)
        }
        detailsJob?.cancel()
        relatedJob?.cancel()
        state = state.copy(
            selectedSection = section,
            selectedSemester = selectedTerm,
            selectedSchool = school,
            sectionHistory = if (previous == null) state.sectionHistory
                else state.sectionHistory + previous,
            details = null,
            detailError = null,
            relatedCourseCode = null,
            relatedSections = emptyList(),
            loadingRelated = false,
            relatedError = null,
            loadingDetails = true,
        )
        detailsJob = viewModelScope.launch {
            try {
                val details = CatalogRepository(school).details(selectedTerm.code, section)
                if (state.selectedSection?.crn == section.crn && state.selectedSchool?.id == school.id) {
                    state = state.copy(details = details, loadingDetails = false)
                    if (state.library.classes.any {
                            it.schoolId == school.id && it.semesterCode == selectedTerm.code &&
                                it.section.crn == section.crn && it.creditHours.isBlank()
                        }) libraryRepository.save(school, selectedTerm, section, details)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (state.selectedSection?.crn == section.crn && state.selectedSchool?.id == school.id) {
                    state = state.copy(
                        detailError = error.message ?: "Details are unavailable.",
                        loadingDetails = false,
                    )
                }
            }
        }
    }

    fun closeSection() {
        detailsJob?.cancel()
        relatedJob?.cancel()
        val previous = state.sectionHistory.lastOrNull()
        if (previous != null) {
            state = state.copy(
                sectionHistory = state.sectionHistory.dropLast(1),
                selectedSection = previous.section,
                selectedSemester = previous.semester,
                selectedSchool = previous.school,
                details = previous.details,
                loadingDetails = false,
                detailError = null,
                relatedCourseCode = null,
                relatedSections = emptyList(),
                loadingRelated = false,
                relatedError = null,
            )
            if (previous.details == null) refreshSelectedSection()
            return
        }
        state = state.copy(selectedSection = null, selectedSemester = null,
            selectedSchool = null, sectionHistory = emptyList(), details = null, detailError = null,
            relatedCourseCode = null, relatedSections = emptyList(), loadingRelated = false,
            relatedError = null)
    }

    fun refreshSelectedSection() {
        val section = state.selectedSection ?: return
        val semester = state.selectedSemester ?: return
        val school = state.selectedSchool ?: return
        if (state.loadingDetails) return
        detailsJob?.cancel()
        state = state.copy(loadingDetails = true, detailError = null)
        detailsJob = viewModelScope.launch {
            try {
                val details = CatalogRepository(school).details(semester.code, section)
                if (state.selectedSection?.crn == section.crn &&
                    state.selectedSemester?.code == semester.code) {
                    state = state.copy(details = details, loadingDetails = false)
                    if (state.library.classes.any { it.schoolId == school.id &&
                            it.semesterCode == semester.code && it.section.crn == section.crn &&
                            it.creditHours.isBlank() }) {
                        libraryRepository.save(school, semester, section, details)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (state.selectedSection?.crn == section.crn) {
                    state = state.copy(loadingDetails = false,
                        detailError = error.message ?: "Refresh failed.")
                }
            }
        }
    }

    fun findRelatedCourse(code: String) {
        val semester = state.selectedSemester ?: return
        val school = state.selectedSchool ?: return
        val normalized = code.trim().uppercase()
        val subject = normalized.substringBefore(' ')
        if (!normalized.matches(Regex("[A-Z]{2,5} \\d{4,5}[A-Z]?"))) return
        relatedJob?.cancel()
        relatedJob = viewModelScope.launch {
            state = state.copy(relatedCourseCode = normalized, relatedSections = emptyList(),
                loadingRelated = true, relatedError = null)
            try {
                val sections = CatalogRepository(school).search(semester.code, subject, normalized)
                    .sections.filter { it.code.equals(normalized, ignoreCase = true) }
                if (state.relatedCourseCode == normalized) {
                    state = state.copy(relatedSections = sections, loadingRelated = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (state.relatedCourseCode == normalized) {
                    state = state.copy(loadingRelated = false,
                        relatedError = error.message ?: "Course lookup failed.")
                }
            }
        }
    }

    fun dismissRelatedCourse() {
        relatedJob?.cancel()
        state = state.copy(relatedCourseCode = null, relatedSections = emptyList(),
            loadingRelated = false, relatedError = null)
    }

    fun saveSelected() {
        val semester = state.selectedSemester ?: return
        val school = state.selectedSchool ?: return
        val section = state.selectedSection ?: return
        val details = state.details
        viewModelScope.launch { libraryRepository.save(school, semester, section, details) }
    }

    fun addSelectedToPlan(planId: String) {
        val semester = state.selectedSemester ?: return
        val school = state.selectedSchool ?: return
        val section = state.selectedSection ?: return
        val details = state.details
        viewModelScope.launch {
            libraryRepository.addToPlan(planId, school, semester, section, details)
        }
    }

    fun createPlanWithSelected(name: String) {
        val semester = state.selectedSemester ?: return
        val school = state.selectedSchool ?: return
        val section = state.selectedSection ?: return
        val details = state.details
        viewModelScope.launch {
            libraryRepository.createPlanWithClass(name, school, semester, section, details)
        }
    }

    fun removeSaved(id: String) {
        viewModelScope.launch { libraryRepository.remove(id) }
    }

    fun restoreSaved(saved: SavedClass, planIds: Set<String>) {
        viewModelScope.launch { libraryRepository.restore(saved, planIds) }
    }

    fun createPlan(name: String, semester: CatalogOption, onCreated: ((String) -> Unit)? = null) {
        val school = state.school
        viewModelScope.launch { libraryRepository.createPlan(name, school, semester)?.let { onCreated?.invoke(it) } }
    }

    fun removePlan(id: String) {
        viewModelScope.launch { libraryRepository.removePlan(id) }
    }

    fun duplicatePlan(id: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            libraryRepository.duplicatePlan(id)?.let(onCreated)
        }
    }

    fun toggleInPlan(planId: String, classId: String) {
        viewModelScope.launch { libraryRepository.toggleInPlan(planId, classId) }
    }

    fun setSeatWatch(id: String, enabled: Boolean) {
        viewModelScope.launch { libraryRepository.setWatch(id, enabled) }
    }

    fun refreshMissingCredits(planId: String) {
        val plan = state.library.plans.firstOrNull { it.id == planId } ?: return
        val missing = state.library.classes.filter {
            it.id in plan.classIds && it.creditHours.isBlank()
        }
        if (missing.isEmpty()) return
        creditsJob?.cancel()
        creditsJob = viewModelScope.launch {
            val school = SchoolCatalogs.byId(plan.schoolId)
            val semester = CatalogOption(plan.semesterCode, plan.semesterLabel)
            val repository = CatalogRepository(school)
            missing.forEach { saved ->
                try {
                    val details = repository.details(semester.code, saved.section)
                    libraryRepository.save(school, semester, saved.section, details)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep the total explicitly incomplete when public details are unavailable.
                }
            }
        }
    }
}

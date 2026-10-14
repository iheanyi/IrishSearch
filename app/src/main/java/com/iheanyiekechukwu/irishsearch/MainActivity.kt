package com.iheanyiekechukwu.irishsearch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import kotlinx.coroutines.launch

internal val DayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

class MainActivity : ComponentActivity() {
    private lateinit var model: SearchViewModel
    private var pendingWatchId: String? = null
    private var pendingSavedClassId by mutableStateOf<String?>(null)
    private var pendingMessage by mutableStateOf<String?>(null)
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val id = pendingWatchId
        pendingWatchId = null
        if (granted && id != null) model.setSeatWatch(id, true)
        else pendingMessage = "Enable notifications to watch seats"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingSavedClassId = savedClassId(intent)
        enableEdgeToEdge()
        setContent {
            IrishSearchTheme {
                model = viewModel()
                IrishSearchScreen(model, ::openBookstore, ::setSeatWatch,
                    pendingSavedClassId, { pendingSavedClassId = null },
                    pendingMessage, { pendingMessage = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSavedClassId = savedClassId(intent)
    }

    private fun savedClassId(intent: Intent?): String? =
        intent?.takeIf { it.action == SeatWatchWorker.ACTION_OPEN_SAVED_CLASS }
            ?.getStringExtra(SeatWatchWorker.EXTRA_SAVED_CLASS_ID)

    private fun openBookstore(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun setSeatWatch(id: String, enabled: Boolean) {
        if (!enabled) {
            model.setSeatWatch(id, false)
            return
        }
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            model.setSeatWatch(id, true)
        } else {
            pendingWatchId = id
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IrishSearchScreen(
    model: SearchViewModel,
    openBookstore: (String) -> Unit,
    setSeatWatch: (String, Boolean) -> Unit,
    openSavedClassId: String?,
    onSavedClassOpened: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
) {
    val state = model.state
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pickingSemester by rememberSaveable { mutableStateOf(false) }
    var pickingSubject by rememberSaveable { mutableStateOf(false) }
    var selectedPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAppMenu by remember { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var previewing by rememberSaveable { mutableStateOf(false) }
    var addingSelectedToSchedule by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    LaunchedEffect(openSavedClassId, state.libraryLoaded) {
        if (openSavedClassId != null && state.libraryLoaded) {
            state.library.classes.firstOrNull { it.id == openSavedClassId }?.let { saved ->
                tab = 1
                model.openSection(saved.section,
                    CatalogOption(saved.semesterCode, saved.semesterLabel), saved.schoolId)
            }
            onSavedClassOpened()
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }
    fun notify(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        snackbarScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = actionLabel != null,
                duration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
    val transfers = rememberLibraryTransfers(state.library, model::importLibrary) { notify(it) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val checkingSeats by remember(context) { SeatWatchWorker.checking(context) }.collectAsState(initial = false)
    fun removeWithUndo(id: String) {
        val saved = state.library.classes.firstOrNull { it.id == id } ?: return
        val planIds = state.library.plans.filter { id in it.classIds }.map { it.id }.toSet()
        model.removeSaved(id)
        notify("Removed from saved classes", "Undo") {
            model.restoreSaved(saved, planIds)
        }
    }
    val section = state.selectedSection
    val selectedId = section?.let {
        "${(state.selectedSchool ?: state.school).id}:${state.selectedSemester?.code}:${it.crn}"
    }
    val isSaved = selectedId != null && state.library.classes.any { it.id == selectedId }
    val goBack = {
        if (previewing) previewing = false else model.closeSection()
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (section != null && !previewing) ScheduleActionFab {
                addingSelectedToSchedule = true
            }
        },
        topBar = {
            if (section == null && tab == 0) {
                TopAppBar(
                    title = {
                        if (tab == 0) Column {
                            Text("Browse", style = MaterialTheme.typography.headlineSmall)
                            Text(state.school.name, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else Text("IrishSearch", style = MaterialTheme.typography.titleMedium)
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showAppMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "App options")
                            }
                            DropdownMenu(expanded = showAppMenu, onDismissRequest = { showAppMenu = false }) {
                                DropdownMenuItem(text = { Text("Backup & restore") }, onClick = {
                                    showAppMenu = false
                                    transfers.openBackup()
                                })
                                DropdownMenuItem(text = { Text("Privacy policy") }, onClick = {
                                    showAppMenu = false
                                    showPrivacy = true
                                })
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            } else if (section != null) {
                TopAppBar(
                    title = { Text(if (previewing) "Schedule preview" else "Course details") },
                    navigationIcon = {
                        IconButton(onClick = goBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!previewing) {
                            IconButton(onClick = {
                                if (isSaved) {
                                    removeWithUndo(selectedId)
                                } else {
                                    model.saveSelected()
                                    notify("Saved on this phone")
                                }
                            }) {
                                Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isSaved) "Remove saved class" else "Save class")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        },
        bottomBar = {
            if (section == null) NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
            ) {
                listOf("Browse", "Saved", "Schedules", "Registration").forEachIndexed { index, label ->
                    val icon = when (index) {
                        0 -> Icons.Default.Search
                        1 -> Icons.Default.Bookmark
                        2 -> Icons.Default.CalendarMonth
                        else -> Icons.Default.Notifications
                    }
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val horizontal = if (maxWidth >= 600.dp) AppSpacing.Expanded else AppSpacing.Page
            val destination: @Composable () -> Unit = {
                when (tab) {
                    0 -> BrowsePage(state, model, horizontal,
                        { pickingSemester = true }, { pickingSubject = true })
                    1 -> SavedPage(state, model, horizontal, setSeatWatch,
                        onRemoveSaved = ::removeWithUndo, onOpenPlans = { tab = 2 })
                    2 -> SchedulesPage(state, model, horizontal, selectedPlanId,
                        { selectedPlanId = it }, onMessage = { notify(it) },
                        onBrowse = { semester -> model.chooseSemester(semester); tab = 0 },
                        onExportCalendar = transfers.exportCalendar, onShareSchedule = transfers.shareImage)
                    else -> RegistrationDashboard(state.library,
                        onOpen = { saved -> model.openSection(saved.section,
                            CatalogOption(saved.semesterCode, saved.semesterLabel), saved.schoolId) },
                        onRefresh = { SeatWatchWorker.refresh(context); notify("Seat check queued. Android will run it when connected and battery is sufficient.") },
                        onWatch = { saved, enabled -> setSeatWatch(saved.id, enabled) },
                        onMessage = { notify(it) }, checking = checkingSeats, horizontal = horizontal)
                }
            }
            if (section != null) {
                PredictiveDetailLayer(
                    enabled = true,
                    onBack = goBack,
                    destination = {
                        if (previewing) CourseDetailPage(state, horizontal, openBookstore,
                            onPreview = { previewing = true },
                            onRelatedCourse = model::findRelatedCourse,
                            activePlanId = selectedPlanId,
                            onSelectPlan = { selectedPlanId = it },
                            onRefresh = model::refreshSelectedSection)
                        else destination()
                    },
                    detail = {
                        if (previewing) CoursePreviewPage(state, horizontal) {
                            addingSelectedToSchedule = true
                        } else CourseDetailPage(state, horizontal, openBookstore,
                            onPreview = { previewing = true },
                            onRelatedCourse = model::findRelatedCourse,
                            activePlanId = selectedPlanId,
                            onSelectPlan = { selectedPlanId = it },
                            onRefresh = model::refreshSelectedSection)
                    },
                )
            } else destination()
        }
    }

    if (pickingSemester) {
        OptionSheet("Semester", state.semesters, state.semester?.code,
            { pickingSemester = false }) {
            model.chooseSemester(it)
            pickingSemester = false
        }
    }
    if (pickingSubject) {
        SubjectSheet(state.subjects, state.selectedSubjects,
            { pickingSubject = false }) { selected ->
            model.setSubjects(selected)
            pickingSubject = false
            model.search()
        }
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy policy") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                    Text("Last updated: October 14, 2026", style = MaterialTheme.typography.bodySmall)
                    Text("IrishSearch is an independent Android app by Iheanyi Ekechukwu for browsing Notre Dame’s public class listings and planning schedules. It does not require an account and contains no advertising or analytics SDKs.")
                    Text("Information sent when you browse", style = MaterialTheme.typography.titleSmall)
                    Text("Loading semesters, searching classes, and opening section details sends HTTPS requests directly from your phone to Notre Dame’s public PATH class search at classsearch.nd.edu. Requests can include your selected semester, subject, search words, filters, instructor query, course code, and section number. The university also receives your IP address and technical request details.")
                    Text("IrishSearch does not route these requests through a developer-operated server, receive university credentials, or access private student records. Notre Dame controls its service and its own data handling, retention, and deletion practices; the developer cannot delete its request logs or guarantee how long it retains them.")
                    Text("Information stored on your phone", style = MaterialTheme.typography.titleSmall)
                    Text("Saved classes, schedule names and contents, personal commitments, latest seat-check status, and seat-alert choices are stored in the app’s private storage. IrishSearch does not upload your library to a developer-operated server. Android backup is disabled. You can remove saved items in the app; clearing the app’s storage in Android settings or uninstalling it removes all its local data.")
                    Text("If you turn on seat alerts, Android periodically checks watched sections against PATH while a network is available and the battery is not low. These requests include the watched course and section identifiers. You can turn alerts off for any section. Notification permission is requested when you enable alerts. Delivery depends on Android scheduling and PATH availability.")
                    Text("Files you export or restore", style = MaterialTheme.typography.titleSmall)
                    Text("Backups, calendars, and schedule images can contain course choices, schedule names, and personal commitments. Android asks you to choose a destination or receiving app. A cloud storage provider or receiving app handles the file under its own policies; IrishSearch does not upload it to a developer-operated server. Exported files remain at their destination after uninstall. Delete them using the provider or app where you saved them. Calendar exports are snapshots, not live synchronization.")
                    Text("Restoring reads only the backup file you select and adds missing records to your phone. Existing records are kept and alerts start off for newly imported classes.")
                    Text("Bookstore links", style = MaterialTheme.typography.titleSmall)
                    Text("Checking textbooks opens the Notre Dame bookstore in your browser. The bookstore and browser handle the visit under their own policies. IrishSearch does not receive your bookstore activity.")
                    Text("Contact and changes", style = MaterialTheme.typography.titleSmall)
                    Text("For questions about IrishSearch or this policy, use the project’s GitHub issues page. Issues are public, so do not include private information. Policy changes are published in the repository and reflected in future app updates.")
                    TextButton(onClick = {
                        runCatching { openBookstore("https://github.com/iheanyi/IrishSearch/issues") }
                            .onFailure {
                                showPrivacy = false
                                notify("Couldn't open GitHub. Visit github.com/iheanyi/IrishSearch/issues in your browser.")
                            }
                    }) { Text("Privacy questions on GitHub") }
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Done") } },
        )
    }
    if (addingSelectedToSchedule && section != null) {
        AddToScheduleDialog(
            state = state,
            onDismiss = { addingSelectedToSchedule = false },
            onAdd = { planId ->
                model.addSelectedToPlan(planId)
                addingSelectedToSchedule = false
                notify("Added to ${state.library.plans.firstOrNull { it.id == planId }?.name ?: "schedule"}")
            },
            onCreate = { name ->
                model.createPlanWithSelected(name)
                addingSelectedToSchedule = false
                notify("Created schedule and added class")
            },
        )
    }
    if (state.relatedCourseCode != null) {
        RelatedCourseSearchSheet(
            code = state.relatedCourseCode,
            semesterLabel = state.selectedSemester?.label.orEmpty(),
            sections = state.relatedSections,
            loading = state.loadingRelated,
            error = state.relatedError,
            onDismiss = model::dismissRelatedCourse,
            onSelectSection = { related ->
                val semester = state.selectedSemester
                val schoolId = state.selectedSchool?.id
                model.openSection(related, semester, schoolId)
                previewing = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BrowsePage(
    state: SearchUiState,
    model: SearchViewModel,
    horizontal: androidx.compose.ui.unit.Dp,
    pickSemester: () -> Unit,
    pickSubject: () -> Unit,
) {
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var choosingAdvanced by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedCourse by rememberSaveable { mutableStateOf<String?>(null) }
    val activeFilters = state.advancedSelections.size +
        (if (state.instructor.isBlank()) 0 else 1) +
        state.selectedDays.size + state.selectedTimeSlots.size
    val browseAll = state.selectedSubjects.isEmpty() && state.keyword.isBlank() && activeFilters == 0
    val courseGroups = remember(state.results) {
        state.results?.sections?.groupBy { it.code to it.title }?.entries?.toList().orEmpty()
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val submitSearch = {
        keyboardController?.hide()
        focusManager.clearFocus()
        expandedCourse = null
        model.search()
    }
    val contentModifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = horizontal, vertical = 8.dp),
    ) {
        item {
            Column(contentModifier.padding(bottom = AppSpacing.Group),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                if (state.loadingFilters) {
                    Row(Modifier.padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Loading the catalog…", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val searchInput: @Composable (Modifier) -> Unit = { fieldModifier ->
                        OutlinedTextField(
                            value = state.keyword,
                            onValueChange = model::setKeyword,
                            modifier = fieldModifier.semantics {
                                contentDescription = "Search by course code, title, or keyword"
                            },
                            placeholder = { Text("Course or keyword", maxLines = 1,
                                overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = if (state.keyword.isBlank()) null else {
                                { IconButton(onClick = { model.setKeyword("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                } }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                        )
                    }
                    val searchAction: @Composable (Modifier) -> Unit = { buttonModifier ->
                        Button(
                            onClick = submitSearch,
                            enabled = state.semester != null && !state.searching,
                            modifier = buttonModifier.heightIn(min = 56.dp).widthIn(min = 96.dp),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Text(if (state.searching) "Loading" else if (browseAll) "Browse all" else "Search")
                        }
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (LocalDensity.current.fontScale >= 1.5f || maxWidth < 320.dp) {
                            Column(Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                                searchInput(Modifier.fillMaxWidth())
                                searchAction(Modifier.align(Alignment.End))
                            }
                        } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                            searchInput(Modifier.weight(1f))
                            searchAction(Modifier)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                        PickerButton(
                            label = state.semester?.label?.replace(" Semester ", " ") ?: "Semester",
                            onClick = pickSemester,
                            enabled = state.semesters.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        )
                        PickerButton(
                            label = when (state.selectedSubjects.size) {
                                0 -> "All subjects"
                                1 -> state.selectedSubjects.first()
                                else -> "${state.selectedSubjects.size} subjects"
                            },
                            onClick = pickSubject,
                            enabled = state.subjects.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            active = state.selectedSubjects.isNotEmpty(),
                        )
                        OutlinedIconButton(onClick = { showAdvanced = true },
                            modifier = Modifier.size(48.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.small,
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                containerColor = if (activeFilters > 0) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                contentColor = if (activeFilters > 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )) {
                            BadgedBox(badge = {
                                if (activeFilters > 0) Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) { Text(activeFilters.toString()) }
                            }) {
                                Icon(Icons.Default.Tune,
                                    contentDescription = if (activeFilters > 0) "$activeFilters active filters" else "More filters")
                            }
                        }
                    }
                }
            }
        }
        if (state.results == null && !state.searching && !state.loadingFilters) {
            item {
                val suggested = listOf("CSE", "MATH", "PSY", "ENGL")
                    .mapNotNull { code -> state.subjects.firstOrNull { it.code == code } }
                if (suggested.isNotEmpty()) {
                    Column(contentModifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Explore subjects", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = pickSubject) { Text("See all") }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                            suggested.forEach { option ->
                                SuggestionChip(
                                    onClick = { model.chooseSubject(option); submitSearch() },
                                    label = { Text(when (option.code) {
                                        "CSE" -> "Computer science"
                                        "MATH" -> "Mathematics"
                                        "PSY" -> "Psychology"
                                        else -> "English"
                                    }) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                        Text("Search by course, choose a subject, or browse the full catalog.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = AppSpacing.Inline))
                    }
                }
            }
        }
        state.error?.let { message ->
            item {
                Surface(modifier = contentModifier, shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = if (state.semesters.isEmpty()) model::loadFilters else submitSearch) {
                            Text("Try again")
                        }
                    }
                }
            }
        }
        if (state.searching) item {
            Row(contentModifier.padding(vertical = AppSpacing.Section),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("Finding classes…", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.results?.let { results ->
            item {
                Row(contentModifier.padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${NumberFormat.getIntegerInstance().format(courseGroups.size)} ${if (courseGroups.size == 1) "course" else "courses"}",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${NumberFormat.getIntegerInstance().format(results.count)} ${if (results.count == 1) "section" else "sections"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(contentModifier,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            }
            if (results.sections.isEmpty()) item {
                EmptyState("No matching classes", "Try another subject, keyword, or meeting time.")
            }
            courseGroups.forEach { group ->
                val groupId = "${group.key.first}|${group.key.second}"
                val expanded = expandedCourse == groupId
                item(key = "course:$groupId") {
                    Column(contentModifier) {
                        CourseGroupRow(group.key.first, group.key.second, group.value.size, expanded) {
                            expandedCourse = if (expanded) null else groupId
                        }
                        // Compose's animation clock follows the Android animator duration scale.
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(tween(180)) + fadeIn(tween(140)),
                            exit = shrinkVertically(tween(160)) + fadeOut(tween(100)),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.Row)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(vertical = AppSpacing.Inline)) {
                                group.value.forEachIndexed { index, section ->
                                    val id = "${state.school.id}:${state.semester?.code}:${section.crn}"
                                    SectionRow(section, state.library.classes.any { it.id == id }) {
                                        model.openSection(section)
                                    }
                                    if (index != group.value.lastIndex) HorizontalDivider(
                                        Modifier.padding(horizontal = AppSpacing.Group),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    }
                }
            }
        }
    }
    if (showAdvanced) {
        val sheetHeight = windowSheetHeight(0.90f, 740.dp)
        ModalBottomSheet(
            onDismissRequest = { showAdvanced = false; choosingAdvanced = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = choosingAdvanced == null),
        ) {
            val sheetFocusManager = LocalFocusManager.current
            val sheetKeyboardController = LocalSoftwareKeyboardController.current
            BackHandler(enabled = choosingAdvanced != null) { choosingAdvanced = null }
            val selectedDefinition = state.advancedDefinitions.firstOrNull { it.field == choosingAdvanced }
            Column(Modifier.fillMaxWidth().height(sheetHeight)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.Page),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                    if (selectedDefinition != null) IconButton(onClick = { choosingAdvanced = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to filters")
                    }
                    Text(selectedDefinition?.label ?: "Filters", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f))
                    if (selectedDefinition == null) TextButton(
                        onClick = model::clearAdvancedFilters, enabled = activeFilters > 0) { Text("Clear") }
                    IconButton(onClick = { sheetKeyboardController?.hide(); showAdvanced = false; choosingAdvanced = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close filters")
                    }
                }
                if (selectedDefinition != null) {
                    val options = listOf(CatalogOption("", "Any")) + selectedDefinition.options
                    LazyColumn(Modifier.weight(1f).selectableGroup(),
                        contentPadding = PaddingValues(horizontal = AppSpacing.Page, vertical = AppSpacing.Inline)) {
                        items(options, key = { it.code }) { option ->
                            val selected = state.advancedSelections[selectedDefinition.field]?.code.orEmpty() == option.code
                            ListItem(
                                headlineContent = { Text(option.label, style = MaterialTheme.typography.bodyLarge) },
                                trailingContent = { RadioButton(selected, onClick = null) },
                                colors = ListItemDefaults.colors(containerColor = if (selected)
                                    MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    headlineColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                    .clip(MaterialTheme.shapes.small).selectable(selected,
                                        role = Role.RadioButton, onClick = {
                                            model.setAdvancedFilter(selectedDefinition.field,
                                                option.takeIf { it.code.isNotBlank() })
                                            choosingAdvanced = null
                                        }),
                            )
                        }
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = AppSpacing.Page, vertical = AppSpacing.Group),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.Group)) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                            Text("Schedule fit", style = MaterialTheme.typography.titleMedium)
                            Text("Choose allowed days and times. Leave a group empty for any day or time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                            Text("Days", style = MaterialTheme.typography.labelLarge)
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val minimumWidth = 64.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                                val columns = ((maxWidth + AppSpacing.Inline) /
                                    (minimumWidth + AppSpacing.Inline)).toInt().coerceIn(1, 4)
                                val chipWidth = (maxWidth - AppSpacing.Inline * (columns - 1)) / columns.toFloat()
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline),
                                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail),
                                    maxItemsInEachRow = columns) {
                                MeetingPreferences.days.forEachIndexed { index, day ->
                                    FilterChip(
                                        selected = index in state.selectedDays,
                                        onClick = { model.setDays(if (index in state.selectedDays)
                                            state.selectedDays - index else state.selectedDays + index) },
                                        label = { Text(day, Modifier.fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                                        modifier = Modifier.width(chipWidth).heightIn(min = 48.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                    )
                                }
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                            Text("Times", style = MaterialTheme.typography.labelLarge)
                            Surface(shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainer) {
                                Column {
                                    MeetingPreferences.slots.forEachIndexed { index, slot ->
                                        val checked = slot.id in state.selectedTimeSlots
                                        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                            .background(if (checked) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .toggleable(checked, role = Role.Checkbox, onValueChange = {
                                                model.setTimeSlots(if (checked) state.selectedTimeSlots - slot.id
                                                    else state.selectedTimeSlots + slot.id)
                                            }).padding(start = AppSpacing.Group, end = AppSpacing.Inline,
                                                top = AppSpacing.Inline, bottom = AppSpacing.Inline),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                                                Text(slot.id.replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                                                        else MaterialTheme.colorScheme.onSurface)
                                                Text(slot.label, style = MaterialTheme.typography.bodyMedium,
                                                    color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Checkbox(checked, onCheckedChange = null)
                                        }
                                        if (index != MeetingPreferences.slots.lastIndex) HorizontalDivider(
                                            Modifier.padding(horizontal = AppSpacing.Group),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                            Text("Every meeting must fit your selections. Classes with unpublished times are excluded when these filters are used.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text("University filters", style = MaterialTheme.typography.titleMedium)
                        Column {
                            state.advancedDefinitions.forEach { definition ->
                                ListItem(
                                    headlineContent = { Text(definition.label, style = MaterialTheme.typography.bodyLarge) },
                                    supportingContent = { Text(state.advancedSelections[definition.field]?.label ?: "Any",
                                        style = MaterialTheme.typography.bodyMedium) },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable {
                                        choosingAdvanced = definition.field
                                    },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = state.instructor, onValueChange = model::setInstructor,
                            modifier = Modifier.fillMaxWidth(), label = { Text("Instructor") },
                            supportingText = { Text("Public results may hide instructor names.") },
                            singleLine = true, shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { sheetKeyboardController?.hide(); sheetFocusManager.clearFocus() }),
                        )
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Button(onClick = { sheetKeyboardController?.hide(); sheetFocusManager.clearFocus(); showAdvanced = false; submitSearch() },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.Page,
                                    vertical = AppSpacing.Row).heightIn(min = 48.dp),
                                enabled = state.semester != null) { Text("Show classes") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerButton(label: String, onClick: () -> Unit, enabled: Boolean,
                         modifier: Modifier = Modifier, active: Boolean = false) {
    OutlinedButton(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, Modifier.size(20.dp))
    }
}

@Composable
private fun CourseGroupRow(code: String, title: String, sectionCount: Int, expanded: Boolean,
                           onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, tween(160), label = "Sections disclosure")
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp)
        .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse sections" else "Show sections",
            onClick = onClick)
        .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
        .padding(vertical = AppSpacing.Row),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(code, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text("$sectionCount ${if (sectionCount == 1) "section" else "sections"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation })
    }
}

@Composable
private fun SectionRow(section: CourseSection, saved: Boolean, onClick: () -> Unit) {
    val meeting = readableMeetings(section).joinToString("; ").ifBlank { "Time to be announced" }
    val availability = when (section.status.trim().uppercase()) {
        "A" -> "Open"
        "F" -> "Full"
        else -> "Status unavailable"
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp)
        .clickable(role = Role.Button, onClickLabel = "Open section ${section.section}", onClick = onClick)
        .padding(horizontal = AppSpacing.Group, vertical = AppSpacing.Row),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
            Text("Section ${section.section}", style = MaterialTheme.typography.labelLarge)
            Text(meeting, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("CRN ${section.crn} · $availability", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if (saved) Icons.Default.Bookmark else Icons.Default.ChevronRight,
            contentDescription = if (saved) "Saved" else null,
            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun CourseRow(section: CourseSection, saved: Boolean, onClick: () -> Unit) {
    val meeting = readableMeetings(section).joinToString("; ").ifBlank { "Time to be announced" }
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
        ListItem(
            overlineContent = { Text("${section.code} · ${section.section}", color = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(section.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(meeting, maxLines = 2,
                overflow = TextOverflow.Ellipsis) },
            trailingContent = {
                if (saved) Icon(Icons.Default.Bookmark, contentDescription = "Saved", tint = MaterialTheme.colorScheme.primary)
                else Icon(Icons.Default.ChevronRight, contentDescription = null)
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        )
        HorizontalDivider()
    }
}

@Composable
private fun SavedPage(
    state: SearchUiState,
    model: SearchViewModel,
    horizontal: androidx.compose.ui.unit.Dp,
    setSeatWatch: (String, Boolean) -> Unit,
    onRemoveSaved: (String) -> Unit,
    onOpenPlans: () -> Unit,
) {
    var addingClass by rememberSaveable { mutableStateOf<String?>(null) }
    val classes = state.library.classes
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = horizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Group),
    ) {
        item { PageHeading("Saved classes", "Keep sections on your phone and watch for open seats.") }
        if (classes.isEmpty()) item {
            EmptyState("Nothing saved yet", "Browse classes and tap a section to save it.")
        }
        items(classes, key = { it.id }) { saved ->
            OutlinedCard(
                onClick = { model.openSection(saved.section,
                    CatalogOption(saved.semesterCode, saved.semesterLabel), saved.schoolId) },
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("${saved.section.code} · ${saved.section.section}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                            Text(saved.section.title, style = MaterialTheme.typography.titleMedium)
                            Text("${SchoolCatalogs.byId(saved.schoolId).name} · ${saved.semesterLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(readableMeetings(saved.section).joinToString("; ")
                                .ifBlank { "Time to be announced" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onRemoveSaved(saved.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove ${saved.section.code}")
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Seat alerts", style = MaterialTheme.typography.labelLarge)
                            Text("Periodic checks while online and battery is not low",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = saved.watchSeats,
                            onCheckedChange = { setSeatWatch(saved.id, it) },
                            modifier = Modifier.semantics {
                                contentDescription = "Seat alerts for ${saved.section.code}, section ${saved.section.section}"
                            })
                    }
                    TextButton(onClick = { addingClass = saved.id }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Text("Manage schedules", Modifier.padding(start = AppSpacing.Inline))
                    }
                }
            }
        }
    }
    addingClass?.let { classId ->
        val saved = classes.firstOrNull { it.id == classId }
        val plans = state.library.plans.filter {
            saved != null && it.schoolId == saved.schoolId && it.semesterCode == saved.semesterCode
        }
        if (saved != null) AlertDialog(
            onDismissRequest = { addingClass = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                    Text("Schedules for ${saved.section.code}", style = MaterialTheme.typography.titleLarge)
                    Text("${saved.semesterLabel} · Section ${saved.section.section}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                    Text(if (plans.isEmpty()) "Create a schedule for this semester to include this section."
                        else "Choose where to include this section.",
                        style = MaterialTheme.typography.bodyMedium)
                    plans.forEach { plan ->
                        val included = classId in plan.classIds
                        ListItem(
                            headlineContent = { Text(plan.name, style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            trailingContent = { Checkbox(included, onCheckedChange = null) },
                            colors = ListItemDefaults.colors(
                                containerColor = if (included) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                headlineColor = if (included) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                .clip(MaterialTheme.shapes.small)
                                .toggleable(included, role = Role.Checkbox,
                                    onValueChange = { model.toggleInPlan(plan.id, classId) }),
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    addingClass = null
                    if (plans.isEmpty()) {
                        model.chooseSchool(SchoolCatalogs.byId(saved.schoolId))
                        model.chooseSemester(CatalogOption(saved.semesterCode, saved.semesterLabel))
                        onOpenPlans()
                    }
                }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (plans.isEmpty()) "Go to schedules" else "Done")
                }
            },
        )
    }
}

@Composable
internal fun PageHeading(title: String, subtitle: String) {
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun EmptyState(title: String, description: String) {
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(vertical = AppSpacing.Expanded),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OptionSheet(
    title: String,
    options: List<CatalogOption>,
    selectedCode: String?,
    onDismiss: () -> Unit,
    onSelect: (CatalogOption) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("") }
    val sheetHeight = windowSheetHeight(0.78f, 620.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(Modifier.fillMaxWidth().height(sheetHeight).padding(horizontal = AppSpacing.Page),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { keyboardController?.hide(); onDismiss() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close $title")
                }
            }
            if (options.size > 8) OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Find $title") },
                singleLine = true, shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
            )
            LazyColumn(Modifier.weight(1f).selectableGroup(),
                contentPadding = PaddingValues(vertical = AppSpacing.Inline),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                items(options.filter { it.label.contains(filter, ignoreCase = true) ||
                    it.code.contains(filter, ignoreCase = true) }, key = { it.code }) { option ->
                    val selected = option.code == selectedCode
                    ListItem(
                        headlineContent = { Text(option.label) },
                        trailingContent = { RadioButton(selected = selected, onClick = null) },
                        colors = ListItemDefaults.colors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            headlineColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .clip(MaterialTheme.shapes.small).selectable(selected = selected,
                            role = Role.RadioButton, onClick = { keyboardController?.hide(); focusManager.clearFocus(); onSelect(option) }),
                    )
                }
            }
        }
    }
}

@Composable
private fun windowSheetHeight(fraction: Float, maximum: Dp): Dp {
    val density = LocalDensity.current
    val size = LocalWindowInfo.current.containerSize
    return with(density) { size.height.toDp() * fraction }.coerceAtMost(maximum)
}

@Immutable
private data class SubjectChoice(val code: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectSheet(
    options: List<CatalogOption>,
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable(initiallySelected,
        stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })) {
        mutableStateOf(initiallySelected)
    }
    val choices = remember(options) {
        options.map { SubjectChoice(it.code, it.label.substringAfter(" - ").trim()) }
    }
    val filteredChoices = remember(choices, filter) {
        val query = filter.trim()
        choices.filter { it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true) }
    }
    val sheetHeight = windowSheetHeight(0.80f, 680.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(Modifier.fillMaxWidth().height(sheetHeight)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.Page,
                vertical = AppSpacing.Inline), verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Subjects", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { keyboardController?.hide(); onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close subjects")
                    }
                }
                Text("Select subjects to include.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(filter, { filter = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Find a subject") }, singleLine = true, shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }))
            }
            LazyColumn(Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = AppSpacing.Page, vertical = AppSpacing.Inline)) {
                items(filteredChoices, key = { it.code }, contentType = { "subject" }) { option ->
                    SubjectChoiceRow(option, option.code in selected) {
                        selected = if (option.code in selected) selected - option.code else selected + option.code
                    }
                }
                if (filteredChoices.isEmpty()) item {
                    EmptyState("No subjects found", "Try a subject name or code.")
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.Page,
                        vertical = AppSpacing.Row), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                        TextButton(onClick = { selected = emptySet() }, enabled = selected.isNotEmpty(),
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear") }
                        Button(onClick = { keyboardController?.hide(); focusManager.clearFocus(); onApply(selected) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(if (selected.isEmpty()) "Browse all subjects" else
                                "Apply ${selected.size} ${if (selected.size == 1) "subject" else "subjects"}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectChoiceRow(option: SubjectChoice, checked: Boolean, onToggle: () -> Unit) {
    val density = LocalDensity.current
    val rowHeight = (with(density) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() } * 2f +
        AppSpacing.Group).coerceAtLeast(72.dp)
    val codeWidth = (56.dp * density.fontScale.coerceAtLeast(1f)).coerceAtMost(80.dp)
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(rowHeight).clip(MaterialTheme.shapes.small)
        .background(if (checked) colors.primaryContainer else Color.Transparent)
        .toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() })
        .semantics(mergeDescendants = true) { contentDescription = "${option.code}, ${option.name}" }
        .padding(start = AppSpacing.Group, end = AppSpacing.Inline, top = AppSpacing.Inline,
            bottom = AppSpacing.Inline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
        Text(option.code, Modifier.width(codeWidth), style = MaterialTheme.typography.labelLarge,
            color = if (checked) colors.onPrimaryContainer else colors.primary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(option.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            color = if (checked) colors.onPrimaryContainer else colors.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

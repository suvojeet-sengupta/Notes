package com.suvojeet.notenext.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as StaggeredGridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.dependency_injection.ViewModelFactory
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.components.ContextualTopAppBar
import com.suvojeet.notenext.ui.components.LabelDialog
import com.suvojeet.notenext.ui.components.NoteItem
import com.suvojeet.notenext.ui.components.MultiActionFab
import com.suvojeet.notenext.ui.components.SearchBar
import com.suvojeet.notenext.ui.components.ColorSelectionDialog
import com.suvojeet.notenext.ui.settings.ThemeMode
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.ui.notes.SortType
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import com.suvojeet.notenext.ui.reminder.ReminderSetDialog
import com.suvojeet.notenext.ui.reminder.RepeatOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onSettingsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onEditLabelsClick: () -> Unit,
    onBinClick: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    events: kotlinx.coroutines.flow.SharedFlow<com.suvojeet.notenext.ui.notes.NotesUiEvent>
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isFabExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()
    var showLabelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReminderSetDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showMoveToProjectDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Observe events from the ViewModel for side-effects like showing toasts or intents.
    // We use `context.getString()` here because events are handled outside the Composable
    // hierarchy and cannot call Composable functions like `stringResource()`.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesUiEvent.SendNotes -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.content)
                    }
                    val chooser = Intent.createChooser(intent, context.getString(R.string.send_notes_via))
                    context.startActivity(chooser)
                }

                is NotesUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.LinkPreviewRemoved -> {
                    Toast.makeText(context, context.getString(R.string.link_preview_removed), Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.ProjectCreated -> {
                    Toast.makeText(context, context.getString(R.string.project_created, event.projectName), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Handle back presses with priority:
    // 1. Deactivate search mode.
    // 2. Clear note selection.
    // 3. Collapse the expanded note view.
    BackHandler(enabled = isSearchActive || isSelectionModeActive || state.expandedNoteId != null) {
        when {
            isSearchActive -> {
                isSearchActive = false
                focusManager.clearFocus()
            }
            isSelectionModeActive -> viewModel.onEvent(NotesEvent.ClearSelection)
            state.expandedNoteId != null -> viewModel.onEvent(NotesEvent.CollapseNote)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AnimatedContent(
                    targetState = isSelectionModeActive,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 90)).togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "TopAppBar Animation"
                ) { targetState ->
                    if (targetState) {
                        ContextualTopAppBar(
                            selectedItemCount = state.selectedNoteIds.size,
                            onClearSelection = { viewModel.onEvent(NotesEvent.ClearSelection) },
                            onTogglePinClick = { viewModel.onEvent(NotesEvent.TogglePinForSelectedNotes) },
                            onReminderClick = { showReminderSetDialog = true },
                            onColorClick = { showColorPickerDialog = true },
                            onArchiveClick = { viewModel.onEvent(NotesEvent.ArchiveSelectedNotes) },
                            onDeleteClick = { showDeleteDialog = true },
                            onCopyClick = { viewModel.onEvent(NotesEvent.CopySelectedNotes) },
                            onSendClick = { viewModel.onEvent(NotesEvent.SendSelectedNotes) },
                            onLabelClick = { showLabelDialog = true },
                            onMoveToProjectClick = { showMoveToProjectDialog = true }
                        )
                    } else {
                        TopAppBar(
                            title = {
                                SearchBar(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    isSearchActive = isSearchActive,
                                    onSearchActiveChange = { isSearchActive = it },
                                    onLayoutToggleClick = { viewModel.onEvent(NotesEvent.ToggleLayout) },
                                    onSortClick = { showSortMenu = true },
                                    layoutType = state.layoutType,
                                    sortMenuExpanded = showSortMenu,
                                    onSortMenuDismissRequest = { showSortMenu = false },
                                    onSortOptionClick = { sortType ->
                                        val newSortType = if (sortType == state.sortType) {
                                            SortType.DATE_MODIFIED // Revert to default if same option is clicked
                                        } else {
                                            sortType
                                        }
                                        viewModel.onEvent(NotesEvent.SortNotes(newSortType))
                                    },
                                    currentSortType = state.sortType
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onMenuClick) {
                                    Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                                }
                            },
                            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                MultiActionFab(
                    isExpanded = isFabExpanded,
                    onExpandedChange = { isFabExpanded = it },
                    onNoteClick = {
                        viewModel.onEvent(NotesEvent.ExpandNote(-1))
                        isFabExpanded = false
                    },
                    onChecklistClick = {
                        viewModel.onEvent(NotesEvent.ExpandNote(-1, "CHECKLIST"))
                        isFabExpanded = false
                    },
                    onProjectClick = {
                        showCreateProjectDialog = true
                        isFabExpanded = false
                    },
                    themeMode = themeMode
                )
            }
        ) { padding ->
            val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(id = R.string.move_to_bin_question)) },
                    text = { Text(stringResource(id = R.string.move_to_bin_message, autoDeleteDays)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.onEvent(NotesEvent.DeleteSelectedNotes)
                                showDeleteDialog = false
                            }
                        ) {
                            Text(stringResource(id = R.string.move_to_bin))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(id = R.string.cancel))
                        }
                    }
                )
            }
            if (showLabelDialog) {
                LabelDialog(
                    labels = state.labels,
                    onDismiss = { showLabelDialog = false },
                    onConfirm = { label ->
                        viewModel.onEvent(NotesEvent.SetLabelForSelectedNotes(label))
                        showLabelDialog = false
                    }
                )
            }
            if (showReminderSetDialog) {
                ReminderSetDialog(
                    onDismissRequest = { showReminderSetDialog = false },
                    onConfirm = { date, time, repeatOption ->
                        viewModel.onEvent(NotesEvent.SetReminderForSelectedNotes(date, time, repeatOption))
                        showReminderSetDialog = false
                    }
                )
            }

            if (showColorPickerDialog) {
                ColorSelectionDialog(
                    onDismiss = { showColorPickerDialog = false },
                    onColorSelected = { color ->
                        viewModel.onEvent(NotesEvent.ChangeColorForSelectedNotes(color))
                        showColorPickerDialog = false
                    },
                    themeMode = themeMode
                )
            }

            if (showCreateProjectDialog) {
                CreateProjectDialog(
                    onDismiss = { showCreateProjectDialog = false },
                    onConfirm = { projectName ->
                        viewModel.onEvent(NotesEvent.CreateProject(projectName))
                        showCreateProjectDialog = false
                    }
                )
            }

            if (showMoveToProjectDialog) {
                MoveToProjectDialog(
                    projects = state.projects,
                    onDismiss = { showMoveToProjectDialog = false },
                    onConfirm = { projectId ->
                        viewModel.onEvent(NotesEvent.MoveSelectedNotesToProject(projectId))
                        showMoveToProjectDialog = false
                    }
                )
            }

            Column(modifier = Modifier.padding(padding).clickable(
                onClick = {
                    if (isFabExpanded) {
                        isFabExpanded = false
                    }
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )) {
                Spacer(modifier = Modifier.height(8.dp))
                val notesToDisplay = if (state.filteredLabel == null) {
                    state.notes
                } else {
                    state.notes.filter { it.note.label == state.filteredLabel }
                }

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (notesToDisplay.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.no_notes_yet),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    val filteredNotes = notesToDisplay.filter { note ->
                        !note.note.isArchived && (note.note.title.contains(searchQuery, ignoreCase = true) || note.note.content.contains(searchQuery, ignoreCase = true))
                    }
                    val pinnedNotes = filteredNotes.filter { it.note.isPinned }
                    val otherNotes = filteredNotes.filter { !it.note.isPinned }

                    when (state.layoutType) {
                        LayoutType.GRID -> {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        Text(
                                            text = stringResource(id = R.string.pinned),
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StaggeredGridItems(pinnedNotes, key = { it.note.id }) { note ->
                                        val isExpanded = state.expandedNoteId == note.note.id
                                        NoteItem(
                                            modifier = Modifier.animateItemPlacement().graphicsLayer { alpha = if (isExpanded) 0f else 1f },
                                            note = note,
                                            isSelected = state.selectedNoteIds.contains(note.note.id),
                                            onNoteClick = {
                                                if (isSelectionModeActive) {
                                                    viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                } else {
                                                    viewModel.onEvent(NotesEvent.ExpandNote(note.note.id))
                                                }
                                            },
                                            onNoteLongClick = {
                                                viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                            }
                                        )
                                    }
                                }

                                if (otherNotes.isNotEmpty()) {
                                    if (pinnedNotes.isNotEmpty()) {
                                        item(span = StaggeredGridItemSpan.FullLine) {
                                            Text(
                                                text = stringResource(id = R.string.others),
                                                modifier = Modifier.padding(8.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    StaggeredGridItems(otherNotes, key = { it.note.id }) { note ->
                                        val isExpanded = state.expandedNoteId == note.note.id
                                        NoteItem(
                                            modifier = Modifier.animateItemPlacement().graphicsLayer { alpha = if (isExpanded) 0f else 1f },
                                            note = note,
                                            isSelected = state.selectedNoteIds.contains(note.note.id),
                                            onNoteClick = {
                                                if (isSelectionModeActive) {
                                                    viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                } else {
                                                    viewModel.onEvent(NotesEvent.ExpandNote(note.note.id))
                                                }
                                            },
                                            onNoteLongClick = {
                                                viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        LayoutType.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(id = R.string.pinned),
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    items(pinnedNotes, key = { it.note.id }) { note ->
                                        val isExpanded = state.expandedNoteId == note.note.id
                                        NoteItem(
                                            modifier = Modifier.animateItemPlacement().graphicsLayer { alpha = if (isExpanded) 0f else 1f },
                                            note = note,
                                            isSelected = state.selectedNoteIds.contains(note.note.id),
                                            onNoteClick = {
                                                if (isSelectionModeActive) {
                                                    viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                } else {
                                                    viewModel.onEvent(NotesEvent.ExpandNote(note.note.id))
                                                }
                                            },
                                            onNoteLongClick = {
                                                viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                            }
                                        )
                                    }
                                }

                                if (otherNotes.isNotEmpty()) {
                                    if (pinnedNotes.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = stringResource(id = R.string.others),
                                                modifier = Modifier.padding(8.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    items(otherNotes, key = { it.note.id }) { note ->
                                        val isExpanded = state.expandedNoteId == note.note.id
                                        NoteItem(
                                            modifier = Modifier.animateItemPlacement().graphicsLayer { alpha = if (isExpanded) 0f else 1f },
                                            note = note,
                                            isSelected = state.selectedNoteIds.contains(note.note.id),
                                            onNoteClick = {
                                                if (isSelectionModeActive) {
                                                    viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                } else {
                                                    viewModel.onEvent(NotesEvent.ExpandNote(note.note.id))
                                                }
                                            },
                                            onNoteLongClick = {
                                                viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.expandedNoteId != null,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            AddEditNoteScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onDismiss = { viewModel.onEvent(NotesEvent.CollapseNote) },
                themeMode = themeMode,
                settingsRepository = settingsRepository,
                events = viewModel.events
            )
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.create_new_project)) },
        text = {
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text(stringResource(id = R.string.project_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(projectName) },
                enabled = projectName.isNotBlank()
            ) {
                Text(stringResource(id = R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun MoveToProjectDialog(
    projects: List<com.suvojeet.notenext.data.Project>,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var selectedProject by remember { mutableStateOf<com.suvojeet.notenext.data.Project?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.move_to_project)) },
        text = {
            Column {
                projects.forEach { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProject = project }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedProject == project),
                            onClick = { selectedProject = project }
                        )
                        Text(text = project.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedProject = null }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedProject == null),
                        onClick = { selectedProject = null }
                    )
                    Text(text = stringResource(id = R.string.none_remove_from_project), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedProject?.id) }
            ) {
                Text(stringResource(id = R.string.move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}


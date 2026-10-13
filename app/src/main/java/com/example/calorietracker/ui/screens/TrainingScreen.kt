package com.example.calorietracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calorietracker.data.PlanDay
import com.example.calorietracker.data.PlanExercise
import com.example.calorietracker.network.AiGuardrails
import com.example.calorietracker.network.GeminiParser
import com.example.calorietracker.ui.components.AppCard
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.MainTab
import com.example.calorietracker.ui.components.MainTabsBar
import com.example.calorietracker.ui.components.PrimaryButton
import com.example.calorietracker.ui.components.RowDivider
import com.example.calorietracker.ui.components.SectionHeader
import com.example.calorietracker.ui.components.Spacing
import com.example.calorietracker.ui.formatDay
import com.example.calorietracker.ui.formatPrescription
import com.example.calorietracker.ui.theme.Accents
import com.example.calorietracker.ui.theme.tabular
import com.example.calorietracker.viewmodel.DayAiState
import com.example.calorietracker.viewmodel.TrainingUiState
import com.example.calorietracker.viewmodel.TrainingViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Quick instructions for "Edit day with AI". */
private val DAY_AI_SUGGESTIONS = listOf(
    "Make it 30 minutes",
    "No equipment",
    "Make it easier",
    "Make it harder",
    "Add a finisher",
)

/** A day picker opened from the menus: move an exercise, or copy a whole day. */
private sealed interface DayPick {
    data class MoveExercise(val exercise: PlanExercise) : DayPick
    data class CopyDay(val fromDayOfWeek: Int) : DayPick
}

@Composable
fun TrainingScreen(
    viewModel: TrainingViewModel,
    onSelectTab: (MainTab) -> Unit,
    onCreateWithAi: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayAi by viewModel.dayAi.collectAsStateWithLifecycle()
    val swappingId by viewModel.swapping.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Open dialogs.
    var editingExercise by remember { mutableStateOf<PlanExercise?>(null) }
    var editingDay by remember { mutableStateOf<PlanDay?>(null) }
    var dayPick by remember { mutableStateOf<DayPick?>(null) }
    var renamingPlan by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = if (message.undo != null) "Undo" else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) message.undo?.let(viewModel::undo)
        }
    }

    Scaffold(
        bottomBar = { MainTabsBar(MainTab.TRAINING, onSelectTab) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.page, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
        ) {
            item {
                ScreenTitle(title = "Training", subtitle = state.plan?.title ?: "Weekly plan") {
                    if (state.plan != null) {
                        PlanMenu(
                            onRename = { renamingPlan = true },
                            onNewPlan = onCreateWithAi,
                            onDeletePlan = { confirmDelete = true },
                        )
                    }
                }
            }
            if (state.isLoading) return@LazyColumn

            if (state.plan == null) {
                item { EmptyPlan(onCreateWithAi = onCreateWithAi, onStartFromScratch = viewModel::createEmptyPlan) }
                return@LazyColumn
            }

            val date = state.selectedDate
            val dow = date.dayOfWeek.value
            val day = state.dayFor(date) ?: PlanDay(dow, "Rest", isRest = true)
            val exercises = state.exercisesFor(date)

            item { WeekStrip(state, onSelect = viewModel::selectDate) }
            item {
                DaySummary(
                    date = date,
                    day = day,
                    done = state.doneCount(date),
                    total = exercises.size,
                    hasExercises = exercises.isNotEmpty(),
                    onRename = { editingDay = day },
                    onEditWithAi = { viewModel.openDayAi(dow) },
                    onCopy = { dayPick = DayPick.CopyDay(dow) },
                    onClear = { viewModel.clearDay(dow) },
                )
            }
            item {
                // The two main ways to change a day, always visible.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { editingExercise = PlanExercise(dayOfWeek = dow, position = 0, name = "", sets = 3, target = "") },
                        label = { Text("Add exercise") },
                        leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                        shape = MaterialTheme.shapes.small,
                    )
                    AssistChip(
                        onClick = { viewModel.openDayAi(dow) },
                        label = { Text("Edit day with AI") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }

            item { SectionHeader("Exercises") }
            item {
                if (exercises.isEmpty()) {
                    AppCard {
                        Text(
                            if (day.isRest) "Rest day. Add an exercise or let AI plan a session if you want to train."
                            else "No exercises yet. Add one, or let AI build this session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    AppCard(contentPadding = PaddingValues(0.dp)) {
                        exercises.forEachIndexed { index, exercise ->
                            if (index > 0) RowDivider(Modifier.padding(start = 62.dp))
                            ExerciseRow(
                                exercise = exercise,
                                done = state.isDone(date, exercise),
                                canCheck = state.canCheck(date),
                                isFirst = index == 0,
                                isLast = index == exercises.lastIndex,
                                isSwapping = swappingId == exercise.id,
                                onToggle = { viewModel.toggleDone(date, exercise) },
                                onEdit = { editingExercise = exercise },
                                onSwapWithAi = { viewModel.swapWithAi(exercise) },
                                onMoveUp = { viewModel.moveUp(exercise) },
                                onMoveDown = { viewModel.moveDown(exercise) },
                                onMoveToDay = { dayPick = DayPick.MoveExercise(exercise) },
                                onDuplicate = { viewModel.duplicate(exercise) },
                                onDelete = { viewModel.deleteExercise(exercise) },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Tap an exercise to edit it, or use ⋮ for more options.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }

    editingExercise?.let { exercise ->
        ExerciseDialog(
            exercise = exercise,
            onSave = { viewModel.saveExercise(it); editingExercise = null },
            onDelete = { viewModel.deleteExercise(exercise); editingExercise = null },
            onDismiss = { editingExercise = null },
        )
    }
    editingDay?.let { day ->
        DayDialog(day = day, onSave = { viewModel.saveDay(it); editingDay = null }, onDismiss = { editingDay = null })
    }
    dayAi?.let { ai ->
        DayAiDialog(
            state = ai,
            dayTitle = state.days[ai.dayOfWeek]?.title.orEmpty(),
            onApply = { viewModel.editDayWithAi(ai.dayOfWeek, it) },
            onDismiss = viewModel::closeDayAi,
        )
    }
    dayPick?.let { pick ->
        val (title, exclude) = when (pick) {
            is DayPick.MoveExercise -> "Move ${pick.exercise.name} to" to pick.exercise.dayOfWeek
            is DayPick.CopyDay -> "Copy ${dayName(pick.fromDayOfWeek)} to" to pick.fromDayOfWeek
        }
        DayPickerDialog(
            title = title,
            subtitle = if (pick is DayPick.CopyDay) "The chosen day's exercises will be replaced." else null,
            exclude = exclude,
            days = state.days,
            onPick = { target ->
                when (pick) {
                    is DayPick.MoveExercise -> viewModel.moveToDay(pick.exercise, target)
                    is DayPick.CopyDay -> viewModel.copyDay(pick.fromDayOfWeek, target)
                }
                dayPick = null
            },
            onDismiss = { dayPick = null },
        )
    }
    if (renamingPlan) {
        TextInputDialog(
            title = "Rename plan",
            label = "Plan name",
            initial = state.plan?.title.orEmpty(),
            onSave = { viewModel.renamePlan(it); renamingPlan = false },
            onDismiss = { renamingPlan = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete training plan?") },
            text = { Text("This removes the whole week and its check-offs. Your nutrition data isn't affected.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePlan(); confirmDelete = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun dayName(dayOfWeek: Int) = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale.getDefault())

@Composable
private fun MenuItem(text: String, icon: ImageVector, enabled: Boolean = true, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = { Text(text, color = if (enabled) color else color.copy(alpha = 0.38f)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = if (destructive) color else MaterialTheme.colorScheme.onSurfaceVariant) },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun PlanMenu(onRename: () -> Unit, onNewPlan: () -> Unit, onDeletePlan: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Plan options")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem("Rename plan", Icons.Outlined.Edit) { open = false; onRename() }
            MenuItem("Regenerate with AI", Icons.Outlined.AutoAwesome) { open = false; onNewPlan() }
            HorizontalDivider()
            MenuItem("Delete plan", Icons.Outlined.DeleteOutline, destructive = true) { open = false; onDeletePlan() }
        }
    }
}

@Composable
private fun EmptyPlan(onCreateWithAi: () -> Unit, onStartFromScratch: () -> Unit) {
    AppCard(contentPadding = PaddingValues(24.dp)) {
        IconBadge(Icons.Outlined.FitnessCenter, MaterialTheme.colorScheme.primary, size = 52.dp)
        Spacer(Modifier.height(18.dp))
        Text("Build your training week", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create your own week exercise by exercise, or describe what you're training for (gym, Hyrox, " +
                "running, home workouts or a sport) and let AI draft it. You can edit everything afterwards.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Create with AI",
            icon = Icons.Outlined.AutoAwesome,
            onClick = onCreateWithAi,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onStartFromScratch, modifier = Modifier.fillMaxWidth()) {
            Text("Build it manually")
        }
    }
}

/** Mon–Sun of this week; a dot marks days with every exercise done. */
@Composable
private fun WeekStrip(state: TrainingUiState, onSelect: (LocalDate) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        state.week.forEach { date ->
            val selected = date == state.selectedDate
            val total = state.exercisesFor(date).size
            val complete = total > 0 && state.doneCount(date) == total
            val isToday = date == state.today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (selected) colors.primary else colors.surfaceContainerLow)
                    .border(
                        BorderStroke(1.dp, if (selected) colors.primary else if (isToday) colors.outline else colors.outlineVariant),
                        MaterialTheme.shapes.medium,
                    )
                    .clickable(role = Role.Tab) { onSelect(date) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val textColor = if (selected) colors.onPrimary else colors.onSurface
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(2.dp))
                Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleSmall.tabular(), color = textColor)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(5.dp)
                        .background(if (complete) (if (selected) colors.onPrimary else Accents.Success) else Color.Transparent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun DaySummary(
    date: LocalDate,
    day: PlanDay,
    done: Int,
    total: Int,
    hasExercises: Boolean,
    onRename: () -> Unit,
    onEditWithAi: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppCard(contentPadding = PaddingValues(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (day.isRest && total == 0) Icons.Outlined.SelfImprovement else Icons.Outlined.FitnessCenter,
                MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(formatDay(date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(day.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Day options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuItem("Rename or set rest day", Icons.Outlined.Edit) { menuOpen = false; onRename() }
                    MenuItem("Edit day with AI", Icons.Outlined.AutoAwesome) { menuOpen = false; onEditWithAi() }
                    MenuItem("Copy to another day", Icons.Outlined.ContentCopy, enabled = hasExercises) { menuOpen = false; onCopy() }
                    HorizontalDivider()
                    MenuItem("Clear exercises", Icons.Outlined.DeleteOutline, enabled = hasExercises, destructive = true) {
                        menuOpen = false; onClear()
                    }
                }
            }
        }
        if (total > 0) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = if (done == total) Accents.Success else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.width(12.dp))
                Text("$done / $total", style = MaterialTheme.typography.labelLarge.tabular())
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: PlanExercise,
    done: Boolean,
    canCheck: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    isSwapping: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onSwapWithAi: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToDay: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(onClick = onEdit, color = Color.Transparent) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(done = done, enabled = canCheck, onClick = onToggle)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                val prescription = formatPrescription(exercise.sets, exercise.target)
                if (prescription.isNotEmpty()) {
                    Text(prescription, style = MaterialTheme.typography.bodyMedium.tabular(), color = MaterialTheme.colorScheme.primary)
                }
                if (exercise.notes.isNotBlank()) {
                    Text(exercise.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box {
                if (isSwapping) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                } else {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Options for ${exercise.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuItem("Edit", Icons.Outlined.Edit) { menuOpen = false; onEdit() }
                    MenuItem("Swap with AI", Icons.Outlined.SwapHoriz) { menuOpen = false; onSwapWithAi() }
                    HorizontalDivider()
                    MenuItem("Move up", Icons.Outlined.ArrowUpward, enabled = !isFirst) { menuOpen = false; onMoveUp() }
                    MenuItem("Move down", Icons.Outlined.ArrowDownward, enabled = !isLast) { menuOpen = false; onMoveDown() }
                    MenuItem("Move to another day", Icons.AutoMirrored.Outlined.DriveFileMove) { menuOpen = false; onMoveToDay() }
                    MenuItem("Duplicate", Icons.Outlined.LibraryAdd) { menuOpen = false; onDuplicate() }
                    HorizontalDivider()
                    MenuItem("Delete", Icons.Outlined.DeleteOutline, destructive = true) { menuOpen = false; onDelete() }
                }
            }
        }
    }
}

/** A round check button: filled with a tick when done, an outline otherwise. Disabled for future days. */
@Composable
private fun CheckCircle(done: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .then(
                if (done) Modifier.background(Accents.Success)
                else Modifier.border(BorderStroke(1.5.dp, outline.copy(alpha = if (enabled) 1f else 0.4f)), CircleShape)
            )
            .clickable(enabled = enabled, role = Role.Checkbox, onClickLabel = if (done) "Mark not done" else "Mark done", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** "Edit day with AI": a free-text instruction plus quick suggestions. */
@Composable
private fun DayAiDialog(state: DayAiState, dayTitle: String, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    var instruction by remember(state.dayOfWeek) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Edit ${dayName(state.dayOfWeek)} with AI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Describe the change to ${dayTitle.ifBlank { "this day" }}. Everything else stays as it is, and you can undo it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it.take(AiGuardrails.MAX_INPUT_CHARS) },
                    placeholder = { Text("e.g. I'm travelling, hotel gym with dumbbells only") },
                    minLines = 2,
                    enabled = !state.isLoading,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAY_AI_SUGGESTIONS.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { instruction = suggestion },
                            label = { Text(suggestion) },
                            enabled = !state.isLoading,
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
                state.error?.let {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = instruction.isNotBlank() && !state.isLoading, onClick = { onApply(instruction) }) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Updating…")
                } else {
                    Text("Apply")
                }
            }
        },
        dismissButton = { TextButton(enabled = !state.isLoading, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DayPickerDialog(
    title: String,
    subtitle: String?,
    exclude: Int,
    days: Map<Int, PlanDay>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                (1..7).filter { it != exclude }.forEach { dow ->
                    Surface(onClick = { onPick(dow) }, color = Color.Transparent, shape = MaterialTheme.shapes.small) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(dayName(dow), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(
                                days[dow]?.title ?: "Rest",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TextInputDialog(title: String, label: String, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(60) },
                label = { Text(label) }, singleLine = true, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onSave(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExerciseDialog(
    exercise: PlanExercise,
    onSave: (PlanExercise) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = exercise.id == 0L
    var name by remember { mutableStateOf(exercise.name) }
    var sets by remember { mutableStateOf(exercise.sets?.toString().orEmpty()) }
    var target by remember { mutableStateOf(exercise.target) }
    var notes by remember { mutableStateOf(exercise.notes) }
    val setsValue = sets.trim().toIntOrNull()
    val setsValid = sets.isBlank() || (setsValue != null && setsValue in 1..GeminiParser.MAX_SETS)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add exercise" else "Edit exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(60) },
                    label = { Text("Exercise") }, placeholder = { Text("e.g. Bench press") },
                    singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sets, onValueChange = { sets = it.filter(Char::isDigit).take(2) },
                        label = { Text("Sets") }, isError = !setsValid, singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.35f),
                    )
                    OutlinedTextField(
                        value = target, onValueChange = { target = it.take(40) },
                        label = { Text("Reps / time / distance") }, placeholder = { Text("8-10 reps") },
                        singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(0.65f),
                    )
                }
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it.take(200) },
                    label = { Text("Notes (optional)") }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                )
                if (!isNew) {
                    TextButton(onClick = onDelete) { Text("Delete exercise", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && setsValid,
                onClick = { onSave(exercise.copy(name = name.trim(), sets = setsValue, target = target.trim(), notes = notes.trim())) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DayDialog(day: PlanDay, onSave: (PlanDay) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(day.title) }
    var isRest by remember { mutableStateOf(day.isRest) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it.take(60) },
                    label = { Text("Day name") }, placeholder = { Text("e.g. Push, Long run, Rest") },
                    singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rest day", modifier = Modifier.weight(1f))
                    Switch(checked = isRest, onCheckedChange = { isRest = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onSave(day.copy(title = title.trim(), isRest = isRest)) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.KeyDefinition
import com.example.data.StudyNote
import com.example.data.Subject
import com.example.data.SubjectColorTag

private enum class ReviewerFilter(val label: String) {
    ALL("All"),
    DEFINITIONS("Definitions"),
    PHOTOS("Tagged Photos"),
    HIGHLIGHTS("Highlights")
}

private data class ReviewerDefinitionEntry(
    val noteId: String,
    val noteTitle: String,
    val photoTag: String,
    val definition: KeyDefinition
)

private data class ReviewerHighlightEntry(
    val noteId: String,
    val noteTitle: String,
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubjectReviewerScreen(
    subject: Subject?,
    notes: List<StudyNote>,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onScanNote: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Reviewer, 1 = Notes
    var activeFilter by remember { mutableStateOf(ReviewerFilter.ALL) }
    var selfQuizMode by remember { mutableStateOf(false) }
    val revealedMap = remember { mutableStateMapOf<String, Boolean>() }

    val subjectStyle = (subject?.colorEnum ?: SubjectColorTag.ROSE).toPinkStyle()

    val allDefinitions = remember(notes) {
        notes.flatMap { note ->
            note.definitions.map { def ->
                ReviewerDefinitionEntry(
                    noteId = note.id,
                    noteTitle = note.title,
                    photoTag = note.photoTag,
                    definition = def
                )
            }
        }
    }

    val taggedPhotoNotes = remember(notes) {
        notes.filter { it.thumbnailBase64.isNotBlank() || it.photoTag.isNotBlank() }
    }

    val allHighlights = remember(notes) {
        notes.flatMap { note ->
            note.highlights.map { phrase ->
                ReviewerHighlightEntry(
                    noteId = note.id,
                    noteTitle = note.title,
                    text = phrase
                )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reviewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (subject != null) {
                            Surface(
                                color = subjectStyle.container,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = subject.iconEnum.toVector(),
                                        contentDescription = null,
                                        tint = subjectStyle.ink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = subject?.name ?: "Subject Reviewer",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${notes.size} notes • ${allDefinitions.size} definitions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (subject != null) {
                ExtendedFloatingActionButton(
                    onClick = { onScanNote(subject.id) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("subject_scan_fab")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DocumentScanner,
                        contentDescription = "Add note to subject"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Note")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_reviewer")
                ) {
                    Text("Reviewer Sheet")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_notes")
                ) {
                    Text("Notes (${notes.size})")
                }
            }

            if (notes.isEmpty()) {
                EmptySubjectReviewerState(
                    subjectName = subject?.name ?: "this subject",
                    onAddNote = { subject?.id?.let(onScanNote) }
                )
            } else if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (selfQuizMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Self-Quiz Mode",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Tap any definition card to reveal",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = selfQuizMode,
                                    onCheckedChange = {
                                        selfQuizMode = it
                                        revealedMap.clear()
                                    },
                                    modifier = Modifier.testTag("self_quiz_switch")
                                )
                            }
                        }
                    }

                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReviewerFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = activeFilter == filter,
                                    onClick = { activeFilter = filter },
                                    label = { Text(filter.label) }
                                )
                            }
                        }
                    }

                    if ((activeFilter == ReviewerFilter.ALL || activeFilter == ReviewerFilter.PHOTOS) && taggedPhotoNotes.isNotEmpty()) {
                        item {
                            SectionTitle(
                                icon = Icons.Rounded.PhotoLibrary,
                                title = "Tagged Page Captures (${taggedPhotoNotes.size})"
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(taggedPhotoNotes, key = { "photo_${it.id}" }) { note ->
                                    TaggedPhotoCard(
                                        note = note,
                                        onClick = { onOpenNote(note.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (activeFilter == ReviewerFilter.ALL || activeFilter == ReviewerFilter.DEFINITIONS) {
                        item {
                            SectionTitle(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "Key Definitions (${allDefinitions.size})"
                            )
                        }
                        if (allDefinitions.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No key definitions highlighted yet. Open any note to highlight terms right on screen.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(
                                allDefinitions,
                                key = { "${it.noteId}_${it.definition.id}_${it.definition.term}" }
                            ) { entry ->
                                val key = "${entry.noteId}_${entry.definition.id}_${entry.definition.term}"
                                val isRevealed = !selfQuizMode || (revealedMap[key] == true)
                                ReviewerDefinitionCard(
                                    entry = entry,
                                    isRevealed = isRevealed,
                                    onToggleReveal = {
                                        if (selfQuizMode) {
                                            revealedMap[key] = !(revealedMap[key] ?: false)
                                        } else {
                                            onOpenNote(entry.noteId)
                                        }
                                    },
                                    onOpenNote = { onOpenNote(entry.noteId) }
                                )
                            }
                        }
                    }

                    if ((activeFilter == ReviewerFilter.ALL || activeFilter == ReviewerFilter.HIGHLIGHTS) && allHighlights.isNotEmpty()) {
                        item {
                            SectionTitle(
                                icon = Icons.Rounded.Bookmark,
                                title = "Highlighted Takeaways (${allHighlights.size})"
                            )
                        }
                        items(
                            allHighlights,
                            key = { "${it.noteId}_${it.text.hashCode()}" }
                        ) { highlight ->
                            Surface(
                                color = subjectStyle.container.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenNote(highlight.noteId) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = highlight.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = subjectStyle.ink,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "From ${highlight.noteTitle}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }) { note ->
                        StudyNoteSummaryCard(
                            note = note,
                            onClick = { onOpenNote(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun TaggedPhotoCard(
    note: StudyNote,
    onClick: () -> Unit
) {
    val thumb = rememberDecodedThumbnail(note.thumbnailBase64)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(190.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = note.photoTag.ifBlank { note.title },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                if (note.photoTag.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = note.photoTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReviewerDefinitionCard(
    entry: ReviewerDefinitionEntry,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onOpenNote: () -> Unit
) {
    val accent = SubjectColorTag.fromRaw(entry.definition.highlightColor).toPinkStyle()
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accent.container),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleReveal)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.container,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = entry.definition.term,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent.ink,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                IconButton(
                    onClick = onOpenNote,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = "Edit in note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (isRevealed) {
                Text(
                    text = entry.definition.definition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tap card to reveal definition",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildString {
                    append(entry.noteTitle)
                    if (entry.photoTag.isNotBlank()) {
                        append(" • ")
                        append(entry.photoTag)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudyNoteSummaryCard(
    note: StudyNote,
    onClick: () -> Unit
) {
    val thumb = rememberDecodedThumbnail(note.thumbnailBase64)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = note.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.photoTag.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = note.photoTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.extractedText.ifBlank { "Tap to edit text & highlight definitions" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${note.definitions.size} definitions • ${note.highlights.size} highlights",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun EmptySubjectReviewerState(
    subjectName: String,
    onAddNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_notes),
            contentDescription = "No notes yet illustration",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(22.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notes in $subjectName yet",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Snap a printed page or neat handwritten note to extract editable text and key definitions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onAddNote,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("empty_subject_add_note_button")
        ) {
            Icon(imageVector = Icons.Rounded.DocumentScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan / Add First Note")
        }
    }
}

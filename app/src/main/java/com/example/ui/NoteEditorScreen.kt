package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.KeyDefinition
import com.example.data.StudyNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: StudyNote?,
    isBusy: Boolean,
    isSpottingDefinitions: Boolean,
    statusMessage: String?,
    onConsumeStatus: () -> Unit,
    onSpotDefinitionsFromText: (String, (List<String>, List<KeyDefinition>) -> Unit) -> Unit,
    onSaveChanges: (
        noteId: String,
        subjectId: String,
        title: String,
        extractedText: String,
        photoTag: String,
        thumbnailBase64: String,
        highlights: List<String>,
        definitions: List<KeyDefinition>
    ) -> Unit,
    onDeleteNote: (String) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusMessage)
            onConsumeStatus()
        }
    }

    if (note == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = { Text("Note") }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Note not found.")
            }
        }
        return
    }

    var title by remember(note.id) { mutableStateOf(note.title) }
    var photoTag by remember(note.id) { mutableStateOf(note.photoTag) }
    var extractedText by remember(note.id) { mutableStateOf(note.extractedText) }
    var highlights by remember(note.id) { mutableStateOf(note.highlights) }
    var definitions by remember(note.id) { mutableStateOf(note.definitions) }

    var modeTab by remember { mutableIntStateOf(0) } // 0 = Highlight On Screen, 1 = Edit Text
    var showAddDefDialog by remember { mutableStateOf(false) }
    var prefillTerm by remember { mutableStateOf("") }
    var prefillBody by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val previewBitmap = rememberDecodedThumbnail(note.thumbnailBase64)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                            .testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = { Text("Highlight & Edit Note") },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = photoTag.ifBlank { title },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Note Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_title_input")
            )

            OutlinedTextField(
                value = photoTag,
                onValueChange = { if (it.length <= 80) photoTag = it },
                label = { Text("Photo Tag") },
                placeholder = { Text("e.g., History Ch.3 • Page 24") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_photo_tag_input")
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = modeTab == 0,
                    onClick = { modeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("editor_highlight_tab")
                ) {
                    Text("Highlight On Screen")
                }
                SegmentedButton(
                    selected = modeTab == 1,
                    onClick = { modeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("editor_text_tab")
                ) {
                    Text("Edit Text")
                }
            }

            if (modeTab == 0) {
                OnScreenHighlighterPanel(
                    extractedText = extractedText,
                    highlights = highlights,
                    onToggleHighlight = { segment ->
                        highlights = if (highlights.any { it.equals(segment, ignoreCase = true) }) {
                            highlights.filterNot { it.equals(segment, ignoreCase = true) }
                        } else {
                            (highlights + segment).take(40)
                        }
                    },
                    onQuickDefineFromSegment = { segment ->
                        val splitIdx = segment.indexOf(':').takeIf { it in 2..40 }
                        if (splitIdx != null) {
                            prefillTerm = segment.substring(0, splitIdx).trim()
                            prefillBody = segment.substring(splitIdx + 1).trim()
                        } else {
                            prefillTerm = ""
                            prefillBody = segment
                        }
                        showAddDefDialog = true
                    }
                )
            } else {
                OutlinedTextField(
                    value = extractedText,
                    onValueChange = { if (it.length <= 25000) extractedText = it },
                    label = { Text("Editable Note Text") },
                    minLines = 7,
                    maxLines = 16,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_text_input")
                )
            }

            KeyDefinitionsEditorSection(
                definitions = definitions,
                isSpotting = isSpottingDefinitions,
                onAiSpotDefinitions = {
                    onSpotDefinitionsFromText(extractedText) { spottedHighlights, spottedDefs ->
                        highlights = (highlights + spottedHighlights).distinct().take(40)
                        val existingTerms = definitions.map { it.term.lowercase() }.toSet()
                        val merged = definitions + spottedDefs.filterNot { it.term.lowercase() in existingTerms }
                        definitions = merged.take(40)
                    }
                },
                onAddDefinitionClick = {
                    prefillTerm = ""
                    prefillBody = ""
                    showAddDefDialog = true
                },
                onRemoveDefinition = { target ->
                    definitions = definitions.filterNot { it.id == target.id && it.term == target.term }
                }
            )

            Button(
                onClick = {
                    onSaveChanges(
                        note.id,
                        note.subjectId,
                        title,
                        extractedText,
                        photoTag,
                        note.thumbnailBase64,
                        highlights,
                        definitions
                    )
                },
                enabled = !isBusy && title.trim().isNotEmpty(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("editor_save_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Changes")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddDefDialog) {
        AddDefinitionDialog(
            initialTerm = prefillTerm,
            initialDefinition = prefillBody,
            onDismiss = { showAddDefDialog = false },
            onAdd = { def ->
                definitions = (definitions + def).take(40)
                showAddDefDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this note?") },
            text = { Text("This note and its highlighted definitions will be removed from the subject reviewer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteNote(note.id)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

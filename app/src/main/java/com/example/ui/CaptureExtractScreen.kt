package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.KeyDefinition
import com.example.data.Subject
import com.example.data.SubjectColorTag
import com.example.data.SubjectIconName
import com.example.viewmodel.ExtractionUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureExtractScreen(
    subjects: List<Subject>,
    initialSubjectId: String?,
    extractionState: ExtractionUiState,
    isBusy: Boolean,
    isSpottingDefinitions: Boolean,
    statusMessage: String?,
    onConsumeStatus: () -> Unit,
    onExtractFromPhotoUri: (Context, Uri) -> Unit,
    onSpotDefinitionsFromText: (String, (List<String>, List<KeyDefinition>) -> Unit) -> Unit,
    onCreateSubject: (String, SubjectColorTag, SubjectIconName, (String) -> Unit) -> Unit,
    onSaveNote: (
        subjectId: String,
        title: String,
        extractedText: String,
        photoTag: String,
        thumbnailBase64: String,
        highlights: List<String>,
        definitions: List<KeyDefinition>
    ) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSubjectId by remember(subjects, initialSubjectId) {
        mutableStateOf(
            initialSubjectId?.takeIf { id -> subjects.any { it.id == id } }
                ?: subjects.firstOrNull()?.id.orEmpty()
        )
    }
    var title by remember { mutableStateOf("") }
    var photoTag by remember { mutableStateOf("") }
    var extractedText by remember { mutableStateOf("") }
    var thumbnailBase64 by remember { mutableStateOf("") }
    var highlights by remember { mutableStateOf<List<String>>(emptyList()) }
    var definitions by remember { mutableStateOf<List<KeyDefinition>>(emptyList()) }

    var textModeTab by remember { mutableIntStateOf(0) } // 0 = Edit Text, 1 = Highlight On Screen
    var showCreateSubjectDialog by remember { mutableStateOf(false) }
    var showAddDefDialog by remember { mutableStateOf(false) }
    var prefillDefTerm by remember { mutableStateOf("") }
    var prefillDefBody by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusMessage)
            onConsumeStatus()
        }
    }

    LaunchedEffect(extractionState) {
        if (extractionState is ExtractionUiState.Ready) {
            title = extractionState.draft.suggestedTitle
            photoTag = extractionState.draft.photoTagSuggestion
            extractedText = extractionState.draft.extractedText
            thumbnailBase64 = extractionState.thumbnailBase64
            highlights = extractionState.draft.highlights
            definitions = extractionState.draft.definitions
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onExtractFromPhotoUri(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onExtractFromPhotoUri(context, uri)
        }
    }

    val previewBitmap = rememberDecodedThumbnail(thumbnailBase64)

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
                            .testTag("capture_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = { Text("Extract & Highlight Note") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step 1: Pick Subject
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Subject",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedButton(
                        onClick = { showCreateSubjectDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("capture_new_subject_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Subject")
                    }
                }

                if (subjects.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tap \"New Subject\" to create your first subject folder before saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { subject ->
                            FilterChip(
                                selected = selectedSubjectId == subject.id,
                                onClick = { selectedSubjectId = subject.id },
                                label = { Text(subject.name) }
                            )
                        }
                    }
                }
            }

            // Step 2: Capture or Pick Photo
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. Scan Printed Page or Handwriting",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Pick a photo from your gallery or snap a page with your camera to extract editable text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery")
                        }
                        OutlinedButton(
                            onClick = {
                                val photoFile = File(context.cacheDir, "note_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("take_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera")
                        }
                    }

                    when (extractionState) {
                        is ExtractionUiState.Loading -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = extractionState.statusText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        is ExtractionUiState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = extractionState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        else -> Unit
                    }

                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = "Captured note preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            // Step 3: Edit Text & Highlight On Screen
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "3. Edit Text & Highlight Key Definitions",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g., Cell Respiration") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_title_input")
                )
                OutlinedTextField(
                    value = photoTag,
                    onValueChange = { if (it.length <= 80) photoTag = it },
                    label = { Text("Photo Tag (optional)") },
                    placeholder = { Text("e.g., Bio Ch.4 • Page 12") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_photo_tag_input")
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = textModeTab == 0,
                        onClick = { textModeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Edit Text")
                    }
                    SegmentedButton(
                        selected = textModeTab == 1,
                        onClick = { textModeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Highlight On Screen")
                    }
                }

                if (textModeTab == 0) {
                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { if (it.length <= 25000) extractedText = it },
                        label = { Text("Editable Note Text") },
                        placeholder = { Text("Extracted text appears here, or type/paste study notes...") },
                        minLines = 6,
                        maxLines = 14,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("capture_text_input")
                    )
                } else {
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
                                prefillDefTerm = segment.substring(0, splitIdx).trim()
                                prefillDefBody = segment.substring(splitIdx + 1).trim()
                            } else {
                                prefillDefTerm = ""
                                prefillDefBody = segment
                            }
                            showAddDefDialog = true
                        }
                    )
                }
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
                    prefillDefTerm = ""
                    prefillDefBody = ""
                    showAddDefDialog = true
                },
                onRemoveDefinition = { target ->
                    definitions = definitions.filterNot { it.id == target.id && it.term == target.term }
                }
            )

            Button(
                onClick = {
                    val effectiveTitle = title.trim().ifEmpty {
                        extractedText.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(50)
                            ?: "Study Note"
                    }
                    onSaveNote(
                        selectedSubjectId,
                        effectiveTitle,
                        extractedText,
                        photoTag,
                        thumbnailBase64,
                        highlights,
                        definitions
                    )
                },
                enabled = !isBusy && selectedSubjectId.isNotBlank() &&
                    (title.isNotBlank() || extractedText.isNotBlank()),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_note_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Subject Reviewer")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCreateSubjectDialog) {
        SubjectEditorDialog(
            initialSubject = null,
            onDismiss = { showCreateSubjectDialog = false },
            onSave = { name, color, icon ->
                onCreateSubject(name, color, icon) { newId ->
                    selectedSubjectId = newId
                }
                showCreateSubjectDialog = false
            }
        )
    }

    if (showAddDefDialog) {
        AddDefinitionDialog(
            initialTerm = prefillDefTerm,
            initialDefinition = prefillDefBody,
            onDismiss = { showAddDefDialog = false },
            onAdd = { def ->
                definitions = (definitions + def).take(40)
                showAddDefDialog = false
            }
        )
    }
}

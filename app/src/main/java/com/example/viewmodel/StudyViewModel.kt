package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExtractedNoteDraft
import com.example.data.KeyDefinition
import com.example.data.NoteAiRepository
import com.example.data.NotesRepository
import com.example.data.StudyNote
import com.example.data.Subject
import com.example.data.SubjectColorTag
import com.example.data.SubjectIconName
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface ExtractionUiState {
    data object Idle : ExtractionUiState
    data class Loading(val statusText: String) : ExtractionUiState
    data class Ready(
        val draft: ExtractedNoteDraft,
        val thumbnailBase64: String
    ) : ExtractionUiState
    data class Error(val message: String) : ExtractionUiState
}

class StudyViewModel(
    private val notesRepository: NotesRepository,
    private val aiRepository: NoteAiRepository,
    private val currentUserId: String
) : ViewModel() {

    val subjectsState: StateFlow<UiState<List<Subject>>> = notesRepository.observeSubjects(currentUserId)
        .map<List<Subject>, UiState<List<Subject>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing subjects", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load subjects."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val notesState: StateFlow<UiState<List<StudyNote>>> = notesRepository.observeNotes(currentUserId)
        .map<List<StudyNote>, UiState<List<StudyNote>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing notes", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load study notes."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _extractionState = MutableStateFlow<ExtractionUiState>(ExtractionUiState.Idle)
    val extractionState: StateFlow<ExtractionUiState> = _extractionState.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isSpottingDefinitions = MutableStateFlow(false)
    val isSpottingDefinitions: StateFlow<Boolean> = _isSpottingDefinitions.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun resetExtraction() {
        _extractionState.value = ExtractionUiState.Idle
    }

    fun createSubject(
        name: String,
        colorTag: SubjectColorTag,
        iconName: SubjectIconName,
        onCreated: (String) -> Unit = {}
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 60) {
            _statusMessage.value = "Subject name must be between 1 and 60 characters."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = notesRepository.createSubject(trimmed, colorTag, iconName)
            _isBusy.value = false
            result.onSuccess { id ->
                _statusMessage.value = "Created \"$trimmed\""
                onCreated(id)
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to create subject."
            }
        }
    }

    fun updateSubject(
        subjectId: String,
        name: String,
        colorTag: SubjectColorTag,
        iconName: SubjectIconName
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 60) {
            _statusMessage.value = "Subject name must be between 1 and 60 characters."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = notesRepository.updateSubject(subjectId, trimmed, colorTag, iconName)
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Updated \"$trimmed\""
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to update subject."
            }
        }
    }

    fun deleteSubject(subjectId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = notesRepository.deleteSubjectAndNotes(subjectId)
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Subject deleted."
                onDeleted()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to delete subject."
            }
        }
    }

    fun extractNoteFromPhotoUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _extractionState.value = ExtractionUiState.Loading("Reading image...")
            val bitmap: Bitmap? = aiRepository.decodeUriToBitmap(context, uri)
            if (bitmap == null) {
                _extractionState.value = ExtractionUiState.Error("Unable to open the selected photo. Please try another image.")
                return@launch
            }
            _extractionState.value = ExtractionUiState.Loading("Extracting text & key definitions with Gemini...")
            val thumbBase64 = aiRepository.createCompactThumbnailBase64(bitmap)
            val result = aiRepository.extractFromPhoto(bitmap)
            result.onSuccess { draft ->
                _extractionState.value = ExtractionUiState.Ready(
                    draft = draft,
                    thumbnailBase64 = thumbBase64
                )
            }.onFailure { err ->
                _extractionState.value = ExtractionUiState.Error(
                    err.localizedMessage ?: "Could not extract text from this photo."
                )
            }
        }
    }

    fun spotDefinitionsFromText(
        text: String,
        onResult: (List<String>, List<KeyDefinition>) -> Unit
    ) {
        if (text.isBlank()) {
            _statusMessage.value = "Write or extract some note text first."
            return
        }
        viewModelScope.launch {
            _isSpottingDefinitions.value = true
            val result = aiRepository.spotDefinitionsFromText(text)
            _isSpottingDefinitions.value = false
            result.onSuccess { (highlights, definitions) ->
                onResult(highlights, definitions)
                _statusMessage.value = "Spotted ${definitions.size} key definition(s)!"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to spot key definitions."
            }
        }
    }

    fun saveNewNote(
        subjectId: String,
        title: String,
        extractedText: String,
        photoTag: String,
        thumbnailBase64: String,
        highlights: List<String>,
        definitions: List<KeyDefinition>,
        onSaved: (String) -> Unit
    ) {
        if (subjectId.isBlank()) {
            _statusMessage.value = "Please select a subject first."
            return
        }
        if (title.isBlank()) {
            _statusMessage.value = "Please enter a note title."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = notesRepository.createNote(
                subjectId = subjectId,
                title = title,
                extractedText = extractedText,
                photoTag = photoTag,
                thumbnailBase64 = thumbnailBase64,
                highlights = highlights,
                definitions = definitions
            )
            _isBusy.value = false
            result.onSuccess { noteId ->
                _extractionState.value = ExtractionUiState.Idle
                _statusMessage.value = "Saved to subject reviewer!"
                onSaved(noteId)
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save note."
            }
        }
    }

    fun updateExistingNote(
        noteId: String,
        subjectId: String,
        title: String,
        extractedText: String,
        photoTag: String,
        thumbnailBase64: String,
        highlights: List<String>,
        definitions: List<KeyDefinition>,
        onSaved: () -> Unit = {}
    ) {
        if (title.isBlank()) {
            _statusMessage.value = "Note title cannot be empty."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = notesRepository.updateNote(
                noteId = noteId,
                subjectId = subjectId,
                title = title,
                extractedText = extractedText,
                photoTag = photoTag,
                thumbnailBase64 = thumbnailBase64,
                highlights = highlights,
                definitions = definitions
            )
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Changes saved!"
                onSaved()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save changes."
            }
        }
    }

    fun deleteNote(noteId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = notesRepository.deleteNote(noteId)
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Note deleted."
                onDeleted()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to delete note."
            }
        }
    }

    private companion object {
        const val TAG = "StudyViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

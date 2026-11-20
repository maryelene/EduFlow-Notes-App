package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.AuthScreen
import com.example.auth.signOut
import com.example.data.NoteAiRepository
import com.example.data.NotesRepository
import com.example.data.configureAppCheck
import com.example.ui.CaptureExtractScreen
import com.example.ui.HomeScreen
import com.example.ui.NoteEditorScreen
import com.example.ui.SubjectReviewerScreen
import com.example.ui.theme.EduFlowTheme
import com.example.viewmodel.StudyViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val ROUTE_HOME = "home"
private const val ROUTE_SUBJECT = "subject/{subjectId}"
private const val ROUTE_CAPTURE = "capture?subjectId={subjectId}"
private const val ROUTE_NOTE = "note/{noteId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            EduFlowTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        key(user.uid) {
            AuthenticatedStudyApp(user = user)
        }
    }
}

@Composable
private fun AuthenticatedStudyApp(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudyViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudyViewModel(
                    notesRepository = NotesRepository(db),
                    aiRepository = NoteAiRepository(),
                    currentUserId = user.uid
                )
            }
        }
    )

    val subjectsState by viewModel.subjectsState.collectAsStateWithLifecycle()
    val notesState by viewModel.notesState.collectAsStateWithLifecycle()
    val extractionState by viewModel.extractionState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isSpotting by viewModel.isSpottingDefinitions.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()
    val notes = (notesState as? UiState.Success)?.data.orEmpty()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                userName = user.displayName?.substringBefore(" ").orEmpty(),
                subjectsState = subjectsState,
                notesState = notesState,
                statusMessage = statusMessage,
                onConsumeStatus = viewModel::consumeStatusMessage,
                onCreateSubject = { name, color, icon ->
                    viewModel.createSubject(name, color, icon)
                },
                onUpdateSubject = viewModel::updateSubject,
                onDeleteSubject = { subjectId ->
                    viewModel.deleteSubject(subjectId)
                },
                onOpenSubject = { subjectId ->
                    navController.navigate("subject/${Uri.encode(subjectId)}")
                },
                onOpenCapture = { subjectId ->
                    viewModel.resetExtraction()
                    val encoded = subjectId?.let { Uri.encode(it) }.orEmpty()
                    navController.navigate("capture?subjectId=$encoded")
                },
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }

        composable(
            route = ROUTE_SUBJECT,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
            val subject = subjects.firstOrNull { it.id == subjectId }
            val subjectNotes = notes.filter { it.subjectId == subjectId }
            SubjectReviewerScreen(
                subject = subject,
                notes = subjectNotes,
                onBack = { navController.popBackStack() },
                onOpenNote = { noteId ->
                    navController.navigate("note/${Uri.encode(noteId)}")
                },
                onScanNote = { targetSubjectId ->
                    viewModel.resetExtraction()
                    navController.navigate("capture?subjectId=${Uri.encode(targetSubjectId)}")
                }
            )
        }

        composable(
            route = ROUTE_CAPTURE,
            arguments = listOf(
                navArgument("subjectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val initialSubjectId = backStackEntry.arguments?.getString("subjectId")
                ?.takeIf { it.isNotBlank() }
            CaptureExtractScreen(
                subjects = subjects,
                initialSubjectId = initialSubjectId,
                extractionState = extractionState,
                isBusy = isBusy,
                isSpottingDefinitions = isSpotting,
                statusMessage = statusMessage,
                onConsumeStatus = viewModel::consumeStatusMessage,
                onExtractFromPhotoUri = viewModel::extractNoteFromPhotoUri,
                onSpotDefinitionsFromText = viewModel::spotDefinitionsFromText,
                onCreateSubject = viewModel::createSubject,
                onSaveNote = { subjectId, title, text, photoTag, thumb, highlights, defs ->
                    viewModel.saveNewNote(
                        subjectId = subjectId,
                        title = title,
                        extractedText = text,
                        photoTag = photoTag,
                        thumbnailBase64 = thumb,
                        highlights = highlights,
                        definitions = defs,
                        onSaved = {
                            navController.popBackStack()
                        }
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = ROUTE_NOTE,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId").orEmpty()
            val note = notes.firstOrNull { it.id == noteId }
            NoteEditorScreen(
                note = note,
                isBusy = isBusy,
                isSpottingDefinitions = isSpotting,
                statusMessage = statusMessage,
                onConsumeStatus = viewModel::consumeStatusMessage,
                onSpotDefinitionsFromText = viewModel::spotDefinitionsFromText,
                onSaveChanges = { id, subjId, title, text, tag, thumb, hl, defs ->
                    viewModel.updateExistingNote(
                        noteId = id,
                        subjectId = subjId,
                        title = title,
                        extractedText = text,
                        photoTag = tag,
                        thumbnailBase64 = thumb,
                        highlights = hl,
                        definitions = defs,
                        onSaved = { navController.popBackStack() }
                    )
                },
                onDeleteNote = { id ->
                    viewModel.deleteNote(id) {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

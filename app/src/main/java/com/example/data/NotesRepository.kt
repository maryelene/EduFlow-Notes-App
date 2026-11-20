package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NotesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeSubjects(userId: String): Flow<List<Subject>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/subjects"
        emitAll(
            db.collection("users").document(userId).collection("subjects")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Subject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeNotes(userId: String): Flow<List<StudyNote>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/notes"
        emitAll(
            db.collection("users").document(userId).collection("notes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createSubject(
        name: String,
        colorTag: SubjectColorTag,
        iconName: SubjectIconName
    ): Result<String> {
        val trimmedName = name.trim()
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/subjects"
        return try {
            val uid = requireUserId()
            require(trimmedName.length in 1..60) { "Subject name must be 1 to 60 characters." }
            val docRef = db.collection("users").document(uid).collection("subjects").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to trimmedName,
                "colorTag" to colorTag.name,
                "iconName" to iconName.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSubject(
        subjectId: String,
        name: String,
        colorTag: SubjectColorTag,
        iconName: SubjectIconName
    ): Result<Unit> {
        val trimmedName = name.trim()
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/subjects/$subjectId"
        return try {
            val uid = requireUserId()
            require(subjectId.isNotBlank()) { "Subject ID must not be blank." }
            require(trimmedName.length in 1..60) { "Subject name must be 1 to 60 characters." }
            val docRef = db.collection("users").document(uid).collection("subjects").document(subjectId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to trimmedName,
                    "colorTag" to colorTag.name,
                    "iconName" to iconName.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSubjectAndNotes(subjectId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/subjects/$subjectId"
        return try {
            val uid = requireUserId()
            require(subjectId.isNotBlank()) { "Subject ID must not be blank." }
            val notesSnapshot = db.collection("users").document(uid).collection("notes")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()

            val batch = db.batch()
            for (doc in notesSnapshot.documents) {
                batch.delete(doc.reference)
            }
            val subjRef = db.collection("users").document(uid).collection("subjects").document(subjectId)
            docPath = subjRef.path
            batch.delete(subjRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createNote(
        subjectId: String,
        title: String,
        extractedText: String,
        photoTag: String,
        thumbnailBase64: String,
        highlights: List<String>,
        definitions: List<KeyDefinition>
    ): Result<String> {
        val cleanTitle = title.trim().take(120)
        val cleanText = extractedText.take(25000)
        val cleanPhotoTag = photoTag.trim().take(80)
        val cleanThumb = thumbnailBase64.take(120000)
        val cleanHighlights = highlights.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(40)
        val cleanDefinitions = definitions
            .filter { it.term.isNotBlank() && it.definition.isNotBlank() }
            .take(40)
            .map { def ->
                def.copy(
                    id = def.id.ifBlank { java.util.UUID.randomUUID().toString().take(8) },
                    term = def.term.trim(),
                    definition = def.definition.trim(),
                    highlightColor = SubjectColorTag.fromRaw(def.highlightColor).name
                )
            }

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notes"
        return try {
            val uid = requireUserId()
            require(subjectId.isNotBlank()) { "Please pick a subject first." }
            require(cleanTitle.isNotEmpty()) { "Note title cannot be empty." }
            val docRef = db.collection("users").document(uid).collection("notes").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "subjectId" to subjectId,
                "title" to cleanTitle,
                "extractedText" to cleanText,
                "photoTag" to cleanPhotoTag,
                "thumbnailBase64" to cleanThumb,
                "highlights" to cleanHighlights,
                "definitions" to cleanDefinitions.map { it.toMap() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNote(
        noteId: String,
        subjectId: String,
        title: String,
        extractedText: String,
        photoTag: String,
        thumbnailBase64: String,
        highlights: List<String>,
        definitions: List<KeyDefinition>
    ): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        val cleanText = extractedText.take(25000)
        val cleanPhotoTag = photoTag.trim().take(80)
        val cleanThumb = thumbnailBase64.take(120000)
        val cleanHighlights = highlights.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(40)
        val cleanDefinitions = definitions
            .filter { it.term.isNotBlank() && it.definition.isNotBlank() }
            .take(40)
            .map { def ->
                def.copy(
                    id = def.id.ifBlank { java.util.UUID.randomUUID().toString().take(8) },
                    term = def.term.trim(),
                    definition = def.definition.trim(),
                    highlightColor = SubjectColorTag.fromRaw(def.highlightColor).name
                )
            }

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notes/$noteId"
        return try {
            val uid = requireUserId()
            require(noteId.isNotBlank()) { "Note ID must not be blank." }
            require(subjectId.isNotBlank()) { "Subject ID must not be blank." }
            require(cleanTitle.isNotEmpty()) { "Note title cannot be empty." }
            val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "subjectId" to subjectId,
                    "title" to cleanTitle,
                    "extractedText" to cleanText,
                    "photoTag" to cleanPhotoTag,
                    "thumbnailBase64" to cleanThumb,
                    "highlights" to cleanHighlights,
                    "definitions" to cleanDefinitions.map { it.toMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notes/$noteId"
        return try {
            val uid = requireUserId()
            require(noteId.isNotBlank()) { "Note ID must not be blank." }
            val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}

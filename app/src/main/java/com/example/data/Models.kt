package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SubjectColorTag(val label: String) {
    ROSE("Rose"),
    BERRY("Berry"),
    BLUSH("Blush"),
    CORAL("Coral"),
    PLUM("Plum"),
    MAGENTA("Magenta");

    companion object {
        fun fromRaw(raw: String?): SubjectColorTag =
            entries.firstOrNull { it.name == raw } ?: ROSE
    }
}

enum class SubjectIconName(val label: String) {
    BOOK("General"),
    SCIENCE("Science"),
    HISTORY("History"),
    MATH("Math"),
    LANGUAGE("Language"),
    ART("Arts");

    companion object {
        fun fromRaw(raw: String?): SubjectIconName =
            entries.firstOrNull { it.name == raw } ?: BOOK
    }
}

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorTag: String = SubjectColorTag.ROSE.name,
    val iconName: String = SubjectIconName.BOOK.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val colorEnum: SubjectColorTag
        get() = SubjectColorTag.fromRaw(colorTag)

    val iconEnum: SubjectIconName
        get() = SubjectIconName.fromRaw(iconName)
}

@Keep
@IgnoreExtraProperties
data class KeyDefinition(
    val id: String = "",
    val term: String = "",
    val definition: String = "",
    val highlightColor: String = SubjectColorTag.ROSE.name
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "term" to term,
        "definition" to definition,
        "highlightColor" to highlightColor
    )
}

@Keep
@IgnoreExtraProperties
data class StudyNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val title: String = "",
    val extractedText: String = "",
    val photoTag: String = "",
    val thumbnailBase64: String = "",
    val highlights: List<String> = emptyList(),
    val definitions: List<KeyDefinition> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.KeyDefinition
import com.example.data.SubjectColorTag
import java.util.UUID

@Composable
fun OnScreenHighlighterPanel(
    extractedText: String,
    highlights: List<String>,
    onToggleHighlight: (String) -> Unit,
    onQuickDefineFromSegment: (String) -> Unit
) {
    val segments = remember(extractedText) { splitTextIntoHighlightableSegments(extractedText) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tap any line below to highlight it in pink, or tap Define to turn it into a flashcard.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (segments.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add or extract text first, then tap lines here to highlight key ideas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            segments.forEachIndexed { index, segment ->
                val isHighlighted = highlights.any { it.equals(segment, ignoreCase = true) }
                val bgColor = if (isHighlighted) {
                    SubjectColorTag.ROSE.toPinkStyle().container
                } else {
                    MaterialTheme.colorScheme.surface
                }
                val textColor = if (isHighlighted) {
                    SubjectColorTag.ROSE.toPinkStyle().ink
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (isHighlighted) 1.5.dp else 1.dp,
                        color = if (isHighlighted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHighlight(segment) }
                        .testTag("highlight_segment_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Highlight,
                            contentDescription = if (isHighlighted) "Highlighted" else "Tap to highlight",
                            tint = if (isHighlighted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = segment,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = { onQuickDefineFromSegment(segment) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Define", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyDefinitionsEditorSection(
    definitions: List<KeyDefinition>,
    isSpotting: Boolean,
    onAiSpotDefinitions: () -> Unit,
    onAddDefinitionClick: () -> Unit,
    onRemoveDefinition: (KeyDefinition) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Key Definitions (${definitions.size})",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAiSpotDefinitions,
                    enabled = !isSpotting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("ai_spot_definitions_button")
                ) {
                    if (isSpotting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Spot")
                }
                Button(
                    onClick = onAddDefinitionClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("add_definition_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        if (definitions.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No key definitions yet. Tap \"Add\" or \"AI Spot\" to build your subject reviewer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            definitions.forEach { def ->
                val accent = SubjectColorTag.fromRaw(def.highlightColor).toPinkStyle()
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, accent.container),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = accent.container,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = def.term,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent.ink,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = def.definition,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { onRemoveDefinition(def) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove definition",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDefinitionDialog(
    initialTerm: String,
    initialDefinition: String,
    onDismiss: () -> Unit,
    onAdd: (KeyDefinition) -> Unit
) {
    var term by remember(initialTerm) { mutableStateOf(initialTerm) }
    var definition by remember(initialDefinition) { mutableStateOf(initialDefinition) }
    var selectedColor by remember { mutableStateOf(SubjectColorTag.ROSE) }
    val colorOptions = listOf(
        SubjectColorTag.ROSE,
        SubjectColorTag.BERRY,
        SubjectColorTag.BLUSH,
        SubjectColorTag.CORAL
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Highlight Key Definition") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("Key Term") },
                    placeholder = { Text("e.g., Photosynthesis") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("definition_term_input")
                )
                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text("Definition") },
                    placeholder = { Text("Short, clear meaning...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("definition_body_input")
                )
                Text(
                    text = "Highlighter Shade",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorOptions.forEach { shade ->
                        val style = shade.toPinkStyle()
                        FilterChip(
                            selected = selectedColor == shade,
                            onClick = { selectedColor = shade },
                            label = { Text(shade.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = style.container,
                                selectedLabelColor = style.ink
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        KeyDefinition(
                            id = UUID.randomUUID().toString().take(8),
                            term = term.trim(),
                            definition = definition.trim(),
                            highlightColor = selectedColor.name
                        )
                    )
                },
                enabled = term.trim().isNotEmpty() && definition.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_add_definition_button")
            ) {
                Text("Highlight & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

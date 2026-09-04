package com.thestudypath.pdfviewer.ui.tools.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.results.ResultActionButtons
import com.thestudypath.pdfviewer.ui.tools.ToolIds
import com.thestudypath.pdfviewer.ui.tools.toolDefinitionFor

@Composable
fun AddPasswordWorkspaceCard(
    uiState: AddPasswordUiState,
    onChooseDocument: () -> Unit,
    onUserPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onOwnerPasswordChange: (String) -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onProtectPdf: () -> Unit,
    onOpenProtectedPdf: (DocumentItem) -> Unit,
    onShareProtectedPdf: (DocumentItem) -> Unit,
    onSaveCopyProtectedPdf: (DocumentItem) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PasswordToolHeader(
                toolId = ToolIds.AddPassword,
                title = "Add Password",
                description = "Choose one PDF, set a password, and save a protected copy in app storage.",
            )

            Button(
                onClick = onChooseDocument,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.selectedDocument == null) "Choose PDF" else "Change PDF")
            }

            uiState.selectedDocument?.let { selectedDocument ->
                DocumentSummaryCard(document = selectedDocument)

                PasswordField(
                    value = uiState.userPassword,
                    onValueChange = onUserPasswordChange,
                    label = "Open password",
                    supportingText = "Users will need this password to open the protected PDF.",
                    imeAction = ImeAction.Next,
                )
                PasswordField(
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirm password",
                    supportingText = "Re-enter the same password to avoid locking yourself out.",
                    imeAction = ImeAction.Next,
                )
                PasswordField(
                    value = uiState.ownerPassword,
                    onValueChange = onOwnerPasswordChange,
                    label = "Owner password (optional)",
                    supportingText = "Leave blank to reuse the open password for owner access.",
                    imeAction = ImeAction.Done,
                )

                OutlinedTextField(
                    value = uiState.outputFileName,
                    onValueChange = onOutputFileNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Output file name") },
                    supportingText = { Text(".pdf is added automatically if needed") },
                    singleLine = true,
                )
            }

            Button(
                onClick = onProtectPdf,
                enabled = uiState.selectedDocument != null && !uiState.isApplyingPassword,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isApplyingPassword) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Protecting PDF…")
                } else {
                    Text(if (uiState.selectedDocument != null) "Protect PDF" else "Choose a PDF first")
                }
            }

            uiState.resultDocument?.let { protectedDocument ->
                PasswordToolResultSection(
                    title = "Protected PDF ready",
                    document = protectedDocument,
                    openLabel = "Open externally",
                    onOpen = { onOpenProtectedPdf(protectedDocument) },
                    onShare = { onShareProtectedPdf(protectedDocument) },
                    onSaveCopy = { onSaveCopyProtectedPdf(protectedDocument) },
                )
            }
        }
    }
}

@Composable
fun RemovePasswordWorkspaceCard(
    uiState: RemovePasswordUiState,
    onChooseDocument: () -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onRemovePassword: () -> Unit,
    onOpenUnlockedPdf: (DocumentItem) -> Unit,
    onShareUnlockedPdf: (DocumentItem) -> Unit,
    onSaveCopyUnlockedPdf: (DocumentItem) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PasswordToolHeader(
                toolId = ToolIds.RemovePassword,
                title = "Remove Password",
                description = "Choose a protected PDF, enter its current password, and save an unlocked copy.",
            )

            Button(
                onClick = onChooseDocument,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.selectedDocument == null) "Choose PDF" else "Change PDF")
            }

            uiState.selectedDocument?.let { selectedDocument ->
                DocumentSummaryCard(document = selectedDocument)
                AssistChip(
                    onClick = {},
                    label = { Text("Enter the current password to unlock this copy") },
                )

                PasswordField(
                    value = uiState.currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = "Current password",
                    supportingText = "The password is used only to decrypt the selected PDF.",
                    imeAction = ImeAction.Next,
                )

                OutlinedTextField(
                    value = uiState.outputFileName,
                    onValueChange = onOutputFileNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Output file name") },
                    supportingText = { Text(".pdf is added automatically if needed") },
                    singleLine = true,
                )
            }

            Button(
                onClick = onRemovePassword,
                enabled = uiState.selectedDocument != null && !uiState.isRemovingPassword,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isRemovingPassword) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Removing password…")
                } else {
                    Text(if (uiState.selectedDocument != null) "Unlock PDF" else "Choose a PDF first")
                }
            }

            uiState.resultDocument?.let { unlockedDocument ->
                PasswordToolResultSection(
                    title = "Unlocked PDF ready",
                    document = unlockedDocument,
                    openLabel = "Open",
                    onOpen = { onOpenUnlockedPdf(unlockedDocument) },
                    onShare = { onShareUnlockedPdf(unlockedDocument) },
                    onSaveCopy = { onSaveCopyUnlockedPdf(unlockedDocument) },
                )
            }
        }
    }
}

@Composable
private fun PasswordToolHeader(
    toolId: String,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = toolDefinitionFor(toolId)?.icon ?: return,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String,
    imeAction: ImeAction,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text(supportingText) },
        singleLine = true,
        visualTransformation = if (value.isEmpty()) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
    )
}

@Composable
private fun PasswordToolResultSection(
    title: String,
    document: DocumentItem,
    openLabel: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSaveCopy: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        DocumentSummaryCard(document = document)
        ResultActionButtons(
            onOpen = onOpen,
            onShare = onShare,
            onSaveCopy = onSaveCopy,
            openLabel = openLabel,
        )
    }
}



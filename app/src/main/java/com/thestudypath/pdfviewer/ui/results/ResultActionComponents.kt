package com.thestudypath.pdfviewer.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ResultActionButtons(
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSaveCopy: () -> Unit,
    openLabel: String = "Open",
    shareLabel: String = "Share",
    saveCopyLabel: String = "Save copy",
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Text(openLabel, modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(shareLabel, modifier = Modifier.padding(start = 8.dp))
            }
        }
        OutlinedButton(
            onClick = onSaveCopy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(saveCopyLabel)
        }
    }
}


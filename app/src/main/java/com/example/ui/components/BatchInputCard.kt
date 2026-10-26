package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FormatType
import com.example.data.network.YouTubeParser

@Composable
fun BatchInputCard(
  inputText: String,
  onInputChanged: (String) -> Unit,
  selectedFormat: FormatType,
  onFormatSelected: (FormatType) -> Unit,
  selectedQuality: String,
  onQualitySelected: (String) -> Unit,
  maxConcurrency: Int,
  onConcurrencySelected: (Int) -> Unit,
  onStartBatch: () -> Unit
) {
  val context = LocalContext.current
  var isSettingsExpanded by remember { mutableStateOf(false) }

  val detectedUrls by remember(inputText) {
    derivedStateOf {
      YouTubeParser.extractBatchUrls(inputText)
    }
  }

  val videoQualities = listOf("1080p HD", "720p HD", "480p SD", "360p")
  val audioQualities = listOf("320 kbps", "256 kbps", "192 kbps", "128 kbps")
  val currentQualities = if (selectedFormat == FormatType.VIDEO_MP4) videoQualities else audioQualities

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("batch_input_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Header with badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                  )
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AddLink,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = "Batch Video Downloader",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Paste multiple YouTube URLs (one per line)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Settings toggle button
        IconButton(
          onClick = { isSettingsExpanded = !isSettingsExpanded },
          modifier = Modifier.testTag("toggle_settings_button")
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Batch Settings",
            tint = if (isSettingsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Multi-line Text Field
      OutlinedTextField(
        value = inputText,
        onValueChange = onInputChanged,
        placeholder = {
          Text(
            "https://www.youtube.com/watch?v=...\nhttps://youtu.be/...\nhttps://www.youtube.com/shorts/...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp)
          .testTag("batch_url_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        trailingIcon = {
          if (inputText.isNotBlank()) {
            IconButton(onClick = { onInputChanged("") }) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear Input",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Bottom bar of input: Detected count & Paste button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (detectedUrls.isNotEmpty()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${detectedUrls.size} valid YouTube link${if (detectedUrls.size > 1) "s" else ""} ready",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )
          }
        } else {
          Text(
            text = "No URLs detected yet",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        OutlinedButton(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrBlank()) {
              if (inputText.isNotBlank()) {
                onInputChanged("$inputText\n$clipText")
              } else {
                onInputChanged(clipText)
              }
            }
          },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("paste_clipboard_button")
        ) {
          Icon(
            imageVector = Icons.Default.ContentPaste,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Paste", fontSize = 12.sp)
        }
      }

      // Expandable batch settings
      AnimatedVisibility(visible = isSettingsExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
        ) {
          // Format selection
          Text(
            text = "Target Format",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FormatType.entries.forEach { format ->
              FilterChip(
                selected = selectedFormat == format,
                onClick = { onFormatSelected(format) },
                label = { Text(format.displayName, fontSize = 12.sp) },
                leadingIcon = {
                  Icon(
                    imageVector = if (format == FormatType.VIDEO_MP4) Icons.Default.Videocam else Icons.Default.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("format_chip_${format.name}")
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Quality selection
          Text(
            text = "Target Quality / Bitrate",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            currentQualities.forEach { quality ->
              FilterChip(
                selected = selectedQuality == quality,
                onClick = { onQualitySelected(quality) },
                label = { Text(quality, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("quality_chip_$quality")
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Concurrency control
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Concurrent Download Limit",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(1, 2, 3, 5).forEach { limit ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      if (maxConcurrency == limit) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onConcurrencySelected(limit) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${limit}x",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (maxConcurrency == limit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Start Batch Download Button
      Button(
        onClick = onStartBatch,
        enabled = detectedUrls.isNotEmpty(),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("start_batch_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (detectedUrls.isNotEmpty()) "Download Batch (${detectedUrls.size} items)" else "Add URLs to Start Batch",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }
    }
  }
}

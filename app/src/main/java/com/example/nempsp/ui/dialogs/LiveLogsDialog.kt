package com.example.nempsp.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.GamepadState
import com.example.nempsp.model.LogEntry
import com.example.nempsp.model.LogLevel
import com.example.nempsp.repository.LogRepository

@Composable
fun LiveLogsDialog(
    currentState: GamepadState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allLogs by LogRepository.logsFlow.collectAsState()
    var selectedFilterMode by remember { mutableStateOf<ConnectionMode?>(null) }
    var showOnlyErrors by remember { mutableStateOf(false) }

    val filteredLogs = remember(allLogs, selectedFilterMode, showOnlyErrors) {
        allLogs.filter { log ->
            val matchMode = selectedFilterMode == null || log.mode == selectedFilterMode
            val matchLevel = !showOnlyErrors || log.level == LogLevel.ERROR || log.level == LogLevel.WARN
            matchMode && matchLevel
        }
    }

    val listState = rememberLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .testTag("dialog_live_logs"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F1219),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF252D3F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Console de Débogage & Logs Live",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E2638))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${filteredLogs.size} logs",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Copy button
                        IconButton(
                            onClick = {
                                val text = filteredLogs.joinToString("\n") {
                                    "[${it.formattedTime}] [${it.level.name}] [${it.tag}] ${it.message}"
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("NEMPSP Logs", text))
                                Toast.makeText(context, "Logs copiés dans le presse-papier", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copier", tint = Color(0xFFB0BEC5), modifier = Modifier.size(20.dp))
                        }

                        // Clear button
                        IconButton(
                            onClick = { LogRepository.clear() }
                        ) {
                            Icon(Icons.Default.Delete, "Effacer", tint = Color(0xFFFF7043), modifier = Modifier.size(20.dp))
                        }

                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Fermer", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Real-time packet inspector
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0C11))
                        .border(1.dp, Color(0xFF1F2433), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "État Stick: X: ${String.format(java.util.Locale.US, "%.2f", currentState.analogX)}  Y: ${String.format(java.util.Locale.US, "%.2f", currentState.analogY)}",
                            color = Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Masque: 0x${Integer.toHexString(currentState.buttonsMask).uppercase()} (${currentState.activeButtons.size} btns)",
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )

                        Text(
                            text = if (currentState.activeButtons.isEmpty()) "Aucune touche" else currentState.activeButtons.joinToString(" ") { it.symbol },
                            color = Color(0xFF81C784),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtrer:", color = Color(0xFF78909C), fontSize = 11.sp)

                    FilterChip(
                        label = "Tous",
                        selected = selectedFilterMode == null,
                        onClick = { selectedFilterMode = null }
                    )
                    FilterChip(
                        label = "WiFi",
                        selected = selectedFilterMode == ConnectionMode.WIFI_UDP,
                        onClick = { selectedFilterMode = ConnectionMode.WIFI_UDP }
                    )
                    FilterChip(
                        label = "Bluetooth",
                        selected = selectedFilterMode == ConnectionMode.BLUETOOTH,
                        onClick = { selectedFilterMode = ConnectionMode.BLUETOOTH }
                    )
                    FilterChip(
                        label = "USB",
                        selected = selectedFilterMode == ConnectionMode.USB_ADB,
                        onClick = { selectedFilterMode = ConnectionMode.USB_ADB }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    FilterChip(
                        label = "Erreurs",
                        selected = showOnlyErrors,
                        onClick = { showOnlyErrors = !showOnlyErrors },
                        accentColor = Color(0xFFFF5252)
                    )
                }

                // Logs list (terminal look)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF06080C))
                        .border(1.dp, Color(0xFF1A1F2C), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (filteredLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Aucun log à afficher pour le moment.\nUtilisez la manette ou testez une connexion pour voir les paquets.",
                                color = Color(0xFF546E7A),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredLogs, key = { it.id }) { log ->
                                LogRow(log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = Color(0xFF3DDC84)
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) accentColor.copy(alpha = 0.2f) else Color(0xFF141722))
            .border(1.dp, if (selected) accentColor else Color(0xFF232838), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (selected) accentColor else Color(0xFF90A4AE),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LogRow(log: LogEntry) {
    val levelColor = when (log.level) {
        LogLevel.INFO -> Color(0xFF64B5F6)
        LogLevel.SUCCESS -> Color(0xFF00E676)
        LogLevel.WARN -> Color(0xFFFFB74D)
        LogLevel.ERROR -> Color(0xFFFF5252)
        LogLevel.PACKET -> Color(0xFFB388FF)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = log.formattedTime,
            color = Color(0xFF546E7A),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 1.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(levelColor.copy(alpha = 0.15f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = log.tag,
                color = levelColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = log.message,
            color = if (log.level == LogLevel.ERROR) Color(0xFFFF8A80) else Color(0xFFCFD8DC),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

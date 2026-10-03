package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntity
import com.example.ui.theme.DangerColor
import com.example.ui.theme.LocalAmoaColors
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    entries: List<HistoryEntity>,
    onSelectHistoryExpr: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onClearAll: () -> Unit,
    onImportEntries: (List<HistoryEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAmoaColors.current
    val context = LocalContext.current

    var selectedModeFilter by remember { mutableStateOf("all") }
    var filterDateString by remember { mutableStateOf<String?>(null) } // "yyyy-MM-dd"
    var showClearDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    var editingNoteEntryId by remember { mutableStateOf<String?>(null) }
    var draftNoteText by remember { mutableStateOf("") }
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    fun pickSearchDate() {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                filterDateString = sdf.format(chosenCal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    // Filter entries
    val filteredEntries = remember(entries, selectedModeFilter, filterDateString) {
        entries.filter { e ->
            val modeMatch = when (selectedModeFilter) {
                "Scientific" -> e.mode == "Scientific"
                "Length" -> e.mode == "Length"
                "Time" -> e.mode == "Time"
                else -> true
            }
            val dateMatch = if (filterDateString != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(Date(e.timestamp)) == filterDateString
            } else true

            modeMatch && dateMatch
        }
    }

    // Group by Day
    val groupedEntries = remember(filteredEntries) {
        val groups = LinkedHashMap<String, MutableList<HistoryEntity>>()
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        filteredEntries.forEach { entry ->
            val entryCal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
            val groupLabel = when {
                dayKeyFormat.format(entryCal.time) == dayKeyFormat.format(today.time) -> {
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    "Today · ${sdf.format(entryCal.time)}"
                }
                dayKeyFormat.format(entryCal.time) == dayKeyFormat.format(yesterday.time) -> {
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    "Yesterday · ${sdf.format(entryCal.time)}"
                }
                else -> {
                    val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
                    sdf.format(entryCal.time)
                }
            }
            groups.getOrPut(groupLabel) { mutableListOf() }.add(entry)
        }
        groups
    }

    // Share / Export History JSON
    fun exportHistory() {
        if (entries.isEmpty()) {
            Toast.makeText(context, "No history entries to export", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val jsonArray = JSONArray()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            entries.forEach { e ->
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("ts", sdf.format(Date(e.timestamp)))
                obj.put("mode", e.mode)
                obj.put("exprRaw", e.exprRaw)
                obj.put("result", e.result)
                obj.put("note", e.note)
                jsonArray.put(obj)
            }
            val jsonStr = jsonArray.toString(2)
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, jsonStr)
                putExtra(Intent.EXTRA_TITLE, "AMOA_History_Export.json")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export AMOA History"))
        } catch (ex: Exception) {
            Toast.makeText(context, "Export error: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Parse and Import JSON
    fun importHistoryFromJson(jsonStr: String) {
        try {
            val jsonArray = JSONArray(jsonStr)
            val importedList = mutableListOf<HistoryEntity>()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "${System.currentTimeMillis()}-$i")
                val tsStr = obj.optString("ts", "")
                val ts = try {
                    if (tsStr.isNotEmpty()) sdf.parse(tsStr)?.time ?: System.currentTimeMillis()
                    else System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
                val mode = obj.optString("mode", "Scientific")
                val exprRaw = obj.optString("exprRaw", "")
                val result = obj.optString("result", "")
                val note = obj.optString("note", "")

                if (exprRaw.isNotEmpty() && result.isNotEmpty()) {
                    importedList.add(HistoryEntity(id, ts, mode, exprRaw, result, note))
                }
            }

            if (importedList.isNotEmpty()) {
                onImportEntries(importedList)
                Toast.makeText(context, "Imported ${importedList.size} calculations successfully!", Toast.LENGTH_LONG).show()
                showImportDialog = false
                importJsonText = ""
            } else {
                Toast.makeText(context, "No valid entries found in JSON", Toast.LENGTH_SHORT).show()
            }
        } catch (ex: Exception) {
            Toast.makeText(context, "Invalid JSON format: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Toolbar: Title & Buttons (Import, Export, Clear)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FULL HISTORY (1 YEAR)",
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = colors.textDim,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Import
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.keyBg)
                        .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                        .clickable { showImportDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("import_history_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Upload, contentDescription = "Import", tint = colors.teal, modifier = Modifier.size(13.dp))
                        Text("Import", fontSize = 11.sp, color = colors.text, fontWeight = FontWeight.Medium)
                    }
                }

                // Export
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.keyBg)
                        .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                        .clickable { exportHistory() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("export_history_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Download, contentDescription = "Export", tint = colors.brass, modifier = Modifier.size(13.dp))
                        Text("Export", fontSize = 11.sp, color = colors.text, fontWeight = FontWeight.Medium)
                    }
                }

                // Clear
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.keyBg)
                        .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                        .clickable { showClearDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("clear_history_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = DangerColor, modifier = Modifier.size(13.dp))
                        Text("Clear", fontSize = 11.sp, color = DangerColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Filters Row (Mode & Date search)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode filter
            ExposedDropdownMenuBox(
                expanded = modeDropdownExpanded,
                onExpandedChange = { modeDropdownExpanded = !modeDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = if (selectedModeFilter == "all") "All modes" else selectedModeFilter,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeDropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brass,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedContainerColor = colors.keyBg,
                        unfocusedContainerColor = colors.keyBg
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                ExposedDropdownMenu(
                    expanded = modeDropdownExpanded,
                    onDismissRequest = { modeDropdownExpanded = false },
                    modifier = Modifier.background(colors.panelBg)
                ) {
                    listOf("all" to "All modes", "Scientific" to "Scientific", "Length" to "Length", "Time" to "Time").forEach { (k, v) ->
                        DropdownMenuItem(
                            text = { Text(v, color = colors.text, fontSize = 12.sp) },
                            onClick = {
                                selectedModeFilter = k
                                modeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Date Search Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.keyBg)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable { pickSearchDate() }
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date Search", tint = colors.brass, modifier = Modifier.size(14.dp))
                    Text(
                        text = filterDateString ?: "Date",
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.text
                    )
                }
            }

            // Reset button if date filter active
            if (filterDateString != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.keyBg)
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                        .clickable { filterDateString = null }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Show all", fontSize = 11.sp, color = colors.teal, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subtitle Hint
        Text(
            text = "Tap any Scientific entry to load it back into the calculator.",
            fontSize = 11.sp,
            color = colors.textDim
        )

        // HISTORY TAPE LIST
        if (groupedEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.panelBg)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (filterDateString != null) "No calculations on this date.\nTry another day." else "No calculations yet.\nEverything you calculate will be saved here.",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textDim,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.paperBg)
                    .border(1.dp, colors.paperLine, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedEntries.forEach { (dayLabel, groupList) ->
                    item {
                        Text(
                            text = dayLabel,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.brass,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        )
                        HorizontalDivider(color = colors.paperLine, thickness = 1.dp)
                    }

                    items(groupList, key = { it.id }) { entry ->
                        val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(entry.timestamp))
                        val isSci = entry.mode == "Scientific"
                        val isEditingThisNote = editingNoteEntryId == entry.id

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isSci) onSelectHistoryExpr(entry.exprRaw)
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                                .testTag("history_item_${entry.id}")
                        ) {
                            // Meta Row: Time, Mode badge, Note button, Delete button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeStr,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textDim
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Mode badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.keyBg)
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = entry.mode,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.brass
                                        )
                                    }

                                    // Add/Edit Note button
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit note",
                                        tint = colors.teal,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable {
                                                editingNoteEntryId = entry.id
                                                draftNoteText = entry.note
                                            }
                                    )

                                    // Delete button
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete entry",
                                        tint = DangerColor,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable { onDeleteEntry(entry.id) }
                                    )
                                }
                            }

                            // Math Calculation line
                            Text(
                                text = "${entry.exprRaw} =",
                                fontSize = 13.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.text.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = entry.result,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.brass,
                                modifier = Modifier.padding(top = 1.dp)
                            )

                            // Existing note
                            if (entry.note.isNotEmpty() && !isEditingThisNote) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.keyBg.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "📝 ${entry.note}",
                                        fontSize = 11.5.sp,
                                        color = colors.textDim
                                    )
                                }
                            }

                            // Note editor input
                            if (isEditingThisNote) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = draftNoteText,
                                        onValueChange = { draftNoteText = it },
                                        placeholder = { Text("Add a comment…", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.brass,
                                            unfocusedBorderColor = colors.border,
                                            focusedTextColor = colors.text,
                                            unfocusedTextColor = colors.text,
                                            focusedContainerColor = colors.keyBg,
                                            unfocusedContainerColor = colors.keyBg
                                        ),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                    )
                                    Button(
                                        onClick = {
                                            onUpdateNote(entry.id, draftNoteText.trim())
                                            editingNoteEntryId = null
                                            draftNoteText = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.brass),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Save", fontSize = 11.sp, color = colors.equalsText, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = {
                                            editingNoteEntryId = null
                                            draftNoteText = ""
                                        }
                                    ) {
                                        Text("✕", color = colors.textDim)
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = colors.paperLine.copy(alpha = 0.5f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Footer
        Text(
            text = "Entries older than 365 days are removed automatically. Saved on this device.",
            fontSize = 10.5.sp,
            color = colors.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
        )
    }

    // Confirmation dialog for Clear
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("Are you sure you want to clear all calculations? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerColor)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = colors.textDim)
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import History") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste the exported JSON history content below:", fontSize = 12.sp, color = colors.textDim)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("[{\"exprRaw\":\"...\"}]", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brass,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text,
                            focusedContainerColor = colors.keyBg,
                            unfocusedContainerColor = colors.keyBg
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { importHistoryFromJson(importJsonText) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brass)
                ) {
                    Text("Import", color = colors.equalsText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = colors.textDim)
                }
            }
        )
    }
}

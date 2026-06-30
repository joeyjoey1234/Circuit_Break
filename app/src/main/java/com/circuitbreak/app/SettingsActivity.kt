package com.circuitbreak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitbreak.app.data.ActivityItem
import com.circuitbreak.app.data.ItemStore
import com.circuitbreak.app.ui.theme.*

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(
                loadDefaults = { ItemStore.loadDefaults(this) },
                loadMerged = { type ->
                    val (defPhys, defCog) = ItemStore.loadDefaults(this)
                    ItemStore.getMergedItems(this, if (type == "physical") defPhys else defCog, type)
                },
                isRemoved = { false }, // handled internally now
                onRemoveDefault = { ItemStore.addRemoved(this, it) },
                onRemoveCustom = { type, uid -> ItemStore.removeCustomById(this, type, uid) },
                onAdd = { type, item -> ItemStore.addCustom(this, type, item) },
                onEdit = { type, oldUid, newItem -> ItemStore.updateCustom(this, type, oldUid, newItem) },
                onRestoreAll = { ItemStore.restoreAllDefaults(this) },
                soundEnabled = { ItemStore.isSoundEnabled(this) },
                onSoundToggle = { ItemStore.setSoundEnabled(this, it) }
            )
        }
    }
}

@Composable
fun SettingsScreen(
    loadDefaults: () -> Pair<List<ActivityItem>, List<ActivityItem>>,
    loadMerged: (String) -> List<ActivityItem>,
    isRemoved: (String) -> Boolean,
    onRemoveDefault: (String) -> Unit,
    onRemoveCustom: (String, String) -> Unit,
    onAdd: (String, ActivityItem) -> Unit,
    onEdit: (String, String, ActivityItem) -> Unit,
    onRestoreAll: () -> Unit,
    soundEnabled: () -> Boolean,
    onSoundToggle: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("physical", "cognitive")
    val trigger = remember { mutableIntStateOf(0) }
    val displayItems = remember(selectedTab, trigger.intValue) {
        loadMerged(tabs[selectedTab])
    }

    val (defPhys, defCog) = remember { loadDefaults() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ActivityItem?>(null) }
    var confirmRemove by remember { mutableStateOf<ActivityItem?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    var newA by remember { mutableStateOf("") }
    var newB by remember { mutableStateOf("") }
    var newD by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf("") }

    val soundOn = remember { mutableStateOf(soundEnabled()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Edit Items", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sound", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = soundOn.value,
                    onCheckedChange = {
                        soundOn.value = it
                        onSoundToggle(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Accent,
                        checkedTrackColor = Accent.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Border
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Accent
        ) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = { Text(label.replaceFirstChar { it.uppercase() }, color = if (selectedTab == i) Accent else TextMuted) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayItems) { item ->
                val isCustom = !defPhys.any { it.uid() == item.uid() } && !defCog.any { it.uid() == item.uid() }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.a, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (item.b.isNotBlank()) Text(item.b, color = TextSecondary, fontSize = 12.sp)
                            Row {
                                Text("${item.cat}  ·  ${item.d}", color = TextMuted, fontSize = 11.sp)
                                if (isCustom) Text("  · custom", color = AccentGreen, fontSize = 11.sp)
                            }
                        }
                        Row {
                            if (isCustom) {
                                TextButton(onClick = {
                                    newA = item.a; newB = item.b; newD = item.d; newCat = item.cat
                                    editingItem = item
                                    showAddDialog = true
                                }) {
                                    Text("Edit", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                            TextButton(onClick = {
                                confirmRemove = item
                            }) {
                                Text("Remove", color = Color(0xFFFF4444), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { showRestoreConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore removed defaults", color = TextMuted, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                newA = ""; newB = ""; newD = ""; newCat = ""
                editingItem = null
                showAddDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("+ Add Item", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    // ── remove confirmation ──
    confirmRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            containerColor = CardBg,
            title = { Text("Remove item?", color = Color.White) },
            text = { Text("\"${item.a}\" will be removed from the list.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    val isCustom = !defPhys.any { it.uid() == item.uid() } && !defCog.any { it.uid() == item.uid() }
                    if (isCustom) {
                        onRemoveCustom(tabs[selectedTab], item.uid())
                    } else {
                        onRemoveDefault(item.uid())
                    }
                    trigger.intValue++
                    confirmRemove = null
                }) { Text("Remove", color = Color(0xFFFF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    // ── restore confirmation ──
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            containerColor = CardBg,
            title = { Text("Restore all defaults?", color = Color.White) },
            text = { Text("All previously removed default items will reappear in the spin list.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onRestoreAll()
                    trigger.intValue++
                    showRestoreConfirm = false
                }) { Text("Restore", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    // ── add / edit dialog ──
    if (showAddDialog) {
        val isEdit = editingItem != null
        AlertDialog(
            onDismissRequest = { showAddDialog = false; editingItem = null },
            containerColor = CardBg,
            title = { Text(if (isEdit) "Edit item" else "Add ${tabs[selectedTab]} item", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newA, onValueChange = { newA = it },
                        label = { Text("Activity") }, singleLine = true, colors = textFieldColors()
                    )
                    OutlinedTextField(
                        value = newB, onValueChange = { newB = it },
                        label = { Text("Resource / instruction") }, singleLine = true, colors = textFieldColors()
                    )
                    OutlinedTextField(
                        value = newD, onValueChange = { newD = it },
                        label = { Text("Time / detail") }, singleLine = true, colors = textFieldColors()
                    )
                    OutlinedTextField(
                        value = newCat, onValueChange = { newCat = it },
                        label = { Text("Category") }, singleLine = true, colors = textFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newA.isNotBlank()) {
                        val newItem = ActivityItem(newA, newB, newD, newCat, tabs[selectedTab])
                        if (isEdit) {
                            onEdit(tabs[selectedTab], editingItem!!.uid(), newItem)
                        } else {
                            onAdd(tabs[selectedTab], newItem)
                        }
                        trigger.intValue++
                        newA = ""; newB = ""; newD = ""; newCat = ""
                        showAddDialog = false; editingItem = null
                    }
                }) { Text(if (isEdit) "Save" else "Add", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; editingItem = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextMuted,
    focusedBorderColor = Accent,
    unfocusedBorderColor = Border,
    cursorColor = Accent
)

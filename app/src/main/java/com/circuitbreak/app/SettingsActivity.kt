package com.circuitbreak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
                loadItems = { type ->
                    val (defPhys, defCog) = ItemStore.loadDefaults(this)
                    ItemStore.getMergedItems(this, if (type == "physical") defPhys else defCog, type)
                },
                onRemove = { type, item ->
                    val (defPhys, defCog) = ItemStore.loadDefaults(this)
                    val defs = if (type == "physical") defPhys else defCog
                    if (defs.any { it.a == item.a }) {
                        val removed = ItemStore.getRemoved(this).toMutableSet()
                        removed.add(item.a)
                        ItemStore.setRemoved(this, removed)
                    } else {
                        ItemStore.removeCustom(this, type, item)
                    }
                },
                onAdd = { type, item ->
                    ItemStore.addCustom(this, type, item)
                }
            )
        }
    }
}

@Composable
fun SettingsScreen(
    loadItems: (String) -> List<ActivityItem>,
    onRemove: (String, ActivityItem) -> Unit,
    onAdd: (String, ActivityItem) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("physical", "cognitive")

    val items = remember(selectedTab) { loadItems(tabs[selectedTab]) }
    // Force recomposition after add/remove
    val trigger = remember { mutableIntStateOf(0) }
    val displayItems = remember(selectedTab, trigger.intValue) { loadItems(tabs[selectedTab]) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newA by remember { mutableStateOf("") }
    var newB by remember { mutableStateOf("") }
    var newD by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Text("Edit Items", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

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
                            Text("${item.cat}  ·  ${item.d}", color = TextMuted, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            onRemove(tabs[selectedTab], item)
                            trigger.intValue++
                        }) {
                            Text("Remove", color = Color(0xFFFF4444), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("+ Add Item", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardBg,
            title = { Text("Add ${tabs[selectedTab]} item", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newA, onValueChange = { newA = it },
                        label = { Text("Activity") },
                        singleLine = true,
                        colors = textFieldColors()
                    )
                    OutlinedTextField(
                        value = newB, onValueChange = { newB = it },
                        label = { Text("Resource / instruction") },
                        singleLine = true,
                        colors = textFieldColors()
                    )
                    OutlinedTextField(
                        value = newD, onValueChange = { newD = it },
                        label = { Text("Time / detail") },
                        singleLine = true,
                        colors = textFieldColors()
                    )
                    OutlinedTextField(
                        value = newCat, onValueChange = { newCat = it },
                        label = { Text("Category") },
                        singleLine = true,
                        colors = textFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newA.isNotBlank()) {
                        onAdd(tabs[selectedTab], ActivityItem(newA, newB, newD, newCat, tabs[selectedTab]))
                        trigger.intValue++
                        newA = ""; newB = ""; newD = ""; newCat = ""
                        showAddDialog = false
                    }
                }) { Text("Add", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = TextMuted) }
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

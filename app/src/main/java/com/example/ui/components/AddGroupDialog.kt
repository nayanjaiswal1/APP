package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted

@Composable
fun AddGroupDialog(
    users: List<UserEntity>,
    currentUserId: Long,
    onDismiss: () -> Unit,
    onSaveGroup: (name: String, category: String, colorHex: String, memberIds: List<Long>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Trip") }
    var selectedColor by remember { mutableStateOf("#6750A4") }
    val selectedMemberIds = remember {
        mutableStateListOf(currentUserId).apply {
            // Also pre-check other friends
            addAll(users.filter { it.id != currentUserId }.map { it.id })
        }
    }

    val categories = listOf("Trip", "Home", "Dining", "Couple", "Project", "Other")
    val colors = listOf("#6750A4", "#7D5260", "#21005D", "#4F378B", "#625B71", "#7E57C2")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_group_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = PurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create New Group",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Hawaii Trip, Roommates") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Category", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedCategory == cat) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (selectedCategory == cat) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Theme Color", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(colors) { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Select Group Members", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    users.forEach { user ->
                        val isChecked = selectedMemberIds.contains(user.id)
                        val isYou = user.id == currentUserId
                        val name = if (isYou) "You" else user.name

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isYou) {
                                        if (isChecked) selectedMemberIds.remove(user.id)
                                        else selectedMemberIds.add(user.id)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (!isYou) {
                                        if (checked) selectedMemberIds.add(user.id)
                                        else selectedMemberIds.remove(user.id)
                                    }
                                },
                                enabled = !isYou,
                                colors = CheckboxDefaults.colors(checkedColor = PurplePrimary)
                            )
                            AvatarCircle(name = name, colorHex = user.avatarColorHex, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (groupName.isNotBlank() && selectedMemberIds.isNotEmpty()) {
                            onSaveGroup(groupName, selectedCategory, selectedColor, selectedMemberIds)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    enabled = groupName.isNotBlank()
                ) {
                    Text("Create Group", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


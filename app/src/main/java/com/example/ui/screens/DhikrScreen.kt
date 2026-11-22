package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.DhikrEntity
import com.example.data.model.DhikrAlertMode
import com.example.data.model.DhikrScreenPosition
import com.example.ui.components.AddCustomDhikrDialog
import com.example.ui.components.DhikrPositionMiniPreview
import com.example.ui.components.SettingsSectionCard
import com.example.ui.viewmodel.NoorUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DhikrScreen(
    uiState: NoorUiState,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (DhikrAlertMode) -> Unit,
    onDailyCountChange: (Int) -> Unit,
    onScreenPositionChange: (DhikrScreenPosition) -> Unit,
    onActiveHoursChange: (Int, Int) -> Unit,
    onReshuffleToday: () -> Unit,
    onToggleDhikrItem: (DhikrEntity, Boolean) -> Unit,
    onAddCustomDhikr: (titleAr: String, textAr: String) -> Unit,
    onDeleteCustomDhikr: (DhikrEntity) -> Unit,
    onPreviewBannerPosition: () -> Unit,
    onTestRandomDhikrNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.settings.dhikrSettings
    val enabledCount = uiState.dhikrList.count { it.isEnabled }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            "الكل",
            "التسبيح",
            "التحميد",
            "التكبير",
            "الحوقلة",
            "التوكل",
            "الصلاة على النبي ﷺ",
            "الاستغفار",
            "التهليل",
            "مخصص من المستخدم"
        )
    }
    var selectedCategory by remember { mutableStateOf("الكل") }

    val filteredList = remember(uiState.dhikrList, selectedCategory) {
        if (selectedCategory == "الكل") uiState.dhikrList
        else uiState.dhikrList.filter { it.categoryAr == selectedCategory }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Card: Master Toggle + Quick Test
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "إشعارات ذكر الله اليومية",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "أذكار قصيرة مكتوبة تظهر تلقائياً على الشاشة في أوقات عشوائية خلال اليوم",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Switch(
                                checked = cfg.enabled,
                                onCheckedChange = onEnabledChange,
                                modifier = Modifier.testTag("dhikr_master_switch")
                            )
                        }

                        Button(
                            onClick = onTestRandomDhikrNow,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("dhikr_test_random_btn")
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تجربة ذكر عشوائي الآن")
                        }
                    }
                }
            }

            // 1. Alert Mode Selector (Text with tone / Silent text)
            item {
                SettingsSectionCard(
                    title = "نوع إشعار الذكر",
                    icon = Icons.Default.NotificationsActive
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DhikrAlertMode.entries.forEach { mode ->
                            val selected = cfg.mode == mode
                            Surface(
                                onClick = { onModeChange(mode) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = if (selected) {
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dhikr_mode_${mode.name}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = mode.titleAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = mode.descAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Daily Random Count & Active Window
            item {
                SettingsSectionCard(
                    title = "التكرار والتشغيل العشوائي في اليوم",
                    icon = Icons.Default.Shuffle
                ) {
                    Text(
                        text = "عدد مرات التذكير العشوائية في اليوم: ${cfg.dailyCount} مرة (من $enabledCount أذكار مفعّلة)",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3, 5, 10, 15, 20, 30).forEach { count ->
                            FilterChip(
                                selected = cfg.dailyCount == count,
                                onClick = { onDailyCountChange(count) },
                                label = { Text("$count مرات") },
                                modifier = Modifier.testTag("dhikr_count_chip_$count")
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ضبط عدد المرات يومياً (1 – 48)", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onDailyCountChange((cfg.dailyCount - 1).coerceAtLeast(1)) },
                                    modifier = Modifier.testTag("dhikr_count_minus")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "إنقاص التكرار")
                                }
                                Text(
                                    text = "${cfg.dailyCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { onDailyCountChange((cfg.dailyCount + 1).coerceAtMost(48)) },
                                    modifier = Modifier.testTag("dhikr_count_plus")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "زيادة التكرار")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نطاق الساعات النشطة: ${formatHourAr(cfg.activeStartHour)} – ${formatHourAr(cfg.activeEndHour)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            6 to 22,
                            7 to 22,
                            8 to 23,
                            0 to 23
                        ).forEach { (start, end) ->
                            val label = if (start == 0 && end == 23) "طوال اليوم (24 ساعة)"
                            else "من ${formatHourAr(start)} إلى ${formatHourAr(end)}"
                            FilterChip(
                                selected = cfg.activeStartHour == start && cfg.activeEndHour == end,
                                onClick = { onActiveHoursChange(start, end) },
                                label = { Text(label) }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onReshuffleToday,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("dhikr_reshuffle_btn")
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إعادة توزيع أوقات اليوم عشوائياً")
                    }
                }
            }

            // 3. Screen Display Position & Hide Control
            item {
                SettingsSectionCard(
                    title = "مكان ظهور الإشعار على الشاشة أو إخفاؤه",
                    icon = Icons.Default.Tune
                ) {
                    Text(
                        text = "تحكم بموضع ظهور بانر الذكر على الشاشة أو اختر إخفاءه تماماً:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DhikrScreenPosition.entries.forEach { pos ->
                            FilterChip(
                                selected = cfg.screenPosition == pos,
                                onClick = { onScreenPositionChange(pos) },
                                label = { Text(pos.titleAr) },
                                modifier = Modifier.testTag("dhikr_pos_${pos.name}")
                            )
                        }
                    }

                    DhikrPositionMiniPreview(
                        position = cfg.screenPosition,
                        mode = cfg.mode
                    )

                    OutlinedButton(
                        onClick = onPreviewBannerPosition,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("preview_dhikr_position_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("معاينة مكان الإشعار على الشاشة الآن")
                    }
                }
            }

            // 4. Dhikr Catalog Header & Category Filter + Add Custom Button
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مكتبة الأذكار (${filteredList.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("add_custom_dhikr_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("إضافة ذكر")
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }

            items(filteredList, key = { it.id }) { dhikr ->
                DhikrItemCard(
                    dhikr = dhikr,
                    onToggleEnabled = { onToggleDhikrItem(dhikr, it) },
                    onDeleteCustom = { onDeleteCustomDhikr(dhikr) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddDialog) {
        AddCustomDhikrDialog(
            onAdd = onAddCustomDhikr,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun DhikrItemCard(
    dhikr: DhikrEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onDeleteCustom: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (dhikr.isEnabled) 1.dp else 0.75.dp,
            color = if (dhikr.isEnabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dhikr.titleAr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dhikr.categoryAr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = dhikr.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("dhikr_enable_${dhikr.id}")
                )
            }

            Text(
                text = dhikr.textAr,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (dhikr.isCustom) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                TextButton(
                    onClick = onDeleteCustom,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dhikr_delete_${dhikr.id}")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("حذف الذكر")
                }
            }
        }
    }
}

private fun formatHourAr(hour24: Int): String {
    val h = hour24.coerceIn(0, 23)
    return when {
        h == 0 -> "12:00 ص"
        h < 12 -> "$h:00 ص"
        h == 12 -> "12:00 م"
        else -> "${h - 12}:00 م"
    }
}

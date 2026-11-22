package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.MuezzinEntity
import com.example.ui.viewmodel.NoorUiState

@Composable
fun MuezzinLibraryScreen(
    uiState: NoorUiState,
    onDownload: (MuezzinEntity) -> Unit,
    onDelete: (MuezzinEntity) -> Unit,
    onSelect: (MuezzinEntity) -> Unit,
    onTogglePreview: (MuezzinEntity) -> Unit,
    formatSize: (MuezzinEntity) -> String,
    modifier: Modifier = Modifier
) {
    val downloadedCount = uiState.muezzins.count { it.isDownloaded }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مكتبة أصوات المؤذنين",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "نزّل أصوات الحرمين والأقصى ومؤذني العالم الإسلامي لتعمل تلقائياً عند الأذان بدون إنترنت.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "$downloadedCount / ${uiState.muezzins.size} محمل",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            items(uiState.muezzins, key = { it.id }) { muezzin ->
                val isSelected = uiState.settings.selectedMuezzinId == muezzin.id
                val isPreviewing = uiState.previewingMuezzinId == muezzin.id

                MuezzinCard(
                    muezzin = muezzin,
                    isSelected = isSelected,
                    isPreviewing = isPreviewing,
                    sizeLabel = formatSize(muezzin),
                    onDownload = { onDownload(muezzin) },
                    onDelete = { onDelete(muezzin) },
                    onSelect = { onSelect(muezzin) },
                    onTogglePreview = { onTogglePreview(muezzin) }
                )
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun MuezzinCard(
    muezzin: MuezzinEntity,
    isSelected: Boolean,
    isPreviewing: Boolean,
    sizeLabel: String,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onTogglePreview: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = muezzin.nameAr,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${muezzin.mosqueAr} • ${muezzin.regionAr}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = sizeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (muezzin.isDownloading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { (muezzin.downloadProgress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "جاري التنزيل... ${muezzin.downloadProgress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!muezzin.isDownloaded) {
                    Button(
                        onClick = onDownload,
                        enabled = !muezzin.isDownloading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("muezzin_download_btn_${muezzin.id}")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (muezzin.isDownloading) "جاري التنزيل..." else "تنزيل الصوت")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onTogglePreview,
                        modifier = Modifier.testTag("muezzin_preview_btn_${muezzin.id}")
                    ) {
                        Icon(
                            imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "معاينة الصوت"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPreviewing) "إيقاف" else "استماع")
                    }

                    if (isSelected) {
                        OutlinedButton(
                            onClick = onSelect,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "المؤذن المختار",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("المؤذن الحالي")
                        }
                    } else {
                        Button(
                            onClick = onSelect,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("muezzin_select_btn_${muezzin.id}")
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("تعيين للأذان")
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("muezzin_delete_btn_${muezzin.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الصوت لتوفير المساحة",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

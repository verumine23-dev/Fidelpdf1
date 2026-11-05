package com.fidelpdf.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidelpdf.app.AppViewModel
import com.fidelpdf.app.MainTab
import com.fidelpdf.app.engine.HistoryEntry
import com.fidelpdf.app.engine.HistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun formatDate(ms: Long): String =
    SimpleDateFormat("d MMM yyyy · HH:mm", Locale.FRANCE).format(Date(ms))

// ======================================================================= PAGE D'ACCUEIL

@Composable
fun MainScreen(vm: AppViewModel) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNav(
                selected = vm.tab,
                onHistory = { vm.tab = MainTab.HISTORY },
                onScan = { vm.showScanSheet = true },
                onDoc = { vm.tab = MainTab.DOC }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            when (vm.tab) {
                MainTab.HISTORY -> HistoryTab(vm)
                MainTab.DOC -> DocumentationScreen()
            }
        }
    }
}

/** Barre de navigation : Historique | Scanner (caméra, au centre) | Documentation. */
@Composable
private fun BottomNav(selected: MainTab, onHistory: () -> Unit, onScan: () -> Unit, onDoc: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column {
            Spacer(Modifier.height(30.dp))
            Surface(Modifier.fillMaxWidth(), tonalElevation = 3.dp, shadowElevation = 12.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(
                        Icons.Rounded.History, "Historique", selected == MainTab.HISTORY,
                        onHistory, Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            "Scanner",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    NavItem(
                        Icons.Rounded.MenuBook, "Documentation", selected == MainTab.DOC,
                        onDoc, Modifier.weight(1f)
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(66.dp)
                .shadow(10.dp, CircleShape)
                .background(Brush.linearGradient(listOf(Indigo, Violet, Teal)), CircleShape)
                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                .clip(CircleShape)
                .clickable { onScan() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CameraAlt, "Scanner un document", tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .height(64.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(horizontal = 18.dp, vertical = 3.dp)
        ) {
            Icon(icon, label, tint = tint)
        }
        Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = tint)
    }
}

// ======================================================================= FENÊTRE DE SCAN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSheet(onDismiss: () -> Unit, onUpload: () -> Unit, onCamera: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Scanner un document", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(
                "Choisissez comment fournir votre document.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            SheetOption(
                Icons.Rounded.UploadFile,
                "Téléverser un document à scanner",
                "Choisir une photo ou une image dans votre galerie",
                onUpload
            )
            SheetOption(
                Icons.Rounded.CameraAlt,
                "Utiliser l'appareil photo",
                "Photographier le document maintenant",
                onCamera
            )
        }
    }
}

@Composable
private fun SheetOption(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Indigo, Violet))),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ======================================================================= HISTORIQUE

@Composable
private fun HistoryTab(vm: AppViewModel) {
    var toDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    Column(Modifier.fillMaxSize()) {
        HistoryHeader(vm.history)
        if (vm.history.isEmpty()) {
            EmptyHistory(onScan = { vm.showScanSheet = true })
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.history, key = { it.id }) { e ->
                    HistoryCard(e, onOpen = { vm.openEntry(e) }, onDelete = { toDelete = e })
                }
            }
        }
    }
    val target = toDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer ce scan ?") },
            text = { Text("« " + target.title + " » et son PDF seront supprimés définitivement de cet appareil.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteEntry(target)
                    toDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun HistoryHeader(history: List<HistoryEntry>) {
    val avg = if (history.isEmpty()) 0 else (history.map { it.score }.average() * 100).roundToInt()
    val pages = history.size
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Navy, IndigoDeep, Indigo, Violet)))
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppLogo(48.dp)
                Column(Modifier.weight(1f)) {
                    Text("Historique", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(
                        if (pages == 0) "Vos scans apparaîtront ici" else "Retrouvez, rééditez et téléchargez vos PDF",
                        fontSize = 13.sp,
                        color = Color(0xCCFFFFFF)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderStat(pages.toString(), if (pages > 1) "documents" else "document", Modifier.weight(1f))
                HeaderStat(if (pages == 0) "—" else "$avg %", "fidélité moyenne", Modifier.weight(1f))
                HeaderStat(
                    history.sumOf { it.texts }.toString(),
                    "textes réels",
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeaderStat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x26FFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color(0xCCFFFFFF))
    }
}

@Composable
private fun EmptyHistory(onScan: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Description, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Aucun scan pour l'instant", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Appuyez sur le bouton caméra au centre de la barre du bas pour scanner votre premier document.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onScan,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(52.dp)
        ) {
            Icon(Icons.Rounded.CameraAlt, null)
            Spacer(Modifier.width(10.dp))
            Text("Scanner un document", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun rememberHistoryImage(e: HistoryEntry, name: String, maxW: Int): ImageBitmap? {
    val ctx = LocalContext.current
    val state = produceState<ImageBitmap?>(initialValue = null, e.id, e.pdfBytes, name) {
        value = withContext(Dispatchers.IO) {
            HistoryStore.loadBitmap(HistoryStore.file(ctx, e.id, name), maxW)?.asImageBitmap()
        }
    }
    return state.value
}

@Composable
private fun HistoryCard(e: HistoryEntry, onOpen: () -> Unit, onDelete: () -> Unit) {
    val thumb = rememberHistoryImage(e, "thumb.jpg", 420)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .width(84.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(thumb, e.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                } else {
                    Icon(Icons.Rounded.Description, null, tint = Color(0xFFB9BBD6))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    e.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(formatDate(e.createdAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill2("Fidélité " + (e.score * 100).roundToInt() + " %")
                    Pill2(if (e.source.startsWith("Télé")) "Import" else if (e.source.startsWith("Appareil")) "Photo" else e.source)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, "Supprimer ce scan", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Pill2(text: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Text(
            text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

// ======================================================================= DÉTAIL D'UN SCAN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: AppViewModel, onBack: () -> Unit, onShare: (HistoryEntry) -> Unit) {
    val e = vm.selected
    if (e == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val pdfImg = rememberHistoryImage(e, "preview.jpg", 1200)
    val photoImg = rememberHistoryImage(e, "original.jpg", 1200)
    var mode by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour") }
                },
                actions = {
                    IconButton(onClick = { vm.reedit(e) }) { Icon(Icons.Rounded.Edit, "Rééditer") }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onShare(e) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Partager", maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = { vm.downloadEntry(e) },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.SaveAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Télécharger", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("PDF reconstruit") })
                FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Photo scannée") })
            }
            val shown = if (mode == 0) pdfImg else photoImg
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (e.height > 0) e.width.toFloat() / e.height else 0.7f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (shown != null) {
                    Image(shown, e.title, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                } else {
                    Icon(Icons.Rounded.Description, null, tint = Color(0xFFB9BBD6), modifier = Modifier.size(48.dp))
                }
            }

            FilledTonalButton(
                onClick = { vm.reedit(e) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Edit, null)
                Spacer(Modifier.width(8.dp))
                Text("Rééditer ce document", fontWeight = FontWeight.SemiBold)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Informations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    InfoRow("Titre", e.title)
                    InfoRow("Date du scan", formatDate(e.createdAt))
                    InfoRow("Source", e.source)
                    InfoRow("Fidélité visuelle", (e.score * 100).roundToInt().toString() + " %")
                    InfoRow("Textes réels", e.texts.toString())
                    InfoRow("Traits de tableau", e.rules.toString())
                    InfoRow("Aplats de couleur", e.fills.toString())
                    InfoRow("Éléments détourés", e.cutouts.toString())
                    InfoRow("Polices utilisées", if (e.fonts.isEmpty()) "—" else e.fonts.joinToString(", "))
                    InfoRow("Résolution du scan", e.width.toString() + " × " + e.height + " px")
                    InfoRow("Taille du PDF", formatSize(e.pdfBytes))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes >= 1024 * 1024) String.format(Locale.FRANCE, "%.1f Mo", bytes / 1048576.0)
    else (bytes / 1024).toString() + " Ko"

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.45f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.55f))
    }
}

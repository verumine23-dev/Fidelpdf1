package com.fidelpdf.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidelpdf.app.AppViewModel
import com.fidelpdf.app.engine.CutoutKind
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ======================================================================= BIENVENUE

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, IndigoDeep, Indigo)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x336366F1), radius = size.width * 0.6f, center = Offset(size.width * 0.95f, size.height * 0.08f))
            drawCircle(Color(0x2214B8A6), radius = size.width * 0.55f, center = Offset(0f, size.height * 0.72f))
        }
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppLogo(52.dp)
                Text("FidelPDF", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(36.dp))
            Text(
                "Votre scan,\nreconstruit en vrai PDF.",
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Texte réel, polices fidèles, tableaux vectoriels, couleurs et tampons détourés : pas une image dans un PDF.",
                color = Color(0xCCFFFFFF),
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(28.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Feature(Icons.Rounded.TextFields, "Texte et polices", "Arial, Times, Calibri, Cambria, Courier")
                    Feature(Icons.Rounded.GridOn, "Tableaux et traits", "Tracés vectoriels nets, épaisseurs exactes")
                    Feature(Icons.Rounded.Palette, "Couleurs fidèles", "Aplats, textes et bordures colorés")
                    Feature(Icons.Rounded.ContentCut, "Tampons et signatures", "Fond supprimé, recollés à leur place")
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Indigo)
            ) {
                Text("Continuer", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.size(10.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Traitement 100 % sur votre appareil. Aucun document n'est envoyé.",
                color = Color(0x99FFFFFF),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, sub: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x26FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White)
        }
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(sub, color = Color(0xB3FFFFFF), fontSize = 12.sp)
        }
    }
}

// ======================================================================= TRAITEMENT

private val steps = listOf(
    "Nettoyage et redressement",
    "Lecture du texte (OCR)",
    "Tableaux, traits et aplats",
    "Polices, tailles et couleurs",
    "Construction du PDF vectoriel"
)

@Composable
fun WorkingScreen(stage: Int, label: String, image: Bitmap?) {
    val transition = rememberInfiniteTransition(label = "work")
    val beam by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beam"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    val progress by animateFloatAsState(
        targetValue = (stage + 0.6f) / steps.size,
        animationSpec = tween(600),
        label = "progress"
    )
    val img = remember(image) { image?.asImageBitmap() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, IndigoDeep)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Reconstruction en cours", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Étape " + min(stage + 1, steps.size) + " sur " + steps.size + (if (label.isNotEmpty()) " · $label" else ""),
                color = Color(0xB3FFFFFF),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(14.dp))

            // Le document analysé, balayé par un faisceau de scan
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x1FFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (img != null) {
                    Image(
                        img,
                        null,
                        Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit,
                        alpha = 0.92f
                    )
                }
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height * beam
                    drawRect(Color(0x1A14B8A6), Offset.Zero, Size(size.width, y))
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x6614B8A6), Color.Transparent),
                            startY = y - 70f,
                            endY = y + 70f
                        ),
                        Offset(0f, y - 70f),
                        Size(size.width, 140f)
                    )
                    drawLine(Color(0xFF5EEAD4), Offset(0f, y), Offset(size.width, y), strokeWidth = 5f)
                    // Coins de visée
                    val len = 46f
                    val m = 22f
                    val sw = 6f
                    val c = Color.White
                    val w = size.width
                    val h = size.height
                    drawLine(c, Offset(m, m), Offset(m + len, m), sw, StrokeCap.Round)
                    drawLine(c, Offset(m, m), Offset(m, m + len), sw, StrokeCap.Round)
                    drawLine(c, Offset(w - m, m), Offset(w - m - len, m), sw, StrokeCap.Round)
                    drawLine(c, Offset(w - m, m), Offset(w - m, m + len), sw, StrokeCap.Round)
                    drawLine(c, Offset(m, h - m), Offset(m + len, h - m), sw, StrokeCap.Round)
                    drawLine(c, Offset(m, h - m), Offset(m, h - m - len), sw, StrokeCap.Round)
                    drawLine(c, Offset(w - m, h - m), Offset(w - m - len, h - m), sw, StrokeCap.Round)
                    drawLine(c, Offset(w - m, h - m), Offset(w - m, h - m - len), sw, StrokeCap.Round)
                }
            }

            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = Teal,
                trackColor = Color(0x33FFFFFF)
            )
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .graphicsLayer { alpha = if (i == stage) pulse else 1f }
                                .clip(CircleShape)
                                .background(
                                    when {
                                        i < stage -> Teal
                                        i == stage -> Color.White
                                        else -> Color(0x26FFFFFF)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (i < stage) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            s,
                            color = if (i <= stage) Color.White else Color(0x80FFFFFF),
                            fontSize = 14.sp,
                            fontWeight = if (i == stage) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ======================================================================= RÉSULTAT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showScore by rememberSaveable { mutableStateOf(true) }
    val drafts = remember { mutableStateMapOf<Int, String>() }
    val dirty = vm.baseTexts.filter { t ->
        val d = drafts[t.id]
        d != null && d != (vm.edits[t.id] ?: t.text)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Résultat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour") }
                },
                actions = {
                    IconButton(onClick = { showScore = !showScore }) {
                        Icon(
                            if (showScore) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (showScore) "Masquer le score" else "Afficher le score"
                        )
                    }
                    IconButton(onClick = { vm.showRescan = true }) {
                        Icon(Icons.Rounded.Refresh, "Refaire le scan")
                    }
                }
            )
        },
        bottomBar = {
            if (tab == 2) {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, modifier = Modifier.imePadding()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { drafts.clear() },
                            enabled = dirty.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Annuler") }
                        Button(
                            onClick = {
                                vm.applyEdits(dirty.associate { it.id to (drafts[it.id] ?: it.text) })
                                drafts.clear()
                            },
                            enabled = dirty.isNotEmpty(),
                            modifier = Modifier
                                .weight(2f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Check, null)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                if (dirty.isEmpty()) "Enregistrer les textes"
                                else "Enregistrer (" + dirty.size + ")",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShare,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Rounded.Share, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Partager", maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Rounded.SaveAlt, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Enregistrer le PDF", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    ) { padding ->
        val model = vm.model
        if (model == null || vm.preview == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.padding(padding)) {
                AnimatedVisibility(visible = showScore) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { vm.score },
                                    modifier = Modifier.size(64.dp),
                                    strokeWidth = 7.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                                Text(
                                    (vm.score * 100).roundToInt().toString() + "%",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Fidélité visuelle estimée",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    model.texts.size.toString() + " textes · " + model.rules.size + " traits · " +
                                        model.fills.size + " aplats · " + model.cutouts.size + " éléments",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(onClick = { showScore = false }) {
                                Icon(Icons.Rounded.VisibilityOff, "Masquer", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                TabRow(selectedTabIndex = tab) {
                    listOf("Aperçu", "Éléments", "Texte").forEachIndexed { i, t ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> PreviewTab(vm)
                        1 -> ElementsTab(vm)
                        else -> TextTab(vm, drafts)
                    }
                }
            }
        }
    }
}

private class FractionShape(val fraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, 0f, size.width * fraction, size.height))
}

@Composable
private fun Pill(text: String, modifier: Modifier) {
    Surface(color = Color(0xCC000000), shape = RoundedCornerShape(50), modifier = modifier.padding(10.dp)) {
        Text(
            text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PreviewTab(vm: AppViewModel) {
    val orig = vm.original ?: return
    val prev = vm.preview ?: return
    val heat = vm.heat
    val origImg = remember(orig) { orig.asImageBitmap() }
    val prevImg = remember(prev) { prev.asImageBitmap() }
    val heatImg = remember(heat) { heat?.asImageBitmap() }
    var mode by rememberSaveable { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("PDF", "Scan", "Comparer", "Écarts").forEachIndexed { i, l ->
                FilterChip(selected = mode == i, onClick = { mode = i }, label = { Text(l) })
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
        ) {
            when (mode) {
                0 -> ZoomImage(listOf(prevImg), Modifier.fillMaxSize())
                1 -> ZoomImage(listOf(origImg), Modifier.fillMaxSize())
                2 -> CompareSlider(origImg, prevImg, orig.width.toFloat() / orig.height)
                else -> ZoomImage(
                    if (heatImg != null) listOf(origImg, heatImg) else listOf(origImg),
                    Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CompareSlider(origImg: androidx.compose.ui.graphics.ImageBitmap, prevImg: androidx.compose.ui.graphics.ImageBitmap, aspect: Float) {
    var frac by remember { mutableFloatStateOf(0.5f) }
    var boxW by remember { mutableIntStateOf(1) }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val wDp = if (maxWidth / maxHeight < aspect) maxWidth else maxHeight * aspect
        val hDp = wDp / aspect
        Box(
            Modifier
                .width(wDp)
                .height(hDp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .onSizeChanged { boxW = max(1, it.width) }
                .pointerInput(Unit) {
                    detectTapGestures { o -> frac = (o.x / size.width).coerceIn(0f, 1f) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        frac = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
        ) {
            Image(origImg, "Scan", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, filterQuality = FilterQuality.High)
            Image(
                prevImg, "PDF",
                Modifier
                    .fillMaxSize()
                    .clip(FractionShape(frac)),
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.High
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .offset { IntOffset((boxW * frac).roundToInt() - 4, 0) }
                    .background(Color.White)
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((boxW * frac).roundToInt() - 18.dp.roundToPx(), 0) }
                    .size(36.dp)
                    .shadow(6.dp, CircleShape)
                    .background(Indigo, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SwapHoriz, null, tint = Color.White)
            }
            Pill("PDF", Modifier.align(Alignment.TopStart))
            Pill("SCAN", Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun ElementsTab(vm: AppViewModel) {
    val orig = vm.original ?: return
    val origImg = remember(orig) { orig.asImageBitmap() }
    var kind by remember { mutableStateOf(CutoutKind.STAMP) }
    var sel by remember { mutableStateOf<Rect?>(null) }
    var boxSize by remember { mutableStateOf(IntSize(1, 1)) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Tracez un cadre autour d'un tampon, d'une signature, d'un logo ou d'une photo : le fond est supprimé et l'élément est recollé exactement à sa place.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CutoutKind.values().forEach { k ->
                FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
            }
        }
        if (vm.suggestions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.suggestions.forEachIndexed { i, r ->
                    AssistChip(
                        onClick = {
                            val k = boxSize.width.toFloat() / orig.width
                            sel = Rect(r.left * k, r.top * k, r.right * k, r.bottom * k)
                            kind = CutoutKind.STAMP
                        },
                        label = { Text("Tampon possible " + (i + 1)) },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, tint = Amber) }
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(orig.width.toFloat() / orig.height)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .onSizeChanged { boxSize = IntSize(max(1, it.width), max(1, it.height)) }
                .pointerInput(Unit) {
                    var start = Offset.Zero
                    detectDragGestures(
                        onDragStart = { o ->
                            start = o
                            sel = Rect(o, o)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val px = change.position.x.coerceIn(0f, size.width.toFloat())
                            val py = change.position.y.coerceIn(0f, size.height.toFloat())
                            sel = Rect(min(start.x, px), min(start.y, py), max(start.x, px), max(start.y, py))
                        }
                    )
                }
        ) {
            Image(origImg, "Scan", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Canvas(Modifier.fillMaxSize()) {
                val k = size.width / orig.width
                for (r in vm.suggestions) {
                    drawRect(
                        color = Amber,
                        topLeft = Offset(r.left * k, r.top * k),
                        size = Size(r.width() * k, r.height() * k),
                        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                    )
                }
                val s = sel
                if (s != null) {
                    drawRect(Color(0x334F46E5), Offset(s.left, s.top), Size(s.width, s.height))
                    drawRect(Indigo, Offset(s.left, s.top), Size(s.width, s.height), style = Stroke(width = 4f))
                }
            }
        }
        val s = sel
        Button(
            onClick = {
                if (s != null) {
                    val k = orig.width.toFloat() / boxSize.width
                    vm.addCutout(android.graphics.RectF(s.left * k, s.top * k, s.right * k, s.bottom * k), kind)
                    sel = null
                }
            },
            enabled = s != null && s.width > 12f && s.height > 12f,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.ContentCut, null)
            Spacer(Modifier.size(8.dp))
            Text("Détourer et coller (" + kind.label.lowercase() + ")", fontWeight = FontWeight.SemiBold)
        }

        if (vm.cutoutItems.isNotEmpty()) {
            Text("Éléments recollés", fontWeight = FontWeight.Bold)
            vm.cutoutItems.forEach { c ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFD9DBE8)),
                            contentAlignment = Alignment.Center
                        ) {
                            val img = remember(c) { c.bitmap.asImageBitmap() }
                            Image(img, c.kind.label, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(c.kind.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                c.bitmap.width.toString() + " × " + c.bitmap.height + " px · fond transparent",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { vm.removeCutout(c.id) }) {
                            Icon(Icons.Rounded.Delete, "Supprimer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextTab(vm: AppViewModel, drafts: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, String>) {
    var focusedId by remember { mutableStateOf<Int?>(null) }
    val prev = vm.preview
    val model = vm.model
    val prevImg = remember(prev) { prev?.asImageBitmap() }

    Column(Modifier.fillMaxSize()) {
        // Aperçu agrandi autour du texte en cours de correction (le PDF n'est jamais assombri)
        if (prevImg != null && prev != null && model != null) {
            val focus = vm.baseTexts.firstOrNull { it.id == focusedId }
            androidx.compose.foundation.Canvas(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
            ) {
                val pk = prev.width / model.width.toFloat()
                val srcW = max(1, (prev.width / 2.2f).toInt())
                val srcH = max(1, min(prev.height, (srcW * size.height / size.width).toInt()))
                val cx = (focus?.box?.centerX() ?: (model.width / 2f)) * pk
                val cy = (focus?.box?.centerY() ?: 0f) * pk
                val sx = (cx - srcW / 2f).coerceIn(0f, max(0f, (prev.width - srcW).toFloat())).toInt()
                val sy = (cy - srcH / 2f).coerceIn(0f, max(0f, (prev.height - srcH).toFloat())).toInt()
                drawImage(
                    image = prevImg,
                    srcOffset = IntOffset(sx, sy),
                    srcSize = IntSize(srcW, srcH),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.High
                )
                if (focus != null) {
                    val kx = size.width / srcW
                    val ky = size.height / srcH
                    val b = focus.box
                    drawRect(
                        color = Amber,
                        topLeft = Offset((b.left * pk - sx) * kx - 3f, (b.top * pk - sy) * ky - 3f),
                        size = Size(b.width() * pk * kx + 6f, b.height() * pk * ky + 6f),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Corrigez les textes mal lus, puis appuyez sur « Enregistrer » pour mettre le PDF à jour. Sous chaque texte, la police retrouvée.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(vm.baseTexts, key = { it.id }) { t ->
                OutlinedTextField(
                    value = drafts[t.id] ?: vm.edits[t.id] ?: t.text,
                    onValueChange = { drafts[t.id] = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) focusedId = t.id },
                    label = { Text(t.font.label, fontSize = 11.sp) },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

package com.fidelpdf.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.fidelpdf.app.ui.CropScreen
import com.fidelpdf.app.ui.DetailScreen
import com.fidelpdf.app.ui.FidelTheme
import com.fidelpdf.app.ui.MainScreen
import com.fidelpdf.app.ui.ResultScreen
import com.fidelpdf.app.ui.ScanSheet
import com.fidelpdf.app.ui.WelcomeScreen
import com.fidelpdf.app.ui.WorkingScreen
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FidelTheme {
                App(vm)
            }
        }
    }
}

private fun newCameraUri(ctx: Context): Uri {
    val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
    val f = File(dir, "capture_" + System.currentTimeMillis() + ".jpg")
    return FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
}

private fun openPdf(ctx: Context, uri: Uri) {
    try {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(view)
    } catch (_: Throwable) {
        // aucune application PDF installée : le fichier reste dans Téléchargements
    }
}

private fun sharePdf(ctx: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(send, "Partager le PDF"))
}

@Composable
fun App(vm: AppViewModel) {
    val ctx = LocalContext.current
    var pendingCamera by rememberSaveable { mutableStateOf<Uri?>(null) }
    val snackbar = remember { SnackbarHostState() }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = pendingCamera
        if (ok && u != null) vm.prepareCrop(u, true, vm.rescanning || vm.showRescanPending)
        else vm.showRescanPending = false
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.prepareCrop(uri, false, vm.rescanning || vm.showRescanPending)
        else vm.showRescanPending = false
    }
    val legacySave = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> vm.finishLegacySave(uri) }

    // Android 7 à 9 : le ViewModel demande le sélecteur de fichier
    LaunchedEffect(vm.pendingPicker) {
        val name = vm.pendingPicker
        if (name != null) legacySave.launch(name)
    }

    // Messages du bas (avec bouton « Ouvrir » après un enregistrement)
    LaunchedEffect(vm.message) {
        val msg = vm.message
        if (msg != null) {
            val uri = vm.openUri
            val r = snackbar.showSnackbar(
                message = msg,
                actionLabel = if (uri != null) "Ouvrir" else null,
                withDismissAction = true
            )
            if (r == SnackbarResult.ActionPerformed && uri != null) openPdf(ctx, uri)
            vm.message = null
            vm.openUri = null
        }
    }

    BackHandler(enabled = vm.screen == Screen.WORKING) { /* traitement en cours */ }
    BackHandler(enabled = vm.screen == Screen.CROP) { vm.cancelCrop() }
    BackHandler(enabled = vm.screen == Screen.RESULT || vm.screen == Screen.DETAIL) {
        vm.screen = Screen.MAIN
    }
    BackHandler(enabled = vm.screen == Screen.MAIN && vm.tab == MainTab.DOC) {
        vm.tab = MainTab.HISTORY
    }

    Box(Modifier.fillMaxSize()) {
        when (vm.screen) {
            Screen.WELCOME -> WelcomeScreen(onContinue = { vm.screen = Screen.MAIN })

            Screen.MAIN -> {
                MainScreen(vm)
                if (vm.showScanSheet) {
                    ScanSheet(
                        onDismiss = { vm.showScanSheet = false },
                        onUpload = {
                            vm.showScanSheet = false
                            gallery.launch("image/*")
                        },
                        onCamera = {
                            vm.showScanSheet = false
                            val u = newCameraUri(ctx)
                            pendingCamera = u
                            camera.launch(u)
                        }
                    )
                }
            }

            Screen.CROP -> CropScreen(vm)

            Screen.WORKING -> WorkingScreen(vm.stageIndex, vm.stageLabel, vm.workingBitmap)

            Screen.RESULT -> ResultScreen(
                vm = vm,
                onBack = { vm.screen = Screen.MAIN },
                onSave = { vm.downloadCurrent() },
                onShare = { vm.shareCurrent { uri -> sharePdf(ctx, uri) } }
            )

            Screen.DETAIL -> DetailScreen(
                vm = vm,
                onBack = { vm.screen = Screen.MAIN },
                onShare = { e ->
                    val uri = vm.shareEntry(e)
                    if (uri != null) sharePdf(ctx, uri)
                }
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp, start = 12.dp, end = 12.dp)
        ) { data -> Snackbar(snackbarData = data) }

        if (vm.showRescan) {
            AlertDialog(
                onDismissRequest = { vm.showRescan = false },
                title = { Text("Refaire le scan") },
                text = { Text("Choisissez comment améliorer le résultat. Le document actuel de l'historique sera remplacé.") },
                confirmButton = {
                    TextButton(onClick = { vm.rescanCrop() }) { Text("Recadrer la photo") }
                },
                dismissButton = {
                    Column {
                        TextButton(onClick = {
                            vm.showRescan = false
                            vm.showRescanPending = true
                            val u = newCameraUri(ctx)
                            pendingCamera = u
                            camera.launch(u)
                        }) { Text("Nouvelle photo") }
                        TextButton(onClick = {
                            vm.showRescan = false
                            vm.showRescanPending = true
                            gallery.launch("image/*")
                        }) { Text("Autre fichier") }
                        TextButton(onClick = { vm.showRescan = false }) { Text("Annuler") }
                    }
                }
            )
        }

        vm.error?.let { err ->
            AlertDialog(
                onDismissRequest = { vm.error = null },
                confirmButton = { TextButton(onClick = { vm.error = null }) { Text("OK") } },
                title = { Text("Oups") },
                text = { Text(err) }
            )
        }
    }
}

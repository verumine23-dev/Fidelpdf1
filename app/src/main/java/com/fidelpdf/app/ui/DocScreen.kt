package com.fidelpdf.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ======================================================================= PAGE DOCUMENTATION

@Composable
fun DocumentationScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppLogo(44.dp)
            Column {
                Text("Documentation", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Tout savoir sur FidelPDF",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- À quoi sert l'application
        DocCard {
            SectionTitle("À quoi sert FidelPDF ?")
            Body(
                "FidelPDF transforme la photo ou le scan d'un document en un vrai PDF, identique à l'original. " +
                    "Ce n'est pas une image placée dans un PDF : tout est reconstruit."
            )
            Bullet("Le texte est du vrai texte : net à tous les zooms, sélectionnable et copiable.")
            Bullet("Les polices sont retrouvées (Arial, Times, Calibri, Cambria, Courier) avec leur taille, leur gras et leur italique.")
            Bullet("Chaque texte est replacé exactement à sa position, aligné sur sa ligne et sur sa colonne.")
            Bullet("Les tableaux sont retracés en traits vectoriels : colonnes, lignes, épaisseurs et couleurs.")
            Bullet("Les tampons, signatures, logos et photos sont découpés, leur fond est supprimé, puis ils sont recollés à leur place.")
            Body("Résultat : un document propre et professionnel, prêt à être envoyé ou imprimé.")
        }

        // --- Les boutons
        DocCard {
            SectionTitle("Les boutons de l'application")
            ButtonRow(Icons.Rounded.TouchApp, "Continuer", "Sur la page de bienvenue, ouvre la page d'accueil de l'application.")
            ButtonRow(Icons.Rounded.History, "Historique (en bas à gauche)", "Affiche tous vos scans : aperçu, titre et corbeille. Un appui ouvre le détail.")
            ButtonRow(Icons.Rounded.CameraAlt, "Scanner (appareil photo, au centre)", "Ouvre la fenêtre de choix pour démarrer un nouveau scan.")
            ButtonRow(Icons.Rounded.MenuBook, "Documentation (en bas à droite)", "Cette page : l'utilité de l'application et la marche à suivre.")
            ButtonRow(Icons.Rounded.UploadFile, "Téléverser un document à scanner", "Choisit une photo ou une image déjà enregistrée dans votre téléphone.")
            ButtonRow(Icons.Rounded.CameraAlt, "Utiliser l'appareil photo", "Photographie le document sur place.")
            ButtonRow(Icons.Rounded.Crop, "Recadrer", "Après la photo ou l'import, placez les 4 coins sur ceux de la feuille : elle est redressée (perspective corrigée). Bouton Détecter pour un placement automatique, pivot possible.")
            ButtonRow(Icons.Rounded.Refresh, "Refaire le scan (en haut du résultat)", "Recadrer à nouveau, reprendre une photo ou choisir un autre fichier pour un meilleur résultat.")
            ButtonRow(Icons.Rounded.Visibility, "Œil (en haut du résultat)", "Masque ou affiche la carte de fidélité pour gagner de la place.")
            ButtonRow(Icons.Rounded.CompareArrows, "PDF / Scan / Comparer / Écarts", "PDF et Scan s'affichent dans une visionneuse : pincez pour zoomer, double appui pour agrandir. Comparer : curseur. Écarts : différences en rouge.")
            ButtonRow(Icons.Rounded.Edit, "Rééditer (détail d'un scan)", "Rouvre le document sur la page Résultat avec vos corrections pour continuer à le modifier.")
            ButtonRow(Icons.Rounded.CompareArrows, "Comparer", "Glissez le curseur pour voir le scan et le PDF reconstruit. « Écarts » surligne les différences en rouge.")
            ButtonRow(Icons.Rounded.ContentCut, "Éléments", "Tracez un cadre autour d'un tampon, d'une signature, d'un logo ou d'une photo puis « Détourer et coller ».")
            ButtonRow(Icons.Rounded.TextFields, "Texte", "Corrigez une lecture erronée, puis appuyez sur Enregistrer : le PDF est mis à jour. L'aperçu agrandi montre où se trouve le texte.")
            ButtonRow(Icons.Rounded.Share, "Partager", "Envoie le PDF par WhatsApp, e-mail ou toute autre application.")
            ButtonRow(Icons.Rounded.SaveAlt, "Enregistrer le PDF / Télécharger", "Enregistre le PDF dans le dossier Téléchargements, sous « FidelPDF ».")
            ButtonRow(Icons.Rounded.Delete, "Corbeille", "Supprime un scan de l'historique, après confirmation.")
        }

        // --- Le processus
        Text("Du scan au PDF identique, pas à pas", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Body(
            "Les écrans ci-dessous sont des illustrations fidèles de l'application. " +
                "Les pastilles jaunes numérotées correspondent aux explications sous chaque écran."
        )

        StepCard(
            1, "Ouvrir l'application et appuyer sur « Continuer »",
            "Au lancement, la page de bienvenue présente l'application. Un seul bouton : Continuer.",
            listOf("Bouton « Continuer » : ouvre la page d'accueil.")
        ) { s -> MockWelcome(s) }

        StepCard(
            2, "Découvrir la page d'accueil et ses 3 boutons",
            "La barre du bas contient les trois boutons de navigation. Le bouton rond au centre, avec l'icône d'appareil photo, sert à scanner.",
            listOf(
                "Historique : retrouvez vos scans précédents.",
                "Scanner (icône d'appareil photo) : démarre un nouveau scan.",
                "Documentation : cette page d'aide."
            )
        ) { s -> MockHome(s) }

        StepCard(
            3, "Choisir : téléverser ou prendre une photo",
            "Un appui sur le bouton caméra ouvre une fenêtre avec deux options.",
            listOf(
                "Téléverser un document à scanner : choisissez une image de votre galerie.",
                "Utiliser l'appareil photo : photographiez le document à plat, bien éclairé et bien cadré."
            )
        ) { s -> MockSheet(s) }

        StepCard(
            4, "Patienter pendant la reconstruction",
            "L'application analyse votre document : un faisceau le balaie pendant qu'elle redresse l'image, lit le texte, détecte les tableaux, retrouve les polices et construit le PDF.",
            listOf(
                "Votre document, balayé par le faisceau de scan.",
                "Barre de progression globale.",
                "Les 5 étapes : la pastille clignote sur l'étape en cours."
            )
        ) { s -> MockWorking(s) }

        StepCard(
            5, "Vérifier le résultat",
            "Après chaque scan, vous arrivez toujours sur la page Résultat. Vérifiez et corrigez si besoin.",
            listOf(
                "Fidélité visuelle estimée : plus elle est haute, plus le PDF ressemble au scan.",
                "Onglets : Comparer, Éléments (tampons, signatures…) et Texte (corrections).",
                "Curseur : glissez-le pour comparer le scan et le PDF."
            )
        ) { s -> MockResult(s, false) }

        StepCard(
            6, "Enregistrer ou partager le PDF",
            "Les deux boutons du bas exportent le document. Une fois enregistré, un message vous permet de l'ouvrir directement.",
            listOf(
                "Partager : envoyez le PDF à un contact.",
                "Enregistrer le PDF : copie dans Téléchargements/FidelPDF.",
                "Message de confirmation avec le bouton « Ouvrir »."
            )
        ) { s -> MockResult(s, true) }

        StepCard(
            7, "Retrouver et gérer vos scans dans l'historique",
            "Chaque scan est conservé automatiquement. Le titre est tiré du document ; à défaut il s'appelle « Scan 1 », « Scan 2 »… Un appui ouvre le détail : aperçu du PDF, photo scannée, informations et bouton Télécharger.",
            listOf(
                "Aperçu du document scanné.",
                "Titre tiré du document, avec la date du scan.",
                "Corbeille : supprime ce scan (après confirmation)."
            )
        ) { s -> MockHistory(s) }

        DocCard {
            SectionTitle("Conseils pour un résultat plus fidèle")
            Bullet("Posez le document à plat, sur un fond uni, avec une lumière uniforme (sans ombre de la main).")
            Bullet("Cadrez la page entière, en évitant les reflets.")
            Bullet("Restez bien en face du document : un léger angle est corrigé, un angle fort dégrade le résultat.")
            Bullet("Après le scan, comparez avec le curseur et corrigez les textes mal lus dans l'onglet « Texte ».")
            Bullet("Pour un tampon ou une signature : resserrez le cadre autour de l'élément.")
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ----------------------------------------------------------------------- Blocs de texte

@Composable
private fun DocCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Body(text: String) {
    Text(text, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(Teal)
        )
        Text(text, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun ButtonRow(icon: ImageVector, title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Indigo, Violet))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StepCard(
    n: Int,
    title: String,
    text: String,
    legend: List<String>,
    mock: @Composable BoxScope.(MockScope) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Indigo, Violet))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(n.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            }
            Text(text, fontSize = 14.sp, lineHeight = 20.sp)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Phone(mock)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                legend.forEachIndexed { i, l ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Amber),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((i + 1).toString(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1300))
                        }
                        Text(l, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------- Écrans illustrés

private class MockScope(val w: Dp, val h: Dp)

private val MockBg = Color(0xFFF6F7FB)
private val MockInk = Color(0xFF14152B)
private val MockMuted = Color(0xFFB9BBD6)

@Composable
private fun Phone(content: @Composable BoxScope.(MockScope) -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    BoxWithConstraints(
        Modifier
            .width(210.dp)
            .height(420.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .border(BorderStroke(3.dp, Color(0xFF2B2E4F)), shape)
            .background(MockBg)
    ) {
        val s = MockScope(maxWidth, maxHeight)
        this.content(s)
    }
}

/** Pastille jaune numérotée posée sur l'écran (x, y en fraction de l'écran). */
@Composable
private fun Marker(s: MockScope, n: Int, fx: Float, fy: Float) {
    Box(
        Modifier
            .offset(x = s.w * fx - 11.dp, y = s.h * fy - 11.dp)
            .size(22.dp)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(Amber),
        contentAlignment = Alignment.Center
    ) {
        Text(n.toString(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1300))
    }
}

@Composable
private fun Line(widthFraction: Float, color: Color = MockMuted, h: Dp = 6.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(h)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
private fun BoxScope.MockNav() {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(70.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(46.dp)
                .background(Color.White)
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.History, null, tint = Indigo, modifier = Modifier.size(18.dp))
                Text("Historique", fontSize = 7.sp, color = Indigo, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.MenuBook, null, tint = MockMuted, modifier = Modifier.size(18.dp))
                Text("Documentation", fontSize = 7.sp, color = MockMuted)
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(46.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Indigo, Violet, Teal))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MockCard(title: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .width(38.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFE3E4F2))
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth(title)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MockInk)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MockMuted)
            )
        }
        Icon(Icons.Rounded.Delete, null, tint = Color(0xFFD9534F), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun BoxScope.MockWelcome(s: MockScope) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, IndigoDeep, Indigo)))
    )
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        AppLogo(34.dp)
        Text("Votre scan,\nreconstruit en\nvrai PDF.", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x1AFFFFFF))
        )
    }
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text("Continuer  →", color = Indigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    Marker(s, 1, 0.5f, 0.915f)
}

@Composable
private fun BoxScope.MockHome(s: MockScope) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Historique des scans", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MockInk)
        MockCard(0.7f)
        MockCard(0.5f)
        MockCard(0.6f)
    }
    MockNav()
    Marker(s, 1, 0.17f, 0.935f)
    Marker(s, 2, 0.5f, 0.88f)
    Marker(s, 3, 0.83f, 0.935f)
}

@Composable
private fun BoxScope.MockSheet(s: MockScope) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Historique des scans", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MockInk)
        MockCard(0.7f)
        MockCard(0.5f)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x73000000))
    )
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Scanner un document", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MockInk)
        MockOption(Icons.Rounded.UploadFile, "Téléverser un document à scanner")
        MockOption(Icons.Rounded.CameraAlt, "Utiliser l'appareil photo")
        Spacer(Modifier.height(8.dp))
    }
    Marker(s, 1, 0.9f, 0.775f)
    Marker(s, 2, 0.9f, 0.895f)
}

@Composable
private fun MockOption(icon: ImageVector, label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE9EAF7))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Indigo, Violet))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MockInk)
    }
}

@Composable
private fun BoxScope.MockWorking(s: MockScope) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, IndigoDeep)))
    )
    Column(
        Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Reconstruction en cours", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .fillMaxWidth()
                .height(205.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x1FFFFFFF)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth(0.72f)
                    .height(180.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                for (f in listOf(0.8f, 1f, 0.9f, 1f, 0.6f, 0.95f, 0.7f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MockMuted)
                    )
                }
            }
            Box(
                Modifier
                    .padding(top = 98.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFF5EEAD4))
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .fillMaxHeight()
                    .background(Teal)
            )
        }
        for (i in 0 until 5) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (i < 2) Teal else if (i == 2) Color.White else Color(0x33FFFFFF))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= 2) Color(0xCCFFFFFF) else Color(0x40FFFFFF))
                )
            }
        }
    }
    Marker(s, 1, 0.82f, 0.44f)
    Marker(s, 2, 0.9f, 0.675f)
    Marker(s, 3, 0.1f, 0.77f)
}

@Composable
private fun BoxScope.MockResult(s: MockScope, saved: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text("Résultat", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MockInk)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE3E2FF))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(4.dp, Indigo), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("94%", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = MockInk)
            }
            Text("Fidélité visuelle estimée", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MockInk)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("Comparer", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Indigo)
            Text("Éléments", fontSize = 8.sp, color = MockMuted)
            Text("Texte", fontSize = 8.sp, color = MockMuted)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        ) {
            Column(
                Modifier
                    .padding(12.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Line(0.5f, MockInk, 7.dp)
                Line(1f)
                Line(0.9f)
                Line(1f)
                Line(0.7f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .border(BorderStroke(1.dp, MockInk))
                )
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Indigo)
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Indigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CompareArrows, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color.White)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, Indigo), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Partager", fontSize = 9.sp, color = Indigo, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .weight(1.4f)
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Indigo),
            contentAlignment = Alignment.Center
        ) {
            Text("Enregistrer le PDF", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
    if (saved) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 10.dp, end = 10.dp, bottom = 58.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2B2E4F))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PDF enregistré", fontSize = 8.sp, color = Color.White)
            Text("Ouvrir", fontSize = 8.sp, color = Color(0xFF5EEAD4), fontWeight = FontWeight.Bold)
        }
        Marker(s, 1, 0.27f, 0.93f)
        Marker(s, 2, 0.74f, 0.93f)
        Marker(s, 3, 0.5f, 0.82f)
    } else {
        Marker(s, 1, 0.12f, 0.2f)
        Marker(s, 2, 0.5f, 0.31f)
        Marker(s, 3, 0.5f, 0.55f)
    }
}

@Composable
private fun BoxScope.MockHistory(s: MockScope) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Historique des scans", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MockInk)
        MockCard(0.75f)
        MockCard(0.5f)
        MockCard(0.65f)
        MockCard(0.4f)
    }
    MockNav()
    Marker(s, 1, 0.14f, 0.2f)
    Marker(s, 2, 0.6f, 0.17f)
    Marker(s, 3, 0.9f, 0.2f)
}

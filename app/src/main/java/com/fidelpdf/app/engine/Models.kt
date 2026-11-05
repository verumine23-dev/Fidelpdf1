package com.fidelpdf.app.engine

import android.graphics.Bitmap
import android.graphics.RectF

/** Familles de polices embarquées (métriques compatibles avec les polices Microsoft courantes). */
enum class FontFamilyId(val label: String, val prefix: String) {
    ARIAL("Arial", "LiberationSans"),
    TIMES("Times New Roman", "LiberationSerif"),
    COURIER("Courier New", "LiberationMono"),
    CALIBRI("Calibri", "Carlito"),
    CAMBRIA("Cambria", "Caladea")
}

data class FontChoice(val family: FontFamilyId, val bold: Boolean, val italic: Boolean) {
    val label: String
        get() = family.label + (if (bold) " gras" else "") + (if (italic) " italique" else "")
}

/** Texte réel du PDF. [box] = boîte d'encre mesurée dans le scan (pixels image). */
data class TextItem(
    val id: Int,
    val text: String,
    val original: String,
    val box: RectF,
    val color: Int,
    val font: FontChoice,
    /** Numéro de ligne d'origine (les mots d'une même ligne partagent taille et ligne de base). */
    val line: Int = 0,
    /** Taille et ligne de base harmonisées (0 = calcul automatique). */
    val fixedSize: Float = 0f,
    val fixedBaseline: Float = 0f
)

data class RuleLine(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val thickness: Float,
    val color: Int
)

data class FillRect(val rect: RectF, val color: Int)

enum class CutoutKind(val label: String) {
    STAMP("Tampon"),
    SIGNATURE("Signature"),
    LOGO("Logo"),
    PHOTO("Photo")
}

/** Élément découpé (fond supprimé) puis recollé à sa position d'origine. */
class Cutout(
    val id: Int,
    val kind: CutoutKind,
    val rect: RectF,
    val bitmap: Bitmap,
    val inkColor: Int
)

data class DocModel(
    val width: Int,
    val height: Int,
    val paperColor: Int,
    val fills: List<FillRect>,
    val rules: List<RuleLine>,
    val texts: List<TextItem>,
    val cutouts: List<Cutout>
)

package io.github.artcraftandroid.pdfcraft.ui.viewer

import io.github.artcraftandroid.pdfcraft.jText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.pdfcraft.engine.DigitalId
import io.github.artcraftandroid.pdfcraft.ui.AppViewModel
import io.github.artcraftandroid.pdfcraft.ui.BarIcon
import io.github.artcraftandroid.pdfcraft.ui.LucideIcons
import io.github.artcraftandroid.pdfcraft.ui.OpenDocument
import io.github.artcraftandroid.pdfcraft.ui.PdfDialog
import io.github.artcraftandroid.pdfcraft.ui.PdfField
import io.github.artcraftandroid.pdfcraft.ui.PdfSheet
import io.github.artcraftandroid.pdfcraft.ui.SheetDivider
import io.github.artcraftandroid.pdfcraft.ui.SheetRow
import io.github.artcraftandroid.pdfcraft.ui.SheetTitle
import io.github.artcraftandroid.pdfcraft.ui.pdfColors
import io.github.artcraftandroid.pdfcraft.ui.safeTopPadding
import java.text.DateFormat
import java.util.Date
import org.json.JSONObject

// Digital signatures: signing with a digital ID (a certificate in a .p12 / .pfx file, or one made
// here), and what the signatures in a document say. The engine signs (PAdES, incrementally) and
// validates; these are the screens around it.

/** A signer's certificate, as a signature's details show it. */
data class SignerCert(val name: String, val issuer: String, val validTo: String?, val sha256: String, val selfSigned: Boolean)

/** One signature field and what validating it found (the engine's `sign_list`). */
data class SignatureEntry(
    val field: String,
    val signed: Boolean,
    /** `valid`, `unknown` (intact, but the signer isn't trusted), `invalid` or `unsigned`. */
    val status: String,
    val signer: String?,
    val date: String?,
    val reason: String?,
    val location: String?,
    /** 0-based, for a visible signature or field. */
    val page: Int?,
    /** Points from the page's top-left, for a visible one. */
    val rect: FloatArray?,
    /** Certification level (1 no changes, 2 form filling, 3 comments too), for a certifying signature. */
    val certify: Int?,
    /** Changes since signing: `none`, `allowed` or `disallowed`, and what they were. */
    val modification: String,
    val changes: List<String>,
    /** The engine's explanation, sentence by sentence. */
    val details: List<String>,
    val cert: SignerCert?,
)

private fun JSONObject.text(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

private fun JSONObject.strings(key: String): List<String> =
    optJSONArray(key)?.let { a -> (0 until a.length()).mapNotNull { i -> a.optString(i).takeIf { it.isNotBlank() } } }.orEmpty()

/** The signatures `sign_list` returned. */
fun parseSignatures(o: JSONObject): List<SignatureEntry> {
    val list = o.optJSONArray("signatures") ?: return emptyList()
    return (0 until list.length()).mapNotNull { i ->
        val s = list.optJSONObject(i) ?: return@mapNotNull null
        val cert = s.optJSONObject("certificate")?.let { c ->
            SignerCert(c.text("name") ?: c.text("subject") ?: "", c.text("issuer") ?: "", c.text("valid_to"), c.text("sha256") ?: "", c.optBoolean("self_signed"))
        }
        val rect = s.optJSONArray("rect")?.takeIf { it.length() == 4 }?.let { r -> FloatArray(4) { r.optDouble(it).toFloat() } }
        SignatureEntry(
            field = s.optString("field"),
            signed = s.optBoolean("signed"),
            status = s.text("status") ?: "unsigned",
            signer = s.text("signer") ?: cert?.name?.takeIf { it.isNotEmpty() },
            date = s.text("date"),
            reason = s.text("reason"),
            location = s.text("location"),
            page = if (s.isNull("page")) null else s.optInt("page", 0).takeIf { it >= 1 }?.minus(1),
            rect = rect,
            certify = if (s.isNull("certify")) null else s.optInt("certify", 0).takeIf { it in 1..3 },
            modification = s.text("modification") ?: "none",
            changes = s.strings("changes"),
            details = s.strings("details"),
            cert = cert,
        )
    }
}

/** Where a digital signature goes: an empty signature field, or a box drawn on page [page]. */
class CertTarget(val doc: OpenDocument, val field: String?, val page: Int, val rect: FloatArray?)

/** What a digital signature shows on the page beside the signer's details. */
enum class CertLook(private val labelText: () -> String) { Name({ jText("Name only", "الاسم فقط") }), Typed({ jText("Typed name", "اسم مكتوب") }), Drawn({ jText("My signature", "توقيعي") });
    val label: String get() = labelText()
 }

/** A digital ID file picked to add, waiting for its password ([wrong]: the last one didn't open it). */
class IdImport(val file: java.io.File, val name: String, val wrong: Boolean)

/** A signature's status in a few words, for the list and the message on opening. */
fun SignatureEntry.statusLabel(): String = when {
    !signed -> jText("Not signed yet", "لم يُوقّع بعد")
    status == "valid" -> jText("Valid", "صالح")
    status == "invalid" -> jText("Invalid", "غير صالح")
    else -> jText("Signer not verified", "لم يتم التحقق من الموقّع")
}

private val ValidGreen = Color(0xFF12805C)
private val UnknownAmber = Color(0xFFE68619)

@Composable
private fun statusIcon(s: SignatureEntry): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> = when {
    !s.signed -> LucideIcons.CircleDashed to pdfColors.icon
    s.status == "valid" -> LucideIcons.ShieldCheck to ValidGreen
    s.status == "invalid" -> LucideIcons.CircleX to pdfColors.red
    else -> LucideIcons.TriangleAlert to UnknownAmber
}

/**
 * Drawing where a digital signature goes (Fill & Sign ▸ Sign ▸ Sign with a digital ID): drag a
 * box with one finger (two still scroll and zoom), or tap for a box of the usual size there.
 */
@Composable
fun CertPlaceLayer(vm: AppViewModel, doc: OpenDocument) {
    val c = pdfColors
    var start by remember { mutableStateOf<Offset?>(null) }
    var end by remember { mutableStateOf<Offset?>(null) }
    Box(
        Modifier.fillMaxSize().toolGestures(
            doc,
            onDown = { start = it; end = it },
            onMove = { end = it },
            onUp = { p, moved ->
                val s = start
                start = null
                end = null
                // the hint's Cancel was tapped (it sits over this layer): nothing to place
                if (vm.certPlacing !== doc) return@toolGestures
                val at = s?.let { doc.viewer.screenToPage(it) } ?: return@toolGestures
                val page = doc.info.pages.getOrNull(at.page) ?: return@toolGestures
                val pw = page.width
                val ph = page.height
                val rect = if (moved) {
                    // the far corner in the same page's points, kept on that page
                    val ppp = doc.viewer.pixelsPerPoint(at.page).coerceAtLeast(0.01f)
                    val x1 = (at.x + (p.x - s.x) / ppp).coerceIn(0f, pw)
                    val y1 = (at.y + (p.y - s.y) / ppp).coerceIn(0f, ph)
                    floatArrayOf(minOf(at.x, x1), minOf(at.y, y1), maxOf(at.x, x1), maxOf(at.y, y1))
                } else {
                    null
                }
                vm.placeCert(doc, at.page, fitSignatureBox(rect, at.x, at.y, pw, ph))
            },
            onCancel = { start = null; end = null },
        ),
    ) {
        val s = start
        val e = end
        if (s != null && e != null) {
            Canvas(Modifier.fillMaxSize()) {
                val tl = Offset(minOf(s.x, e.x), minOf(s.y, e.y))
                val size = Size(kotlin.math.abs(e.x - s.x), kotlin.math.abs(e.y - s.y))
                drawRect(c.accent.copy(alpha = 0.12f), tl, size)
                drawRect(c.accent, tl, size, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
            }
        }
        val top = with(LocalDensity.current) { doc.viewer.padTop.toDp() }
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = top + 12.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(8.dp)).background(c.surface).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(jText("Drag a box where the signature goes", "اسحب لرسم مربع في موضع التوقيع"), color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f, fill = false))
            TextButton(onClick = { vm.certPlacing = null }) { Text(jText("Cancel", "إلغاء"), color = c.accent) }
        }
    }
}

/** The signature box: [rect] when it is big enough, else one of the usual size around ([x], [y]); kept on the page. */
internal fun fitSignatureBox(rect: FloatArray?, x: Float, y: Float, pageW: Float, pageH: Float): FloatArray {
    if (rect != null && rect[2] - rect[0] >= 40f && rect[3] - rect[1] >= 16f) return rect
    val w = minOf(180f, pageW)
    val h = minOf(54f, pageH)
    val cx = rect?.let { (it[0] + it[2]) / 2f } ?: x
    val cy = rect?.let { (it[1] + it[3]) / 2f } ?: y
    val left = (cx - w / 2f).coerceIn(0f, pageW - w)
    val top = (cy - h / 2f).coerceIn(0f, pageH - h)
    return floatArrayOf(left, top, left + w, top + h)
}

/**
 * Sign with a digital ID: choose the ID (or add or create one), how the signature looks, an
 * optional reason and place, then the ID's password signs and saves the document.
 */
@Composable
fun CertSignScreen(vm: AppViewModel, target: CertTarget) {
    val c = pdfColors
    androidx.activity.compose.BackHandler { vm.certSign = null }
    val ids = vm.digitalIds.all
    val chosen = vm.chosenId?.takeIf { it in ids }
    val hasDrawn = remember(vm.signaturesChanged) { vm.signatures.signature.isFile }
    var look by remember { mutableStateOf(CertLook.Name) }
    var reason by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var password by remember(chosen) { mutableStateOf("") }
    val unusable = chosen?.let { idProblem(it) }
    val canSign = chosen != null && unusable == null
    fun sign() {
        if (chosen != null && canSign) vm.signWithId(target, chosen, password, if (look == CertLook.Drawn && !hasDrawn) CertLook.Name else look, reason, location)
    }
    Column(Modifier.fillMaxSize().background(c.surface).pointerInput(Unit) { detectTapGestures() }.safeTopPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.certSign = null }) { Text(jText("Cancel", "إلغاء"), color = c.accent) }
            Text(jText("Sign with a digital ID", "التوقيع باستخدام هوية رقمية"), color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = ::sign, enabled = canSign) { Text(jText("Sign", "توقيع"), color = if (canSign) c.accent else c.textSecondary, fontWeight = FontWeight.SemiBold) }
        }
        SheetDivider(inset = 0.dp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            Section(jText("Digital ID", "الهوية الرقمية"))
            if (ids.isEmpty()) {
                Text(
                    jText("You don't have a digital ID yet. Add the .p12 or .pfx file you were given, or create one for yourself.", "ليست لديك هوية رقمية بعد. أضف ملف ‎.p12 أو ‎.pfx الذي حصلت عليه، أو أنشئ هوية لنفسك."),
                    color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            for (id in ids) IdRow(vm, id, selected = id == chosen) { vm.chosenId = id }
            SheetRow(LucideIcons.FolderOpen, jText("Add a digital ID file", "إضافة ملف هوية رقمية"), { vm.pickDigitalId() })
            SheetRow(LucideIcons.Plus, jText("Create a digital ID", "إنشاء هوية رقمية"), { vm.idCreate = true })
            SheetDivider(vertical = 6.dp)

            Section(jText("Appearance", "المظهر"))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (l in CertLook.entries) {
                    val enabled = l != CertLook.Drawn || hasDrawn
                    Chip(l.label, look == l, enabled) { look = l }
                }
            }
            Text(
                when (look) {
                    CertLook.Name -> jText("Your name and the date in the box.", "اسمك والتاريخ داخل المربع.")
                    CertLook.Typed -> jText("Your name in a script font, with your name and the date beside it.", "اسمك بخط زخرفي، وبجانبه اسمك والتاريخ.")
                    CertLook.Drawn -> if (hasDrawn) jText("Your saved Fill & Sign signature, with your name and the date beside it.", "توقيعك المحفوظ في التعبئة والتوقيع، وبجانبه اسمك والتاريخ.") else jText("Make a signature in Fill & Sign first.", "أنشئ توقيعًا في التعبئة والتوقيع أولًا.")
                },
                color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            SheetDivider(vertical = 6.dp)

            Section(jText("Reason (optional)", "السبب (اختياري)"))
            Box(Modifier.padding(horizontal = 16.dp)) { PdfField(reason, { reason = it }, jText("For example: I approve this document", "مثلًا: أوافق على هذا المستند"), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)) }
            Spacer(Modifier.height(12.dp))
            Section(jText("Location (optional)", "الموقع (اختياري)"))
            Box(Modifier.padding(horizontal = 16.dp)) { PdfField(location, { location = it }, jText("City or place", "المدينة أو المكان"), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)) }
            Spacer(Modifier.height(12.dp))

            if (chosen != null) {
                Section(jText("Password for ${chosen.name}", "كلمة مرور ${chosen.name}"))
                Box(Modifier.padding(horizontal = 16.dp)) {
                    PdfField(
                        password, { password = it; vm.certSignError = null }, jText("Digital ID password", "كلمة مرور الهوية الرقمية"), password = true, error = vm.certSignError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { sign() }),
                    )
                }
            }
            (vm.certSignError ?: unusable)?.let { Text(it, color = c.red, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            Text(
                jText("Signing saves the document. Anyone who opens it can check that it hasn't changed since; changes made after signing are listed with the signature.", "يؤدي التوقيع إلى حفظ المستند. يمكن لمن يفتحه التحقق من سلامته، وتظهر التغييرات اللاحقة ضمن تفاصيل التوقيع."),
                color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

/** Why [id] can't sign now, if it can't. */
private fun idProblem(id: DigitalId): String? = when {
    id.expired() -> jText("This digital ID expired on ${shortDate(id.validTo * 1000)}.", "انتهت صلاحية هذه الهوية الرقمية في ${shortDate(id.validTo * 1000)}.")
    id.notYetValid() -> jText("This digital ID isn't valid until ${shortDate(id.validFrom * 1000)}.", "تبدأ صلاحية هذه الهوية الرقمية في ${shortDate(id.validFrom * 1000)}.")
    else -> null
}

private fun shortDate(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

@Composable
private fun Section(title: String) {
    Text(title, color = pdfColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp))
}

@Composable
private fun Chip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = pdfColors
    Text(
        label,
        color = when {
            !enabled -> c.textSecondary.copy(alpha = 0.5f)
            selected -> c.accent
            else -> c.text
        },
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (selected) c.pressed else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** A digital ID to choose: whose it is, who issued it and until when, and Remove. */
@Composable
private fun IdRow(vm: AppViewModel, id: DigitalId, selected: Boolean, onClick: () -> Unit) {
    val c = pdfColors
    var confirm by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (selected) LucideIcons.CircleDot else LucideIcons.Circle, null, tint = if (selected) c.accent else c.icon, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(id.name, color = c.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val by = if (id.selfSigned) jText("Self-signed", "موقّعة ذاتيًا") else jText("Issued by ${id.issuer}", "صادرة عن ${id.issuer}")
            val until = idProblem(id) ?: jText("Valid until ${shortDate(id.validTo * 1000)}", "صالحة حتى ${shortDate(id.validTo * 1000)}")
            Text("$by · $until", color = if (idProblem(id) != null) c.red else c.textSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        BarIcon(LucideIcons.Trash2, jText("Remove ${id.name}'s digital ID", "إزالة الهوية الرقمية لـ ${id.name}"), { confirm = true }, size = 40.dp)
    }
    if (confirm) {
        PdfDialog(
            onDismiss = { confirm = false }, title = jText("Remove digital ID?", "إزالة الهوية الرقمية؟"), confirm = jText("Remove", "إزالة"), destructive = true,
            onConfirm = { confirm = false; vm.removeDigitalId(id) },
        ) { Text(jText("${id.name}'s digital ID is removed from Jahfan. Documents already signed with it keep their signatures.", "ستُزال الهوية الرقمية لـ ${id.name} من جحفان. تحتفظ المستندات الموقّعة مسبقًا بتوقيعاتها."), color = c.text, fontSize = 14.sp) }
    }
}

/** The password of a digital ID file being added. */
@Composable
fun IdPasswordDialog(vm: AppViewModel, imp: IdImport) {
    val c = pdfColors
    var pw by remember(imp) { mutableStateOf("") }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(imp) { runCatching { focus.requestFocus() } }
    PdfDialog(onDismiss = { vm.answerIdImport(null) }, title = jText("Digital ID password", "كلمة مرور الهوية الرقمية"), confirm = jText("Add", "إضافة"), onConfirm = { vm.answerIdImport(pw) }) {
        Text(jText("Enter the password that protects ${imp.name}.", "أدخل كلمة المرور التي تحمي ${imp.name}."), color = c.text, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        PdfField(
            pw, { pw = it }, jText("Password", "كلمة المرور"), error = imp.wrong, password = true, focus = focus,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { vm.answerIdImport(pw) }),
        )
        if (imp.wrong) Text(jText("The password is incorrect.", "كلمة المرور غير صحيحة."), color = c.red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Create a digital ID: a self-signed certificate made on the phone, saved with a password. */
@Composable
fun CreateIdScreen(vm: AppViewModel) {
    val c = pdfColors
    androidx.activity.compose.BackHandler { vm.idCreate = false }
    var name by remember { mutableStateOf(vm.toolPrefs.author) }
    var email by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val problem = when {
        name.isBlank() -> null
        password.isNotEmpty() && password.length < 8 -> jText("The password needs at least 8 characters.", "يجب ألا تقل كلمة المرور عن 8 أحرف.")
        again.isNotEmpty() && again != password -> jText("The passwords don't match.", "كلمتا المرور غير متطابقتين.")
        else -> null
    }
    val ok = name.isNotBlank() && password.length >= 8 && again == password
    Column(Modifier.fillMaxSize().background(c.surface).pointerInput(Unit) { detectTapGestures() }.safeTopPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.idCreate = false }) { Text(jText("Cancel", "إلغاء"), color = c.accent) }
            Text(jText("Create a digital ID", "إنشاء هوية رقمية"), color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = { vm.createDigitalId(name, email, organization, password) }, enabled = ok) {
                Text(jText("Create", "إنشاء"), color = if (ok) c.accent else c.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
        SheetDivider(inset = 0.dp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            Text(
                jText("A digital ID made here is self-signed: a document you sign with it shows any change made after you signed, but nobody else vouches that it's you. ", "الهوية المنشأة هنا موقّعة ذاتيًا: يكشف توقيعها أي تغيير لاحق في المستند، لكنه لا يثبت هويتك بواسطة جهة خارجية. ") +
                    jText("For that, use the ID file an organisation or a certificate authority gives you.", "لإثبات الهوية استخدم ملف هوية من مؤسسة أو جهة إصدار شهادات."),
                color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Section(jText("Name", "الاسم"))
            Box(Modifier.padding(horizontal = 16.dp)) { PdfField(name, { name = it }, jText("Your name", "اسمك")) }
            Section(jText("Email (optional)", "البريد الإلكتروني (اختياري)"))
            Box(Modifier.padding(horizontal = 16.dp)) { PdfField(email, { email = it }, "name@example.com", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)) }
            Section(jText("Organisation (optional)", "المؤسسة (اختياري)"))
            Box(Modifier.padding(horizontal = 16.dp)) { PdfField(organization, { organization = it }, jText("Company or organisation", "الشركة أو المؤسسة")) }
            Section(jText("Password", "كلمة المرور"))
            Box(Modifier.padding(horizontal = 16.dp)) {
                PdfField(password, { password = it }, jText("At least 8 characters", "8 أحرف على الأقل"), password = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                PdfField(again, { again = it }, jText("The password again", "أعد كتابة كلمة المرور"), password = true, error = again.isNotEmpty() && again != password, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))
            }
            problem?.let { Text(it, color = c.red, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            Text(
                jText("Jahfan doesn't keep the password and can't recover it: you type it each time you sign.", "لا يحتفظ جحفان بكلمة المرور ولا يمكنه استعادتها؛ أدخلها عند كل توقيع."),
                color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

/** Tapping an empty signature field: draw a signature there, or sign it with a digital ID. */
@Composable
fun SignFieldChoiceDialog(vm: AppViewModel, doc: OpenDocument, field: io.github.artcraftandroid.pdfcraft.core.WidgetInfo) {
    PdfDialog(onDismiss = { vm.signFieldChoice = null }, title = null, confirm = null, dismiss = null, list = true) {
        SheetRow(LucideIcons.Signature, jText("Add your signature", "إضافة توقيعك"), { vm.signFieldChoice = null; vm.drawSignatureInField(doc, field) })
        SheetRow(LucideIcons.ShieldCheck, jText("Sign with a digital ID", "التوقيع باستخدام هوية رقمية"), { vm.signFieldChoice = null; vm.signFieldWithId(doc, field) })
    }
}

/**
 * Signatures (More ▸ Signatures): each signature field with whether its signature is valid, who
 * signed and when, and on tap the details (certificate, changes since signing, the engine's
 * explanation), Trust this signer, and Sign for an empty field.
 */
@Composable
fun SignaturesSheet(vm: AppViewModel, doc: OpenDocument) {
    val c = pdfColors
    val ctl = doc.controller
    fun close() {
        ctl.sheet = ViewerSheet.None
    }
    // read so Trust redraws the list
    vm.trustedCerts.version
    var open by remember { mutableStateOf<String?>(null) }
    PdfSheet(onDismiss = ::close, maxHeightFraction = 0.85f) {
        SheetTitle(jText("Signatures", "التوقيعات"))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            val list = doc.signatures
            if (list.isEmpty()) {
                Text(jText("This document has no signatures.", "لا توجد توقيعات في هذا المستند."), color = c.textSecondary, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
            for (s in list) {
                val (icon, tint) = statusIcon(s)
                Row(
                    Modifier.fillMaxWidth().clickable { open = if (open == s.field) null else s.field }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (s.signed) s.signer ?: jText("Unknown signer", "موقّع غير معروف") else jText("Empty signature field", "حقل توقيع فارغ"), color = c.text, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val sub = listOfNotNull(
                            s.statusLabel(),
                            if (s.certify != null && s.signed) jText("Certified", "معتمد") else null,
                            pdfDateLabel(s.date),
                            s.page?.let { jText("Page ${it + 1}", "الصفحة ${it + 1}") }.takeIf { !s.signed },
                        ).joinToString(" · ")
                        Text(sub, color = if (s.status == "invalid") c.red else c.textSecondary, fontSize = 13.sp)
                        if (open == s.field) SignatureDetails(vm, doc, s)
                    }
                    Icon(if (open == s.field) LucideIcons.ChevronUp else LucideIcons.ChevronDown, null, tint = c.icon, modifier = Modifier.size(20.dp))
                }
                SheetDivider()
            }
            SheetRow(LucideIcons.ShieldCheck, jText("Add a digital signature", "إضافة توقيع رقمي"), { close(); vm.startCertPlacing(doc) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SignatureDetails(vm: AppViewModel, doc: OpenDocument, s: SignatureEntry) {
    val c = pdfColors
    @Composable
    fun line(label: String, value: String?) {
        if (value.isNullOrBlank()) return
        Text("$label: $value", color = c.text, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
    }
    Column(Modifier.padding(top = 4.dp)) {
        if (s.signed) {
            line(jText("Reason", "السبب"), s.reason)
            line(jText("Location", "الموقع"), s.location)
            s.cert?.let { cert ->
                line(jText("Certificate", "الشهادة"), if (cert.selfSigned) "self-signed" else jText("issued by ${cert.issuer}", "صادرة عن ${cert.issuer}"))
                line(jText("Valid until", "صالحة حتى"), pdfDateLabel(cert.validTo))
            }
            s.certify?.let {
                line(jText("Certified", "معتمد"), when (it) { 1 -> jText("no changes allowed", "لا يُسمح بأي تغييرات"); 2 -> jText("form filling allowed", "يُسمح بتعبئة النماذج"); else -> jText("form filling and comments allowed", "يُسمح بتعبئة النماذج والتعليقات") })
            }
            line(
                jText("Changes since signing", "التغييرات منذ التوقيع"),
                when (s.modification) {
                    "none" -> "none"
                    "allowed" -> jText("allowed ones", "تغييرات مسموحة") + if (s.changes.isEmpty()) "" else " (${s.changes.joinToString("; ")})"
                    else -> jText("some that aren't allowed", "توجد تغييرات غير مسموحة") + if (s.changes.isEmpty()) "" else " (${s.changes.joinToString("; ")})"
                },
            )
            for (d in s.details) Text("• $d", color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        } else {
            Text(jText("Nobody has signed this field yet.", "لم يوقّع أحد هذا الحقل بعد."), color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (s.page != null && s.rect != null) {
                TextButton(onClick = { doc.controller.sheet = ViewerSheet.None; vm.showSignature(doc, s) }) { Text(jText("Go to it", "الانتقال إليه"), color = c.accent) }
            }
            val sha = s.cert?.sha256
            if (s.signed && sha != null) {
                if (vm.trustedCerts.contains(sha)) {
                    TextButton(onClick = { vm.untrustSigner(doc, s) }) { Text(jText("Stop trusting", "إلغاء الثقة"), color = c.accent) }
                } else if (s.status == "unknown") {
                    TextButton(onClick = { vm.trustSigner(doc, s) }) { Text(jText("Trust this signer", "الوثوق بهذا الموقّع"), color = c.accent) }
                }
            }
            if (!s.signed) {
                TextButton(onClick = {
                    doc.controller.sheet = ViewerSheet.None
                    doc.widgets.firstOrNull { it.field == s.field }?.let { vm.signFieldWithId(doc, it) }
                }) { Text(jText("Sign", "توقيع"), color = c.accent) }
            }
        }
    }
}

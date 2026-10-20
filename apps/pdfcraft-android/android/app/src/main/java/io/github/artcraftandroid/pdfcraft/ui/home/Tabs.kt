package io.github.artcraftandroid.pdfcraft.ui.home

import io.github.artcraftandroid.pdfcraft.jText

import io.github.artcraftandroid.pdfcraft.ui.safeTopPadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.artcraftandroid.pdfcraft.ui.AppViewModel
import io.github.artcraftandroid.pdfcraft.ui.BarIcon
import io.github.artcraftandroid.pdfcraft.ui.Inter
import io.github.artcraftandroid.pdfcraft.ui.LucideIcons
import io.github.artcraftandroid.pdfcraft.ui.PdfSwitch
import io.github.artcraftandroid.pdfcraft.ui.SheetDivider
import io.github.artcraftandroid.pdfcraft.ui.SheetRow
import io.github.artcraftandroid.pdfcraft.ui.pdfColors
import io.github.artcraftandroid.pdfcraft.ui.viewer.ToolHues

/** Files: the places files come from; On this device lists every PDF on the phone by folder. */
@Composable
fun FilesTab(vm: AppViewModel, onBrowse: () -> Unit) {
    val c = pdfColors
    if (vm.filesShowLocal) {
        OnThisDevice(vm)
        return
    }
    Column(Modifier.fillMaxSize().safeTopPadding()) {
        // a large title, as the expanded bar of the reference shows it
        Box(Modifier.fillMaxWidth().height(128.dp), contentAlignment = Alignment.BottomStart) {
            Text(jText("Files", "الملفات"), color = c.label, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, bottom = 40.dp))
        }
        LocationRow(LucideIcons.Smartphone, jText("On this device", "على هذا الجهاز")) { vm.filesShowLocal = true }
        LocationRow(LucideIcons.Image, jText("Photos", "الصور")) { vm.pickPhotosToPdf() }
        LocationRow(LucideIcons.FolderOpen, jText("Look in other folders", "استعراض مجلدات أخرى"), onBrowse)
    }
}

@Composable
private fun LocationRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = pdfColors
    Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).clickable(onClick = onClick).padding(start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = c.icon, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = c.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
    }
}

/** On this device: every PDF found on the phone, newest first, under its folder's name. */
@Composable
private fun OnThisDevice(vm: AppViewModel) {
    val c = pdfColors
    BackHandler { vm.filesShowLocal = false }
    LaunchedEffect(Unit) { vm.scanLocal() }
    var sort by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeTopPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { BarIcon(LucideIcons.ArrowLeft, jText("Back", "رجوع"), { vm.filesShowLocal = false }) }
            Text(jText("On this device", "على هذا الجهاز"), color = c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            BarIcon(LucideIcons.RotateCw, jText("Refresh", "تحديث"), { vm.scanLocal(force = true) })
            Box {
                BarIcon(LucideIcons.EllipsisVertical, jText("More options", "خيارات إضافية"), { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surface) {
                    DropdownMenuItem(text = { Text(jText("Sort by name", "ترتيب حسب الاسم")) }, onClick = { sort = 1; menu = false }, trailingIcon = { if (sort == 1) Icon(LucideIcons.Check, null, tint = c.accent) })
                    DropdownMenuItem(text = { Text(jText("Sort by date", "ترتيب حسب التاريخ")) }, onClick = { sort = 0; menu = false }, trailingIcon = { if (sort == 0) Icon(LucideIcons.Check, null, tint = c.accent) })
                    HorizontalDivider(color = c.divider)
                    DropdownMenuItem(text = { Text(jText("Add a folder", "إضافة مجلد")) }, onClick = { menu = false; vm.requestFileAccess() }, leadingIcon = { Icon(LucideIcons.FolderPlus, null, tint = c.icon) })
                    // the folders listed here, each with a way to stop listing it
                    for ((tree, name) in vm.localFolders()) {
                        DropdownMenuItem(text = { Text(jText("Stop listing $name", "إيقاف عرض $name")) }, onClick = { menu = false; vm.removeLocalFolder(tree) }, leadingIcon = { Icon(LucideIcons.Folder, null, tint = c.icon) })
                    }
                }
            }
        }
        val rows = vm.localRows().let { if (sort == 1) it.sortedBy { r -> r.name.lowercase() } else it }
        // what PdfCraft saved itself shows before any folder is added
        if (!vm.hasFileAccess && rows.isEmpty()) {
            EmptyState(LucideIcons.Smartphone, jText("Files on your device", "الملفات على جهازك"), jText("Choose folders to list PDFs from, such as Documents or a folder inside Download (Android doesn't let apps list the whole Download folder). Jahfan sees only the folders you choose, and files you open from anywhere stay in Recent.", "اختر مجلدات لعرض ملفات PDF منها، مثل المستندات أو مجلد داخل التنزيلات. لا يسمح Android للتطبيقات بعرض مجلد التنزيلات كاملًا. لا يصل جحفان إلا إلى المجلدات التي تختارها، وتبقى الملفات التي تفتحها من أي مكان في قائمة الأخيرة."), jText("Add a folder", "إضافة مجلد")) { vm.requestFileAccess() }
            return@Column
        }
        if (rows.isEmpty()) {
            if (vm.scanning) EmptyState(LucideIcons.FileText, jText("Looking for PDFs…", "جارٍ البحث عن ملفات PDF…"), "")
            else EmptyState(LucideIcons.FileText, jText("No PDFs in your folders", "لا توجد ملفات PDF في مجلداتك"), jText("PDFs in the folders you added will appear here.", "ستظهر هنا ملفات PDF الموجودة في المجلدات التي أضفتها."), jText("Add a folder", "إضافة مجلد")) { vm.requestFileAccess() }
            return@Column
        }
        LazyColumn(Modifier.weight(1f)) {
            val groups = rows.groupBy { it.location }
            for ((folder, list) in groups) {
                item(key = "h-$folder") {
                    Text(folder.uppercase().ifEmpty { jText("INTERNAL STORAGE", "وحدة التخزين الداخلية") }, color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                }
                items(list, key = { it.key }) { r -> FileListRow(vm, r) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** A tool of the Tools tab: its tile, name and one line about it. */
private class HubTool(val icon: ImageVector, val name: String, val about: String, val hue: Color, val categories: Set<String>, val go: () -> Unit)

/** Where a category lists its tools in an order of its own (the rest keep the All order). */
private val CategoryOrder get() = mapOf(
    jText("Edit", "تحرير") to listOf(jText("Edit PDF", "تحرير PDF"), jText("Crop pages", "اقتصاص الصفحات"), jText("Organize pages", "تنظيم الصفحات"), jText("Combine files", "دمج الملفات"), jText("Set password", "تعيين كلمة مرور")),
    jText("Convert", "تحويل") to listOf(jText("Export PDF", "تصدير PDF"), jText("Compress PDF", "ضغط PDF")),
)

/** Tools: every tool, by category (All, Read, Create, Edit, Convert), as outlined cards. */
@Composable
fun ToolsTab(vm: AppViewModel) {
    val c = pdfColors
    val categories = listOf(jText("All", "الكل"), jText("Read", "قراءة"), jText("Create", "إنشاء"), jText("Edit", "تحرير"), jText("Convert", "تحويل"))
    var cat by remember { mutableStateOf(jText("All", "الكل")) }
    val tools = remember {
        listOf(
            HubTool(LucideIcons.Signature, jText("Fill & Sign", "تعبئة وتوقيع"), jText("Fill in forms and sign them", "تعبئة النماذج وتوقيعها"), ToolHues.sign, emptySet()) { vm.toolForFile(FileTool.FillSign) },
            HubTool(LucideIcons.MessageSquareText, jText("Add comment", "إضافة تعليق"), jText("Comment, highlight and draw", "التعليق والتمييز والرسم"), ToolHues.comment, emptySet()) { vm.toolForFile(FileTool.Comment) },
            HubTool(LucideIcons.Volume2, jText("Read aloud", "القراءة بصوت عالٍ"), jText("Listen to a PDF", "الاستماع إلى ملف PDF"), ToolHues.read, setOf(jText("Read", "قراءة"))) { vm.toolForFile(FileTool.ReadAloud) },
            HubTool(LucideIcons.FilePlus2, jText("Create PDF", "إنشاء PDF"), jText("Turn images and text into PDFs", "تحويل الصور والنصوص إلى ملفات PDF"), ToolHues.create, setOf(jText("Create", "إنشاء"))) { vm.openCreate() },
            HubTool(LucideIcons.Camera, jText("New scan", "مسح ضوئي جديد"), jText("Scan paper with the camera", "مسح الأوراق بالكاميرا"), ToolHues.scan, setOf(jText("Create", "إنشاء"))) { vm.startScan() },
            HubTool(LucideIcons.Combine, jText("Combine files", "دمج الملفات"), jText("Join several files into one PDF", "دمج عدة ملفات في ملف PDF واحد"), ToolHues.combine, setOf(jText("Create", "إنشاء"), jText("Edit", "تحرير"))) { vm.pickFiles(io.github.artcraftandroid.pdfcraft.ui.FilePickKind.Combine) },
            HubTool(LucideIcons.LayoutGrid, jText("Organize pages", "تنظيم الصفحات"), jText("Reorder, rotate, delete and insert pages", "إعادة ترتيب الصفحات وتدويرها وحذفها وإدراجها"), ToolHues.organize, setOf(jText("Edit", "تحرير"))) { vm.toolForFile(FileTool.Organize) },
            HubTool(LucideIcons.Crop, jText("Crop pages", "اقتصاص الصفحات"), jText("Trim margins or change page size", "قص الهوامش أو تغيير حجم الصفحة"), ToolHues.crop, setOf(jText("Edit", "تحرير"))) { vm.toolForFile(FileTool.Crop) },
            HubTool(LucideIcons.FilePenLine, jText("Edit PDF", "تحرير PDF"), jText("Change the text and pictures", "تعديل النصوص والصور"), ToolHues.edit, setOf(jText("Edit", "تحرير"))) { vm.toolForFile(FileTool.Edit) },
            HubTool(LucideIcons.FileArchive, jText("Compress PDF", "ضغط PDF"), jText("Make a PDF smaller", "تقليل حجم ملف PDF"), ToolHues.compress, setOf(jText("Convert", "تحويل"))) { vm.toolForFile(FileTool.Compress) },
            HubTool(LucideIcons.FileOutput, jText("Export PDF", "تصدير PDF"), jText("Save a PDF as a document, spreadsheet, slides or images", "حفظ PDF كمستند أو جدول بيانات أو شرائح أو صور"), ToolHues.export, setOf(jText("Convert", "تحويل"))) { vm.toolForFile(FileTool.Export) },
            HubTool(LucideIcons.FileLock2, jText("Set password", "تعيين كلمة مرور"), jText("Protect a PDF with a password", "حماية ملف PDF بكلمة مرور"), ToolHues.protect, setOf(jText("Edit", "تحرير"))) { vm.toolForFile(FileTool.Protect) },
        )
    }
    Column(Modifier.fillMaxSize().safeTopPadding()) {
        Text(jText("Tools", "الأدوات"), color = c.label, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp))
        // text tabs with a 2 dp underline under the chosen one
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            for (k in categories) {
                Column(Modifier.clickable(role = Role.Tab) { cat = k }.width(androidx.compose.foundation.layout.IntrinsicSize.Max)) {
                    Text(k, color = if (k == cat) c.label else c.textTertiary, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = if (k == cat) FontWeight.SemiBold else FontWeight.Normal)
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (k == cat) c.label else Color.Transparent))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            GridCells.Adaptive(140.dp), modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(tools.filter { cat == jText("All", "الكل") || cat in it.categories }.sortedBy { t -> CategoryOrder[cat]?.indexOf(t.name)?.takeIf { it >= 0 } ?: Int.MAX_VALUE }) { t ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface)
                        .border(1.dp, c.divider, RoundedCornerShape(12.dp)).clickable(onClick = t.go).padding(16.dp).semantics { contentDescription = t.name },
                ) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(5.dp)).background(tint(t.hue, c.dark)), contentAlignment = Alignment.Center) {
                        Icon(t.icon, null, tint = t.hue, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(t.name, color = c.label, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(t.about, color = c.label, fontSize = 15.sp, lineHeight = 21.sp)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/** Search: file names on the phone and in Recent, with the reference's sort choices. */
@Composable
fun SearchTab(vm: AppViewModel) {
    val c = pdfColors
    var query by remember { mutableStateOf("") }
    var sort by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.scanLocal() }
    Column(Modifier.fillMaxSize().safeTopPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clip(CircleShape).background(c.surfaceRaised).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(LucideIcons.Search, null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.addRecentSearch(query) }),
                textStyle = TextStyle(fontFamily = Inter, fontSize = 16.sp, color = c.text), cursorBrush = SolidColor(c.accent),
                modifier = Modifier.weight(1f).semantics { contentDescription = jText("Search your files", "ابحث في ملفاتك") },
                decorationBox = { inner -> Box { if (query.isEmpty()) Text(jText("Search your files", "ابحث في ملفاتك"), color = c.textSecondary, fontSize = 16.sp); inner() } },
            )
            if (query.isNotEmpty()) BarIcon(LucideIcons.X, jText("Clear", "مسح"), { query = "" }, size = 32.dp)
        }
        if (query.isBlank()) {
            val recent = vm.recentSearches
            if (recent.isEmpty()) {
                EmptyState(LucideIcons.Search, jText("Search your files", "ابحث في ملفاتك"), jText("Find PDFs on this phone by name.", "ابحث عن ملفات PDF على هذا الهاتف بالاسم."))
                return@Column
            }
            // the last searches, to run again with a tap
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(jText("Recent searches", "عمليات البحث الأخيرة"), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(jText("Clear", "مسح"), color = c.toolAccent, fontSize = 15.sp, modifier = Modifier.clickable { vm.clearRecentSearches() }.padding(12.dp))
            }
            LazyColumn(Modifier.weight(1f)) {
                items(recent, key = { it }) { q ->
                    Row(Modifier.fillMaxWidth().height(52.dp).clickable { query = q }.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(LucideIcons.Clock, null, tint = c.icon, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(q, color = c.text, fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        BarIcon(LucideIcons.X, jText("Remove $q", "إزالة $q"), { vm.removeRecentSearch(q) }, size = 40.dp)
                    }
                }
            }
            return@Column
        }
        // a search left in place for a moment counts as one made
        LaunchedEffect(query) {
            kotlinx.coroutines.delay(2000)
            if (vm.searchFiles(query).isNotEmpty()) vm.addRecentSearch(query)
        }
        val results = vm.searchFiles(query).let { list ->
            when (sort) {
                1 -> list.sortedBy { it.name.lowercase() }
                2 -> list.sortedByDescending { it.name.lowercase() }
                3 -> list.sortedBy { it.date }
                4 -> list.sortedByDescending { it.date }
                // most relevant: names starting with the search first, then the newest
                else -> list.sortedWith(compareBy<FileRow> { !it.name.lowercase().startsWith(query.trim().lowercase()) }.thenByDescending { it.date })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(jText("${results.size} ${if (results.size == 1) "result" else "results"} on this device", "عدد النتائج على هذا الجهاز: ${results.size}"), color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box {
                Row(Modifier.clickable { menu = true }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(listOf(jText("Most relevant", "الأكثر صلة"), jText("Name A→Z", "الاسم: تصاعديًا"), jText("Name Z→A", "الاسم: تنازليًا"), jText("Least recently modified", "الأقدم تعديلًا"), jText("Most recently modified", "الأحدث تعديلًا"))[sort], color = c.label, fontSize = 15.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(LucideIcons.ArrowUpDown, null, tint = c.icon, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surface) {
                    listOf(jText("Most relevant", "الأكثر صلة"), jText("Name A→Z", "الاسم: تصاعديًا"), jText("Name Z→A", "الاسم: تنازليًا"), jText("Least recently modified", "الأقدم تعديلًا"), jText("Most recently modified", "الأحدث تعديلًا")).forEachIndexed { i, s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { sort = i; menu = false }, trailingIcon = { if (sort == i) Icon(LucideIcons.Check, null, tint = c.accent) })
                    }
                }
            }
        }
        if (results.isEmpty()) {
            EmptyState(LucideIcons.FileSearch, jText("No results", "لا توجد نتائج"), jText("Check the spelling, or try a shorter name.", "تحقق من الكتابة أو جرّب اسمًا أقصر."))
        } else {
            LazyColumn(Modifier.weight(1f)) { items(results, key = { it.key }) { r -> FileListRow(vm, r) } }
        }
    }
}

/** Settings: About, Preferences, Help & Support. */
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val c = pdfColors
    var page by remember { mutableStateOf("root") }
    BackHandler { if (page == "root") vm.settingsOpen = false else page = "root" }
    Column(Modifier.fillMaxSize().background(c.surface).safeTopPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                BarIcon(LucideIcons.ArrowLeft, jText("Back", "رجوع"), { if (page == "root") vm.settingsOpen = false else page = "root" })
            }
            Text(when (page) { "about" -> jText("About Jahfan", "حول جحفان"); "prefs" -> jText("Preferences", "التفضيلات"); "help" -> jText("Help & Support", "المساعدة والدعم"); else -> jText("Settings", "الإعدادات") }, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            when (page) {
                "root" -> {
                    SheetRow(LucideIcons.Info, jText("About Jahfan", "حول جحفان"), { page = "about" }, chevron = true)
                    SheetRow(LucideIcons.Settings, jText("Preferences", "التفضيلات"), { page = "prefs" }, chevron = true)
                    SheetRow(LucideIcons.CircleHelp, jText("Help & Support", "المساعدة والدعم"), { page = "help" }, chevron = true)
                }
                "prefs" -> Preferences(vm)
                "about" -> About(vm)
                "help" -> {
                    val uri = LocalUriHandler.current
                    var leaving by remember { mutableStateOf<String?>(null) }
                    SheetRow(LucideIcons.CircleHelp, jText("Help", "المساعدة"), { leaving = "$APP_REPO#readme" })
                    SheetRow(LucideIcons.MessageSquare, jText("Report a problem or request a feature", "الإبلاغ عن مشكلة أو طلب ميزة"), { leaving = "$APP_REPO/issues" })
                    leaving?.let { url ->
                        io.github.artcraftandroid.pdfcraft.ui.PdfDialog(
                            onDismiss = { leaving = null }, title = jText("Leaving Jahfan", "مغادرة جحفان"), confirm = jText("Continue", "متابعة"), onConfirm = { leaving = null; uri.openUri(url) },
                        ) { Text(jText("This opens a web page in your browser.", "سيُفتح موقع في متصفحك."), color = c.text, fontSize = 14.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Preferences(vm: AppViewModel) {
    val c = pdfColors
    var authorDialog by remember { mutableStateOf(false) }
    val activity = androidx.activity.compose.LocalActivity.current
    Section(jText("Language", "اللغة"))
    SheetRow(null, "العربية", { activity?.let { io.github.artcraftandroid.pdfcraft.JahfanLanguage.select(it, "ar") } }, checked = io.github.artcraftandroid.pdfcraft.JahfanLanguage.current == "ar")
    SheetRow(null, "English", { activity?.let { io.github.artcraftandroid.pdfcraft.JahfanLanguage.select(it, "en") } }, checked = io.github.artcraftandroid.pdfcraft.JahfanLanguage.current == "en")
    SheetDivider()
    Section(jText("Notifications", "الإشعارات"))
    Text(jText("Jahfan doesn't send notifications.", "لا يرسل جحفان إشعارات."), color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
    SheetDivider()
    Section(jText("Auto-suggestions", "الاقتراحات التلقائية"))
    SheetRow(null, jText("Offer words you used before when filling in forms", "اقتراح كلمات استخدمتها سابقًا عند تعبئة النماذج"), { vm.updateFormSuggestions(!vm.formSuggestions) }) {
        PdfSwitch(vm.formSuggestions, { vm.updateFormSuggestions(it) })
    }
    SheetRow(null, jText("Clear suggestions", "مسح الاقتراحات"), { vm.clearFormSuggestions() })
    SheetDivider()
    Section(jText("Your name on comments", "اسمك في التعليقات"))
    SheetRow(null, vm.toolPrefs.author.ifBlank { jText("Tap to enter your name, to show on your comments", "اضغط لإدخال اسمك ليظهر في تعليقاتك") }, { authorDialog = true })
    SheetDivider()
    Section(jText("Screen setting", "إعداد الشاشة"))
    SheetRow(null, jText("Keep the screen on", "إبقاء الشاشة مضاءة"), { vm.updateKeepScreenOn(!vm.keepScreenOn) }) {
        PdfSwitch(vm.keepScreenOn, { vm.updateKeepScreenOn(it) })
    }
    Text(jText("Keeps the screen on while you read, for up to 30 minutes without a touch.", "تبقى الشاشة مضاءة أثناء القراءة لمدة تصل إلى ٣٠ دقيقة دون لمسها."), color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
    SheetDivider()
    Section(jText("Keyboard", "لوحة المفاتيح"))
    SheetRow(null, jText("Keyboard shortcuts", "اختصارات لوحة المفاتيح"), { vm.updateKeyboardShortcuts(!vm.keyboardShortcuts) }) {
        PdfSwitch(vm.keyboardShortcuts, { vm.updateKeyboardShortcuts(it) })
    }
    Text(
        jText("With a keyboard connected: N or Page Down for the next page, P or Page Up for the previous one, the arrow keys to scroll, Ctrl+P to print, Ctrl and + or − to zoom.", "عند توصيل لوحة مفاتيح: N أو Page Down للصفحة التالية، وP أو Page Up للسابقة، والأسهم للتمرير، وCtrl+P للطباعة، وCtrl مع + أو − للتكبير والتصغير."),
        color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp),
    )
    Spacer(Modifier.height(24.dp))
    if (authorDialog) {
        var name by remember { mutableStateOf(vm.toolPrefs.author) }
        io.github.artcraftandroid.pdfcraft.ui.PdfDialog(
            onDismiss = { authorDialog = false }, title = jText("Your name on comments", "اسمك في التعليقات"),
            confirm = jText("OK", "موافق"), onConfirm = { vm.toolPrefs.author = name.trim(); authorDialog = false; vm.prefsChanged++ },
        ) { io.github.artcraftandroid.pdfcraft.ui.PdfField(name, { name = it }, jText("Your name", "اسمك")) }
    }
}

/** PdfCraft for Android's own repository: its help, its issues and its source. */
private const val APP_REPO = "https://github.com/hussain-hz/jahfan-android"

@Composable
private fun Section(title: String) {
    Text(title, color = pdfColors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun About(vm: AppViewModel) {
    val c = pdfColors
    val clipboard = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    Section(jText("About", "حول التطبيق"))
    val version = jText("Version ${vm.appVersion} (engine ${io.github.artcraftandroid.pdfcraft.core.engineVersion()})", "الإصدار ${vm.appVersion} (المحرك ${io.github.artcraftandroid.pdfcraft.core.engineVersion()})")
    Text(
        version, color = c.text, fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth()
            .pointerInput(version) { detectTapGestures(onLongPress = { clipboard.setText(AnnotatedString(version)); vm.tell(jText("Version copied", "تم نسخ الإصدار")) }) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
    SheetDivider()
    Section(jText("Legal", "معلومات قانونية"))
    SheetRow(null, jText("Open-source licences", "تراخيص المصادر المفتوحة"), { vm.showLicences = true })
    SheetRow(null, jText("Source code", "الشيفرة المصدرية"), { uri.openUri(APP_REPO) })
    SheetRow(null, jText("PdfCraft engine", "محرك PdfCraft"), { uri.openUri("https://github.com/storytold/pdfcraft") })
    Text(
        jText("PdfCraft for Android is built on PdfCraft's Rust engine. MIT OR Apache-2.0. PdfCraft is not affiliated with Adobe; Acrobat is a trademark of Adobe Inc.", "يعتمد PdfCraft لنظام Android على محرك PdfCraft المكتوب بلغة Rust، بترخيص MIT أو Apache-2.0. لا يتبع PdfCraft شركة Adobe؛ وAcrobat علامة تجارية لشركة Adobe Inc."),
        color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(20.dp), overflow = TextOverflow.Visible,
    )
}

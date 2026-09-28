package com.novashield.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.security.MessageDigest
import java.util.Locale

data class Threat(val name: String, val reason: String, val severity: String)
data class ScanResult(val name: String, val risk: Int, val reason: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NovaShieldApp(this) }
    }
}

@Composable
fun NovaShieldApp(context: Context) {
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var results by remember { mutableStateOf(listOf<ScanResult>()) }
    var urlText by remember { mutableStateOf("") }
    var urlRisk by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = scanUri(context, uri)
            results = listOf(result) + results
        }
    }

    fun startScan() {
        scanning = true
        progress = 0f
        results = emptyList()
    }

    LaunchedEffect(scanning) {
        if (scanning) {
            repeat(20) {
                kotlinx.coroutines.delay(80)
                progress = (it + 1) / 20f
            }
            results = installedAppScan(context)
            scanning = false
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF07090D),
            surface = Color(0xFF11151D),
            primary = Color(0xFF7DD3FC),
            secondary = Color(0xFF86EFAC)
        )
    ) {
        Scaffold(
            containerColor = Color(0xFF07090D),
            bottomBar = { BottomBar() }
        ) { pad ->
            LazyColumn(
                modifier = Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Text("NOVA", fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("SHIELD", fontSize = 13.sp, letterSpacing = 5.sp, color = Color.LightGray)
                }
                item {
                    SecurityCard(scanning, progress, results.isEmpty())
                }
                item {
                    ScanButton(scanning) { startScan() }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionCard("Scan File", Icons.Default.Description, Modifier.weight(1f)) {
                            picker.launch(arrayOf("*/*"))
                        }
                        ActionCard("Apps", Icons.Default.Apps, Modifier.weight(1f)) {
                            val i = Intent(Settings.ACTION_APPLICATION_SETTINGS)
                            context.startActivity(i)
                        }
                    }
                }
                item {
                    Text("LINK SECURITY", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Paste a link to check") },
                        trailingIcon = {
                            IconButton(onClick = { urlRisk = checkUrl(urlText) }) {
                                Icon(Icons.Default.Search, null)
                            }
                        },
                        shape = RoundedCornerShape(18.dp)
                    )
                    if (urlRisk != null) {
                        RiskCard(urlRisk!!)
                    }
                }
                if (results.isNotEmpty()) {
                    item {
                        Text("SCAN RESULTS", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    items(results) { r -> ResultCard(r) }
                }
                item {
                    Text(
                        "NOVA Shield uses local heuristics in this starter build. It does not claim to detect every malware family. User confirmation is required before destructive actions.",
                        fontSize = 12.sp, color = Color.Gray
                    )
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun SecurityCard(scanning: Boolean, progress: Float, clean: Boolean) {
    val infinite = rememberInfiniteTransition(label = "spin")
    val rotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "rotation"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11151D))
    ) {
        Box(
            Modifier.fillMaxWidth().height(245.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(150.dp).rotate(if (scanning) rotation else 0f)
                    .background(
                        Brush.sweepGradient(
                            listOf(Color(0xFF7DD3FC), Color.Transparent, Color(0xFF86EFAC))
                        ), CircleShape
                    )
            )
            Box(
                Modifier.size(132.dp).background(Color(0xFF11151D), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (scanning) "${(progress * 100).toInt()}%" else "SECURE",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(if (scanning) "Scanning…" else "Protection ready",
                        fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ScanButton(scanning: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !scanning,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(Icons.Default.Shield, null)
        Spacer(Modifier.width(8.dp))
        Text(if (scanning) "SCANNING…" else "START QUICK SCAN",
            fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(120.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11151D))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = Color(0xFF7DD3FC))
            Text(title, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ResultCard(r: ScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11151D))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (r.risk >= 70) Icons.Default.Warning else Icons.Default.CheckCircle,
                null,
                tint = if (r.risk >= 70) Color(0xFFFF8A80) else Color(0xFF86EFAC)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, fontWeight = FontWeight.Bold)
                Text(r.reason, fontSize = 12.sp, color = Color.Gray)
            }
            Text("${r.risk}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RiskCard(text: String) {
    Card(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11151D))
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Link, null)
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 13.sp)
        }
    }
}

@Composable
fun BottomBar() {
    NavigationBar(containerColor = Color(0xFF0B0E13)) {
        NavigationBarItem(true, {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
        NavigationBarItem(false, {}, icon = { Icon(Icons.Default.Security, null) }, label = { Text("Security") })
        NavigationBarItem(false, {}, icon = { Icon(Icons.Default.History, null) }, label = { Text("History") })
        NavigationBarItem(false, {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
    }
}

fun installedAppScan(context: Context): List<ScanResult> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return apps.map { app ->
        val label = pm.getApplicationLabel(app).toString()
        val suspicious = label.contains("hack", true) ||
                label.contains("crack", true) ||
                label.contains("mod", true) ||
                label.contains("cheat", true)
        ScanResult(
            label,
            if (suspicious) 65 else 0,
            if (suspicious) "Name matches a suspicious keyword; manual review recommended."
            else "No local heuristic warning."
        )
    }.filter { it.risk > 0 }.take(20)
}

fun scanUri(context: Context, uri: Uri): ScanResult {
    val name = queryDisplayName(context, uri)
    val lower = name.lowercase(Locale.US)
    val riskyExt = listOf(".apk", ".xapk", ".apks", ".dex", ".jar")
    val suspicious = riskyExt.any { lower.endsWith(it) } ||
            listOf("crack", "hack", "keygen", "payload", "stealer").any { lower.contains(it) }
    return ScanResult(
        name,
        if (suspicious) 70 else 0,
        if (suspicious) "Potentially risky file type/name. Do not open unless you trust the source."
        else "No local filename/type warning."
    )
}

fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use {
        if (it.moveToFirst()) return it.getString(0)
    }
    return uri.lastPathSegment ?: "Selected file"
}

fun checkUrl(input: String): String {
    val value = input.trim()
    if (value.isBlank()) return "Enter a URL first."
    val lower = value.lowercase(Locale.US)
    if (!lower.startsWith("https://") && !lower.startsWith("http://"))
        return "UNKNOWN RISK — URL scheme is unusual. Treat it carefully."
    val redFlags = listOf(
        "bit.ly", "tinyurl.com", "t.co", "is.gd", "shorturl.at",
        "login-", "verify-", "secure-", "account-", "password-"
    )
    return if (redFlags.any { lower.contains(it) })
        "CAUTION — suspicious URL pattern detected. Do not enter passwords or payment details."
    else
        "NO LOCAL WARNING — this is not proof that the website is safe."
}

fun sha256(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}

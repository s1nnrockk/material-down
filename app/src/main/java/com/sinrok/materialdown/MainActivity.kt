package com.sinrok.materialdown

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Swatch(val name: String, val value: Int)

private val swatches = listOf(
    Swatch("Графит", 0xFF252B35.toInt()),
    Swatch("Синий", 0xFF315DA8.toInt()),
    Swatch("Мятный", 0xFF267D70.toInt()),
    Swatch("Песочный", 0xFF927044.toInt()),
    Swatch("Белый", 0xFFF2F3F5.toInt()),
    Swatch("Чёрный", 0xFF111318.toInt())
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialDownApp() }
    }
}

@Composable
private fun MaterialDownApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(StylePrefs.FILE, 0) }
    var style by remember { mutableStateOf(prefs.getString(StylePrefs.STYLE, StylePrefs.MATERIAL) ?: StylePrefs.MATERIAL) }
    var background by remember { mutableIntStateOf(prefs.getInt(StylePrefs.BACKGROUND, 0xFF252B35.toInt())) }
    var indicator by remember { mutableIntStateOf(prefs.getInt(StylePrefs.INDICATOR, 0xFFF1F3FA.toInt())) }
    var width by remember { mutableFloatStateOf(prefs.getInt(StylePrefs.WIDTH, 144).toFloat()) }
    var pngUri by remember { mutableStateOf(prefs.getString(StylePrefs.PNG_URI, null)) }

    val pngPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            pngUri = uri.toString()
            prefs.edit().putString(StylePrefs.PNG_URI, pngUri).apply()
        }
    }

    fun saveStyle(value: String) {
        style = value
        prefs.edit().putString(StylePrefs.STYLE, value).apply()
    }
    fun saveBackground(value: Int) {
        background = value
        prefs.edit().putInt(StylePrefs.BACKGROUND, value).apply()
    }
    fun saveIndicator(value: Int) {
        indicator = value
        prefs.edit().putInt(StylePrefs.INDICATOR, value).apply()
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF315DA8),
        onPrimary = Color.White,
        secondary = Color(0xFF52647E),
        surface = Color(0xFFFFFFFF),
        background = Color(0xFFF4F6FA)
    )) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("Material Down", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Настрой жестовую полоску под себя.", color = MaterialTheme.colorScheme.onSurfaceVariant)

                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8ECF3))) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Предпросмотр", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4D596B))
                        Box(
                            Modifier.width(width.dp.coerceIn(100.dp, 220.dp)).height(42.dp)
                                .background(
                                    when (style) {
                                        StylePrefs.IOS -> Color(0xFF17191D)
                                        StylePrefs.CUSTOM -> Color(background)
                                        else -> Color(background)
                                    },
                                    RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (style == StylePrefs.CUSTOM && pngUri != null) {
                                Text("PNG", color = Color(indicator), style = MaterialTheme.typography.labelSmall)
                            } else {
                                Box(
                                    Modifier.width(48.dp).height(4.dp)
                                        .background(if (style == StylePrefs.IOS) Color.White else Color(indicator), RoundedCornerShape(50))
                                )
                            }
                        }
                        Text(
                            when (style) {
                                StylePrefs.IOS -> "iOS Style"
                                StylePrefs.CUSTOM -> "Свой стиль"
                                else -> "Material 3"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Text("Стиль полоски", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                StyleChoice("Material 3", "Мягкая поверхность и выразительный индикатор", style == StylePrefs.MATERIAL) { saveStyle(StylePrefs.MATERIAL) }
                StyleChoice("iOS Style", "Минималистичная тёмная капсула", style == StylePrefs.IOS) { saveStyle(StylePrefs.IOS) }
                StyleChoice("Свой стиль", "Собери собственный вариант или загрузи PNG", style == StylePrefs.CUSTOM) { saveStyle(StylePrefs.CUSTOM) }

                if (style == StylePrefs.CUSTOM) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Фон полоски", style = MaterialTheme.typography.titleMedium)
                            SwatchRow(background, ::saveBackground)
                            HorizontalDivider()
                            Text("Цвет индикатора", style = MaterialTheme.typography.titleMedium)
                            SwatchRow(indicator, ::saveIndicator)
                            HorizontalDivider()
                            Text("Ширина: ${width.toInt()} dp", style = MaterialTheme.typography.titleMedium)
                            Slider(value = width, onValueChange = {
                                width = it
                                prefs.edit().putInt(StylePrefs.WIDTH, it.toInt()).apply()
                            }, valueRange = 100f..220f)
                            Button(onClick = { pngPicker.launch(arrayOf("image/png")) }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (pngUri == null) "Выбрать PNG" else "Заменить PNG")
                            }
                            if (pngUri != null) {
                                Text("Свой PNG выбран. Он будет использоваться вместо стандартного индикатора.", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = {
                                    pngUri = null
                                    prefs.edit().remove(StylePrefs.PNG_URI).apply()
                                }) { Text("Убрать PNG") }
                            } else {
                                Text("Можно выбрать прозрачный PNG с изображением своей полоски или индикатора.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Настроить доступ к полоске")
                }
                Text("После выбора стиль сохраняется автоматически. Если полоска уже включена, изменения применятся сразу.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StyleChoice(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFE1EAFB) else Color.White),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF315DA8)) else null,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun SwatchRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        swatches.forEach { swatch ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = { onSelect(swatch.value) },
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(50),
                    color = Color(swatch.value),
                    border = if (selected == swatch.value) androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF315DA8)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0x22000000))
                ) {}
                Text(swatch.name, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

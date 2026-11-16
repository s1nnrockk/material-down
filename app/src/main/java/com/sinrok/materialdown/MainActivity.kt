package com.sinrok.materialdown

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

private data class Swatch(val name: String, val value: Int)
private val swatches = listOf(
    Swatch("Океан", 0xFF315DA8.toInt()), Swatch("Мята", 0xFF267D70.toInt()),
    Swatch("Лаванда", 0xFF7565A8.toInt()), Swatch("Корал", 0xFFC16C5B.toInt()),
    Swatch("Песок", 0xFF927044.toInt()), Swatch("Графит", 0xFF252B35.toInt()),
    Swatch("Белый", 0xFFF2F3F5.toInt()), Swatch("Чёрный", 0xFF111318.toInt())
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
    var height by remember { mutableFloatStateOf(prefs.getInt(StylePrefs.HEIGHT, 42).toFloat()) }
    var radius by remember { mutableFloatStateOf(prefs.getInt(StylePrefs.RADIUS, 32).toFloat()) }
    var opacity by remember { mutableFloatStateOf(prefs.getInt(StylePrefs.OPACITY, 94).toFloat()) }
    var glow by remember { mutableStateOf(prefs.getBoolean(StylePrefs.GLOW, true)) }
    var haptics by remember { mutableStateOf(prefs.getBoolean(StylePrefs.HAPTICS, true)) }
    var reducedMotion by remember { mutableStateOf(prefs.getBoolean(StylePrefs.REDUCED_MOTION, false)) }
    var animation by remember { mutableStateOf(prefs.getString(StylePrefs.ANIMATION, "spring") ?: "spring") }
    var sensitivity by remember { mutableFloatStateOf(prefs.getInt(StylePrefs.SENSITIVITY, 30).toFloat()) }
    var holdDuration by remember { mutableFloatStateOf(prefs.getInt(StylePrefs.HOLD_DURATION, 420).toFloat()) }
    var pngUri by remember { mutableStateOf(prefs.getString(StylePrefs.PNG_URI, null)) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    fun putBool(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun saveStyle(value: String) { style = value; prefs.edit().putString(StylePrefs.STYLE, value).apply() }
    fun saveBackground(value: Int) { background = value; putInt(StylePrefs.BACKGROUND, value) }
    fun saveIndicator(value: Int) { indicator = value; putInt(StylePrefs.INDICATOR, value) }

    val pngPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            pngUri = uri.toString()
            prefs.edit().putString(StylePrefs.PNG_URI, pngUri).apply()
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Пустой файл")
                val obj = JSONObject(json)
                val e = prefs.edit()
                if (obj.has("style")) { style = obj.getString("style"); e.putString(StylePrefs.STYLE, style) }
                if (obj.has("background")) { background = obj.getInt("background"); e.putInt(StylePrefs.BACKGROUND, background) }
                if (obj.has("indicator")) { indicator = obj.getInt("indicator"); e.putInt(StylePrefs.INDICATOR, indicator) }
                listOf(StylePrefs.WIDTH, StylePrefs.HEIGHT, StylePrefs.RADIUS, StylePrefs.OPACITY, StylePrefs.SENSITIVITY, StylePrefs.HOLD_DURATION).forEach { key ->
                    if (obj.has(key)) { val v = obj.getInt(key); when(key) {
                        StylePrefs.WIDTH -> width = v.toFloat()
                        StylePrefs.HEIGHT -> height = v.toFloat()
                        StylePrefs.RADIUS -> radius = v.toFloat()
                        StylePrefs.OPACITY -> opacity = v.toFloat()
                        StylePrefs.SENSITIVITY -> sensitivity = v.toFloat()
                        StylePrefs.HOLD_DURATION -> holdDuration = v.toFloat()
                    }; e.putInt(key, v) }
                }
                listOf(StylePrefs.GLOW, StylePrefs.HAPTICS, StylePrefs.REDUCED_MOTION).forEach { key ->
                    if (obj.has(key)) { val v = obj.getBoolean(key); when(key) {
                        StylePrefs.GLOW -> glow = v
                        StylePrefs.HAPTICS -> haptics = v
                        StylePrefs.REDUCED_MOTION -> reducedMotion = v
                    }; e.putBoolean(key, v) }
                }
                if (obj.has("animation")) { animation = obj.getString("animation"); e.putString(StylePrefs.ANIMATION, animation) }
                e.apply()
                notice = "Тема импортирована"
            } catch (_: Exception) { notice = "Не удалось прочитать файл темы" }
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val obj = JSONObject().apply {
                    put("style", style); put("background", background); put("indicator", indicator)
                    put(StylePrefs.WIDTH, width.toInt()); put(StylePrefs.HEIGHT, height.toInt())
                    put(StylePrefs.RADIUS, radius.toInt()); put(StylePrefs.OPACITY, opacity.toInt())
                    put(StylePrefs.GLOW, glow); put(StylePrefs.HAPTICS, haptics)
                    put(StylePrefs.REDUCED_MOTION, reducedMotion); put("animation", animation)
                    put(StylePrefs.SENSITIVITY, sensitivity.toInt()); put(StylePrefs.HOLD_DURATION, holdDuration.toInt())
                }
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(obj.toString(2)) }
                notice = "Тема экспортирована"
            } catch (_: Exception) { notice = "Не удалось сохранить тему" }
        }
    }

    val accent = Color(background)
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF315DA8), secondary = Color(0xFF52647E), surface = Color.White, background = Color(0xFFF3F5F9))) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Material Down", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Жесты. Внешний вид. Твой стиль.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = Color(0xFFE3EAF7), shape = RoundedCornerShape(14.dp)) {
                        Text("1.1", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF315DA8), fontWeight = FontWeight.Bold)
                    }
                }

                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6EAF1))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ЖИВОЙ ПРЕДПРОСМОТР", style = MaterialTheme.typography.labelMedium, color = Color(0xFF526078), fontWeight = FontWeight.Bold)
                        Box(Modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.width(width.dp.coerceIn(100.dp, 240.dp)).height(height.dp.coerceIn(24.dp, 64.dp))
                                .clip(RoundedCornerShape(radius.dp.coerceIn(8.dp, 32.dp)))
                                .background(if (style == StylePrefs.IOS) Color(0xFF17191D) else Color(background).copy(alpha = opacity / 100f)),
                                contentAlignment = Alignment.Center) {
                                if (glow) Box(Modifier.width(64.dp).height(12.dp).clip(RoundedCornerShape(50)).background(Color(indicator).copy(alpha = 0.18f)))
                                Box(Modifier.width(48.dp).height(4.dp).clip(RoundedCornerShape(50)).background(if (style == StylePrefs.IOS) Color.White else Color(indicator)))
                            }
                        }
                        Text(when(style) { StylePrefs.IOS -> "iOS Style"; StylePrefs.CUSTOM -> "Custom"; StylePrefs.GLASS -> "Liquid Glass"; else -> "Material 3" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                SectionTitle("Внешний вид")
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    StyleCard("Material 3", "Чистый Android-стиль", style == StylePrefs.MATERIAL) { saveStyle(StylePrefs.MATERIAL) }
                    StyleCard("iOS Style", "Минималистичная тёмная капсула", style == StylePrefs.IOS) { saveStyle(StylePrefs.IOS) }
                    StyleCard("Liquid Glass", "Полупрозрачная поверхность с бликом", style == StylePrefs.GLASS) { saveStyle(StylePrefs.GLASS) }
                    StyleCard("Свой стиль", "Полный контроль над оформлением", style == StylePrefs.CUSTOM) { saveStyle(StylePrefs.CUSTOM) }
                }

                SectionTitle("Готовые темы")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    listOf(Triple("Ocean", 0xFF315DA8.toInt(), 0xFFE8F0FF.toInt()), Triple("Mint", 0xFF267D70.toInt(), 0xFFDDF4ED.toInt()), Triple("Sand", 0xFF927044.toInt(), 0xFFF4E9D6.toInt()), Triple("Coral", 0xFFC16C5B.toInt(), 0xFFFFE5DF.toInt()), Triple("Graphite", 0xFF252B35.toInt(), 0xFFE2E5EA.toInt())).forEach { preset ->
                        Card(onClick = { saveStyle(StylePrefs.CUSTOM); saveBackground(preset.second); saveIndicator(preset.third) }, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Box(Modifier.size(width = 58.dp, height = 24.dp).clip(RoundedCornerShape(50)).background(Color(preset.second)), contentAlignment = Alignment.Center) {
                                    Box(Modifier.width(22.dp).height(3.dp).clip(RoundedCornerShape(50)).background(Color(preset.third)))
                                }
                                Text(preset.first, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                SectionTitle("Настройка формы")
                SettingSlider("Ширина", width, 100f..240f, "dp") { width = it; putInt(StylePrefs.WIDTH, it.toInt()) }
                SettingSlider("Высота", height, 24f..64f, "dp") { height = it; putInt(StylePrefs.HEIGHT, it.toInt()) }
                SettingSlider("Скругление", radius, 8f..32f, "dp") { radius = it; putInt(StylePrefs.RADIUS, it.toInt()) }
                SettingSlider("Непрозрачность", opacity, 35f..100f, "%") { opacity = it; putInt(StylePrefs.OPACITY, it.toInt()) }

                SectionTitle("Цвета")
                Text("Фон полоски", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                SwatchRow(background, ::saveBackground)
                Text("Индикатор", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                SwatchRow(indicator, ::saveIndicator)
                OutlinedButton(onClick = { pngPicker.launch(arrayOf("image/png")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (pngUri == null) "Загрузить PNG" else "Заменить PNG")
                }
                if (pngUri != null) TextButton(onClick = { pngUri = null; prefs.edit().remove(StylePrefs.PNG_URI).apply() }) { Text("Убрать выбранный PNG") }

                SectionTitle("Эффекты и анимации")
                SettingSwitch("Glow", "Мягкое свечение при взаимодействии", glow) { glow = it; putBool(StylePrefs.GLOW, it) }
                SettingSwitch("Haptic feedback", "Виброотклик при жесте", haptics) { haptics = it; putBool(StylePrefs.HAPTICS, it) }
                SettingSwitch("Экономия анимаций", "Уменьшает движение и эффекты", reducedMotion) { reducedMotion = it; putBool(StylePrefs.REDUCED_MOTION, it) }
                Text("Стиль анимации", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("spring" to "Spring", "smooth" to "Smooth", "none" to "Off").forEachIndexed { index, item ->
                        SegmentedButton(selected = animation == item.first, onClick = { animation = item.first; prefs.edit().putString(StylePrefs.ANIMATION, item.first).apply() }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(item.second) }
                    }
                }

                SectionTitle("Жесты")
                SettingSlider("Чувствительность свайпа", sensitivity, 15f..80f, "dp") { sensitivity = it; putInt(StylePrefs.SENSITIVITY, it.toInt()) }
                SettingSlider("Задержка для «Недавних»", holdDuration, 250f..900f, "мс") { holdDuration = it; putInt(StylePrefs.HOLD_DURATION, it.toInt()) }
                Text("Свайп вверх — Домой. Свайп вверх с удержанием — Недавние. Назад по-прежнему обрабатывает системный жест Android.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SectionTitle("Темы и резервные копии")
                OutlinedButton(onClick = { exportPicker.launch("material-down-theme.json") }, modifier = Modifier.fillMaxWidth()) { Text("Экспортировать тему") }
                OutlinedButton(onClick = { importPicker.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Импортировать тему") }

                Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Включить / настроить Material Down")
                }
                Text("Настройки сохраняются автоматически. Для работы полоски включи службу Material Down в специальных возможностях Android.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable private fun SectionTitle(value: String) {
    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}
@Composable private fun StyleCard(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), border = if (selected) BorderStroke(1.5.dp, Color(0xFF315DA8)) else null,
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFE6EDFA) else Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected, onClick)
        }
    }
}
@Composable private fun SettingSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text("${value.toInt()} $unit", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
@Composable private fun SettingSwitch(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}
@Composable private fun SwatchRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { swatch ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(onClick = { onSelect(swatch.value) }, modifier = Modifier.size(38.dp), shape = RoundedCornerShape(50), color = Color(swatch.value),
                    border = if (selected == swatch.value) BorderStroke(3.dp, Color(0xFF315DA8)) else BorderStroke(1.dp, Color(0x22000000))) {}
                Text(swatch.name, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
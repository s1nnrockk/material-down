package com.sinrok.materialdown
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF315DA8), onPrimary = Color.White,
                secondary = Color(0xFF52647E), surface = Color(0xFFF7F8FC),
                background = Color(0xFFF7F8FC)
            )) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Text("Material Down", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Жестовая навигация с компактной горизонтальной полоской в стиле Material 3.",
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Как пользоваться", style = MaterialTheme.typography.titleMedium)
                                Text("↑ Быстрый свайп вверх — Домой")
                                Text("↑ Свайп вверх с удержанием — Недавние приложения")
                                Text("← / → Для возврата используйте системный жест от края экрана")
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Включить полоску")
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("В настройках найдите Material Down в списке сервисов специальных возможностей и включите его. Это нужно Android, чтобы показывать полоску поверх других приложений и выполнять системные действия.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

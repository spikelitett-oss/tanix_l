import com.tanix.launcher.ai.AstraDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AstraBanner(vm: com.tanix.launcher.HomeViewModel) {
    var showAstra by remember { mutableStateOf(false) }
    if (showAstra) AstraDialog(vm) { showAstra = false }

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.astra_logo), // Твоя картинка!
                contentDescription = "Astra",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.weight(0.3f).fillMaxHeight()
            )
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(0.7f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Спроси меня о чем угодно",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                FocusBox(Modifier.size(56.dp), CircleShape, 1.1f, onClick = { showAstra = true }) { f ->
                    Box(Modifier.fillMaxSize().background(
                        if (f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mic, "Ask Astra", tint = if (f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

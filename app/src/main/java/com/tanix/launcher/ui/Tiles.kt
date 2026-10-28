@Composable
fun TvBanner(vm: HomeViewModel, onGoTv: () -> Unit) {
    val s = vm.s
    if (s.lastTvUrl.isNotEmpty()) {
        // Баннер с последним каналом
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = s.lastTvLogo,
                    contentDescription = null,
                    modifier = Modifier.size(140.dp).clip(RoundedCornerShape(16.dp)).background(if (s.tvWhiteLogoBg) Color.White else Color.Transparent)
                )
                Spacer(Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Продолжить просмотр",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = s.lastTvName,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    FocusBox(Modifier.height(48.dp), CircleShape, 1.05f, onClick = onGoTv) { f ->
                        Box(
                            Modifier.fillMaxHeight().background(
                                if (f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                            ).padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Смотреть", color = if (f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    } else {
        // Если каналов еще не было
        Box(
            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            FocusBox(Modifier, CircleShape, 1.05f, onClick = onGoTv) { f ->
                Text("Перейти в раздел ТВ", color = if (f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

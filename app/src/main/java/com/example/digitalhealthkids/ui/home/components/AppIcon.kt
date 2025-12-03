package com.example.digitalhealthkids.ui.home.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Ikonu her recomposition'da tekrar çekmemesi için remember kullanıyoruz
    val icon: Drawable? = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            // Bulamazsa null dönsün, aşağıda default gösteririz
            context.packageManager.getDefaultActivityIcon()
        }
    }

    Image(
        painter = rememberAsyncImagePainter(model = icon),
        contentDescription = "App Icon",
        modifier = modifier.size(40.dp)
    )
}
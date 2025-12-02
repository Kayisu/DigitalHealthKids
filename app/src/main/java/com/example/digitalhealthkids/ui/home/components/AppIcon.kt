package com.example.digitalhealthkids.ui.home.components

import androidx.compose.runtime.getValue
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Ikonu arka planda (IO) çekip UI'a getiren state
    val icon: Drawable? by produceState<Drawable?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                // Varsayılan Android ikonu (fallback)
                context.packageManager.getDefaultActivityIcon()
            }
        }
    }

    if (icon != null) {
        Image(
            painter = rememberAsyncImagePainter(icon),
            contentDescription = null,
            modifier = modifier.size(40.dp)
        )
    } else {
        // Yüklenirken boş kutu veya placeholder
    }
}
package com.almamun252.perfectroutine.view

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.almamun252.perfectroutine.data.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Validate/open Room before any screen starts observing its DAO flows.
@Composable
fun DatabaseStartupGate(database: AppDatabase, content: @Composable () -> Unit) {
    var ready by remember(database) { mutableStateOf(false) }
    var failed by remember(database) { mutableStateOf(false) }
    var attempt by remember(database) { mutableIntStateOf(0) }

    LaunchedEffect(database, attempt) {
        ready = false
        failed = false
        try {
            withContext(Dispatchers.IO) {
                database.openHelper.writableDatabase.query("SELECT 1").use { it.moveToFirst() }
            }
            ready = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("DatabaseStartup", "Database could not be opened; no reset attempted", error)
            failed = true
        }
    }

    if (ready) {
        content()
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
            if (failed) {
                Text("আপনার সংরক্ষিত তথ্য খোলা যাচ্ছে না।")
                Text("তথ্য মুছে ফেলা হয়নি। আবার চেষ্টা করুন। সমস্যা থাকলে সহায়তা নিন।")
                Button(onClick = { attempt++ }) { Text("আবার চেষ্টা করুন") }
            } else {
                CircularProgressIndicator()
                Text("আপনার কাজগুলো খুলছি…")
            }
        }
    }
}

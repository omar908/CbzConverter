package com.puchunguita.cbzconverter

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puchunguita.cbzconverter.ui.theme.CbzConverterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CbzConverterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = MainViewModel(application),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// TODO make UI/UX better, add progress text to know the conversion progress so user knows without logs.
// TODO add toast to show user that completion is complete and where to find it and the name of file.
@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            fileName = it.getFileName(context)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = fileName, modifier = Modifier.padding(bottom = 16.dp))

        Button(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
            Text(text = "Select CBZ File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { selectedFileUri?.let { viewModel.convertToEPUB(it) } },
            enabled = selectedFileUri != null
        ) {
            Text(text = "Convert to EPUB")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { selectedFileUri?.let { viewModel.convertToPDF(it) } },
            enabled = selectedFileUri != null
        ) {
            Text(text = "Convert to PDF")
        }
    }
}

fun Uri.getFileName(context: Context): String {
    var name = "Unknown"
    context.contentResolver.query(
        this,
        null,
        null,
        null,
        null
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        name = cursor.getString(nameIndex)
    }
    return name
}
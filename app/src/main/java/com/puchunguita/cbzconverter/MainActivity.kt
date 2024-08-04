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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
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
@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isCurrentlyConverting by viewModel.isCurrentlyConverting.collectAsState()
    val currentTaskStatus by viewModel.currentTaskStatus.collectAsState()
    val currentSubTaskStatus by viewModel.currentSubTaskStatus.collectAsState()
    val maxNumberOfPages by viewModel.maxNumberOfPages.collectAsState()

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
        Text(text = "CBZ Converter", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = fileName, modifier = Modifier.padding(bottom = 16.dp))

        Button(
            onClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            enabled = !isCurrentlyConverting
        ) {
            Text(text = "Select CBZ File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedFileUri?.let {
                    viewModel.convertToPDF(it)
                }
            },
            enabled = selectedFileUri != null && !isCurrentlyConverting
        ) {
            Text(text = "Convert to PDF")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Current Task Status:")
        Text(text = currentTaskStatus)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Current Sub-Task Status:")
        Text(text = currentSubTaskStatus)

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Max Number of Pages per PDF: $maxNumberOfPages")
        Text(text = "Value updates upon clicking Done (✓) on keyboard")
        val focusManager: FocusManager = LocalFocusManager.current

        var tempMaxNumberOfPages by remember { mutableStateOf(maxNumberOfPages.toString()) }

        TextField(
            value = tempMaxNumberOfPages,
            onValueChange = { tempMaxNumberOfPages = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = {
                viewModel.updateMaxNumberOfPagesSizeFromUserInput(tempMaxNumberOfPages)
                focusManager.clearFocus()
            }),
            label = { Text("Update Max Number of Pages per PDF") },
            enabled = !isCurrentlyConverting,
            singleLine = true
        )
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CbzConverterTheme {
        MainScreen(viewModel = MainViewModel(application = ComponentActivity().application))
    }
}
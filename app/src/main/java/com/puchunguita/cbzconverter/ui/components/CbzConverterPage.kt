package com.puchunguita.cbzconverter.ui.components

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puchunguita.cbzconverter.ContextHelper
import com.puchunguita.cbzconverter.MainViewModel
import com.puchunguita.cbzconverter.ui.theme.CbzConverterTheme

@Composable
fun CbzConverterPage(
    selectedFileName: String,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    filePickerLauncher: ManagedActivityResultLauncher<Array<String>, List<Uri>>,
    isCurrentlyConverting: Boolean,
    selectedFilesUri: List<Uri>,
    currentTaskStatus: String,
    currentSubTaskStatus: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        FileConversionSegment(
            selectedFileName,
            viewModel,
            activity,
            filePickerLauncher,
            isCurrentlyConverting,
            selectedFilesUri
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        TasksStatusSegment(currentTaskStatus, currentSubTaskStatus)

        Spacer(modifier = Modifier.height(16.dp))

    }
}

@Composable
private fun TasksStatusSegment(currentTaskStatus: String, currentSubTaskStatus: String) {
    Column(
        Modifier.height(220.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Current Task Status (Scrollable):", fontWeight = FontWeight.SemiBold)
        Column(
            Modifier.height(100.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val scroll = rememberScrollState(0)
            Text(text = currentTaskStatus, modifier = Modifier.verticalScroll(scroll))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Current Sub-Task Status (Scrollable):", fontWeight = FontWeight.SemiBold)
        Column(
            Modifier.height(100.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val scroll = rememberScrollState(0)
            Text(text = currentSubTaskStatus, modifier = Modifier.verticalScroll(scroll))
        }
    }
}

@Composable
private fun FileConversionSegment(
    selectedFileName: String,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    filePickerLauncher: ManagedActivityResultLauncher<Array<String>, List<Uri>>,
    isCurrentlyConverting: Boolean,
    selectedFilesUri: List<Uri>
) {
    Column(
        Modifier.height(230.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "File to Convert (Scrollable):", fontWeight = FontWeight.SemiBold)

        Column(
            Modifier.height(85.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val scroll = rememberScrollState(0)
            Text(text = selectedFileName, modifier = Modifier.verticalScroll(scroll))
        }
        Spacer(modifier = Modifier.height(16.dp))


        Button(
            onClick = {
                viewModel.checkPermissionAndSelectFileAction(activity, filePickerLauncher)
            },
            enabled = !isCurrentlyConverting
        ) {
            Text(text = "Select CBZ File")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedFilesUri.let {
                    viewModel.convertToPDF(it)
                }
            },
            enabled = selectedFilesUri.isNotEmpty() && !isCurrentlyConverting
        ) {
            Text(text = "Convert to PDF")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CbzConverterPagePreview() {
    CbzConverterTheme {
        CbzConverterPage(
            selectedFileName = "Sample File Name",
            viewModel = MainViewModel(contextHelper = ContextHelper(ComponentActivity())),
            activity = ComponentActivity(),
            filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uri: List<Uri> ->
                uri.let {
                }
            },
            isCurrentlyConverting = false,
            selectedFilesUri = listOf(),
            currentTaskStatus = "",
            currentSubTaskStatus = ""
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FileConversionSegmentPreview() {
    CbzConverterTheme {
        FileConversionSegment(
            selectedFileName = "Sample File Name",
            viewModel = MainViewModel(contextHelper = ContextHelper(ComponentActivity())),
            activity = ComponentActivity(),
            filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uri: List<Uri> ->
                uri.let {
                }
            },
            isCurrentlyConverting = false,
            selectedFilesUri = listOf()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TasksStatusSegmentPreview() {
    CbzConverterTheme {
        TasksStatusSegment(
            currentTaskStatus = "Current Task Status",
            currentSubTaskStatus = "Current Sub-Task Status"
        )
    }
}
package com.puchunguita.cbzconverter

import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puchunguita.cbzconverter.ui.components.ConfigurationPage
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
                        activity = this,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, activity: ComponentActivity, modifier: Modifier = Modifier) {
    val isCurrentlyConverting by viewModel.isCurrentlyConverting.collectAsState()
    val currentTaskStatus by viewModel.currentTaskStatus.collectAsState()
    val currentSubTaskStatus by viewModel.currentSubTaskStatus.collectAsState()
    val maxNumberOfPages by viewModel.maxNumberOfPages.collectAsState()
    val overrideSortOrderToUseOffset by viewModel.overrideSortOrderToUseOffset.collectAsState()
    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val selectedFilesUri by viewModel.selectedFileUri.collectAsState()
    val overrideFileName by viewModel.overrideFileName.collectAsState()
    val overrideOutputDirectoryUri by viewModel.overrideOutputDirectoryUri.collectAsState()

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        uris.let {
            viewModel.updateUpdateSelectedFileUriFromUserInput(it)
        }
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            viewModel.updateOverrideOutputPathFromUserInput(it)
        }
    }

    BottomSheetScaffold(
        sheetContent = {
            ConfigurationPage(
                maxNumberOfPages,
                viewModel,
                isCurrentlyConverting,
                overrideSortOrderToUseOffset,
                overrideFileName,
                selectedFilesUri,
                overrideOutputDirectoryUri,
                activity,
                directoryPickerLauncher
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    Modifier.height(250.dp),
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

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    Modifier.height(200.dp),
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

                    Text(text = "Current Sub-Task Status:", fontWeight = FontWeight.SemiBold)
                    Text(text = currentSubTaskStatus)
                }

                Spacer(modifier = Modifier.height(16.dp))

            }
        },
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar (title = { Text("CBZ Converter") })
        },
        sheetPeekHeight = 152.dp
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CbzConverterTheme {
        MainScreen(viewModel = MainViewModel(application = ComponentActivity().application), activity = ComponentActivity())
    }
}
package com.puchunguita.cbzconverter

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puchunguita.cbzconverter.ui.components.CbzConverterPage
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
                        viewModel = MainViewModel(ContextHelper(this)),
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
    val overrideMergeFiles by viewModel.overrideMergeFiles.collectAsState()
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
                overrideMergeFiles,
                overrideFileName,
                selectedFilesUri,
                overrideOutputDirectoryUri,
                activity,
                directoryPickerLauncher
            )
        },
        content = {
            CbzConverterPage(
                selectedFileName,
                viewModel,
                activity,
                filePickerLauncher,
                isCurrentlyConverting,
                selectedFilesUri,
                currentTaskStatus,
                currentSubTaskStatus
            )
        },
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar (title = { Text("CBZ Converter") })
        },
        sheetPeekHeight = 140.dp
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CbzConverterTheme {
        MainScreen(
            viewModel = MainViewModel(contextHelper = ContextHelper(ComponentActivity())),
            activity = ComponentActivity()
        )
    }
}
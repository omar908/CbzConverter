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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
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
    val selectedFileUri by viewModel.selectedFileUri.collectAsState()
    val overrideFileName by viewModel.overrideFileName.collectAsState()
    val overrideOutputDirectoryUri by viewModel.overrideOutputDirectoryUri.collectAsState()

    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            //TODO move file name updates within updateUpdateSelectedFileUriFromUserInput()
            viewModel.updateUpdateSelectedFileUriFromUserInput(it)
            viewModel.updateOriginalFileNameFromUserInput(it.getFileName(context))
            viewModel.updateOverrideFileNameFromUserInput(it.getFileName(context).replace(".cbz", ""))
        }
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            viewModel.updateOverrideOutputPathFromUserInput(it)
        }
    }

    BottomSheetScaffold(
        sheetContent = {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Configurations (Swipe Up) ")

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Max Number of Pages per PDF: $maxNumberOfPages")
                Spacer(modifier = Modifier.height(16.dp))
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
                    label = {
                        if (!maxNumberOfPages.toString().contentEquals(tempMaxNumberOfPages)) {
                            Text(
                                text = "Value not saved, click Done (✓) on keyboard",
                                color = Color.Red
                            )
                        } else {
                            Text("Update Max Number of Pages per PDF")
                        }
                    },
                    enabled = !isCurrentlyConverting,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Default sort order uses file name (ASC)\n" +
                            "Override Sort Order to Use Offset: $overrideSortOrderToUseOffset"
                )
                Checkbox(
                    checked = overrideSortOrderToUseOffset,
                    onCheckedChange = viewModel::toggleOverrideSortOrderToUseOffset
                )

                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Current File Name: $overrideFileName")
                Spacer(modifier = Modifier.height(16.dp))

                var tempFileNameOverride by remember { mutableStateOf(overrideFileName) }

                LaunchedEffect(overrideFileName) {
                    // Updates tempFileNameOverride in case new file is chosen
                    if (tempFileNameOverride != overrideFileName) {
                        tempFileNameOverride = overrideFileName
                    }
                }

                TextField(
                    value = tempFileNameOverride,
                    onValueChange = { tempFileNameOverride = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.updateOverrideFileNameFromUserInput(tempFileNameOverride)
                        focusManager.clearFocus()
                    }),
                    label = {
                        if (!overrideFileName.contentEquals(tempFileNameOverride)) {
                            Text(
                                text = "Value not saved, click Done (✓) on keyboard",
                                color = Color.Red
                            )
                        } else {
                            Text("Override default file name (Exclude Extension)")
                        }
                    },
                    enabled = !isCurrentlyConverting && selectedFileUri != null,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    if (overrideOutputDirectoryUri != null){
                        "Current Output File Path: $overrideOutputDirectoryUri"
                    } else {
                        "No override output directory selected"
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // todo look into why you cannot choose any directory, even though you have full access to storage
                        viewModel.checkPermissionAndSelectDirectoryAction(activity, directoryPickerLauncher)
                    },
                    enabled = !isCurrentlyConverting
                ) {
                    Text(text = "Select & Override Output File Path")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(48.dp))
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "File to Convert: $selectedFileName", modifier = Modifier.padding(bottom = 16.dp))

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

                Column(
                    Modifier.height(200.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Current Task Status (Scrollable):", fontWeight = FontWeight.SemiBold)
                    Column(
                        Modifier.height(85.dp),
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
        MainScreen(viewModel = MainViewModel(application = ComponentActivity().application), activity = ComponentActivity())
    }
}
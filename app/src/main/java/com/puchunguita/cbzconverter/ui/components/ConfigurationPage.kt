package com.puchunguita.cbzconverter.ui.components

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puchunguita.cbzconverter.ContextHelper
import com.puchunguita.cbzconverter.MainViewModel
import com.puchunguita.cbzconverter.ui.theme.CbzConverterTheme

@Composable
fun ConfigurationPage(
    maxNumberOfPages: Int,
    viewModel: MainViewModel,
    isCurrentlyConverting: Boolean,
    overrideSortOrderToUseOffset: Boolean,
    overrideMergeFiles: Boolean,
    overrideFileName: String,
    selectedFilesUri: List<Uri>,
    overrideOutputDirectoryUri: Uri?,
    activity: ComponentActivity,
    directoryPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Configurations (Swipe Up) ")

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        val focusManager: FocusManager = LocalFocusManager.current

        MaxNumberOfPagesConfigSegment(
            maxNumberOfPages,
            viewModel,
            focusManager,
            isCurrentlyConverting
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        SortOrderOverrideConfigSegment(overrideSortOrderToUseOffset, viewModel)

        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        MergeFilesOverrideConfigSegment(overrideMergeFiles, viewModel)

        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        FileNameOverrideConfigSegment(
            overrideFileName,
            viewModel,
            focusManager,
            isCurrentlyConverting,
            selectedFilesUri
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        OutputDirectoryOverrideConfigSegment(
            overrideOutputDirectoryUri,
            viewModel,
            activity,
            directoryPickerLauncher,
            isCurrentlyConverting
        )
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun OutputDirectoryOverrideConfigSegment(
    overrideOutputDirectoryUri: Uri?,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    directoryPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>,
    isCurrentlyConverting: Boolean
) {
    Text(
        if (overrideOutputDirectoryUri != null) {
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
}

@Composable
private fun FileNameOverrideConfigSegment(
    overrideFileName: String,
    viewModel: MainViewModel,
    focusManager: FocusManager,
    isCurrentlyConverting: Boolean,
    selectedFilesUri: List<Uri>
) {
    Text(text = "Current File Name: $overrideFileName")
    Spacer(modifier = Modifier.height(16.dp))

    var tempFileNameOverride by remember { mutableStateOf(MainViewModel.EMPTY_STRING) }

    TextField(
        value = tempFileNameOverride,
        onValueChange = { tempFileNameOverride = it },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        keyboardActions = KeyboardActions(onDone = {
            viewModel.updateOverrideFileNameFromUserInput(tempFileNameOverride)
            focusManager.clearFocus()
        }),
        label = {
            if (!overrideFileName.contentEquals(tempFileNameOverride) && tempFileNameOverride.isNotBlank()) {
                Text(
                    text = "Value not saved, click Done (✓) on keyboard",
                    color = Color.Red
                )
            } else {
                Text("Override default file name (Exclude Extension)")
            }
        },
        enabled = !isCurrentlyConverting && selectedFilesUri.isNotEmpty(),
        singleLine = true
    )
}

@Composable
private fun SortOrderOverrideConfigSegment(
    overrideSortOrderToUseOffset: Boolean,
    viewModel: MainViewModel
) {
    Text(
        text = "Default sort order uses file name (ASC)\n" +
                "Override Sort Order to Use Offset: $overrideSortOrderToUseOffset"
    )
    Checkbox(
        checked = overrideSortOrderToUseOffset,
        onCheckedChange = viewModel::toggleOverrideSortOrderToUseOffset
    )
}

@Composable
private fun MergeFilesOverrideConfigSegment(
    overrideMergeFiles: Boolean,
    viewModel: MainViewModel
) {
    Text(
        text = "Default Behavior:\nApplies logic to each individual CBZ file.\n" +
                "Merge Files Override, mergers all files into one file.\n" +
                "Then applies additional configuration to that one file.\n"+
                "Note: When using this option the first file selected will be used as filename,\n" +
                "Unless an override file name is provide.\n"+
                "Merge Files Override: $overrideMergeFiles"
    )
    Checkbox(
        checked = overrideMergeFiles,
        onCheckedChange = viewModel::toggleMergeFilesOverride
    )
}

@Composable
private fun MaxNumberOfPagesConfigSegment(
    maxNumberOfPages: Int,
    viewModel: MainViewModel,
    focusManager: FocusManager,
    isCurrentlyConverting: Boolean
) {
    Text(text = "Max Number of Pages per PDF: $maxNumberOfPages")
    Spacer(modifier = Modifier.height(16.dp))
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
}

@Preview(showBackground = true)
@Composable
fun ConfigurationPagePreview() {
    CbzConverterTheme {
        ConfigurationPage(
            maxNumberOfPages = 100,
            viewModel = MainViewModel(contextHelper = ContextHelper(ComponentActivity())),
            isCurrentlyConverting = false,
            overrideSortOrderToUseOffset = false,
            overrideMergeFiles = false,
            overrideFileName = "test",
            selectedFilesUri = listOf(),
            overrideOutputDirectoryUri = null,
            activity = ComponentActivity(),
            directoryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
                uri?.let {
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FileNameOverrideConfigSegmentPreview() {
    CbzConverterTheme {
        FileNameOverrideConfigSegment(
            overrideFileName = "test",
            viewModel = MainViewModel(contextHelper = ContextHelper(ComponentActivity())),
            focusManager = LocalFocusManager.current,
            isCurrentlyConverting = false,
            selectedFilesUri = listOf()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SortOrderOverrideConfigSegmentPreview() {
    CbzConverterTheme {
        SortOrderOverrideConfigSegment(
            overrideSortOrderToUseOffset = false,
            viewModel = MainViewModel(contextHelper = ContextHelper(ComponentActivity())),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MaxNumberOfPagesConfigSegmentPreview() {
    CbzConverterTheme {
        MaxNumberOfPagesConfigSegment(
            maxNumberOfPages = 100,
            viewModel = MainViewModel(contextHelper = ContextHelper(ComponentActivity())),
            focusManager = LocalFocusManager.current,
            isCurrentlyConverting = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OutputDirectoryOverrideConfigSegmentPreview() {
    CbzConverterTheme {
        OutputDirectoryOverrideConfigSegment(
            overrideOutputDirectoryUri = null,
            viewModel = MainViewModel(contextHelper = ContextHelper(ComponentActivity())),
            activity = ComponentActivity(),
            directoryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
                uri?.let {
                }
            },
            isCurrentlyConverting = false
        )
    }
}
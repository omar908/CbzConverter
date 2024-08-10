package com.puchunguita.cbzconverter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsManager {

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001

        fun checkPermissionAndSelectFileAction(
            activity: ComponentActivity,
            filePickerLauncher: ManagedActivityResultLauncher<Array<String>, List<Uri>>
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    ContextCompat.startActivity(activity, intent, null)
                } else {
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
            } else {
                val writePermission = ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                val readPermission = ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )

                if (writePermission != PackageManager.PERMISSION_GRANTED ||
                    readPermission != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ),
                        STORAGE_PERMISSION_CODE
                    )
                } else {
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
            }
        }

        fun checkPermissionAndSelectDirectoryAction(
            activity: ComponentActivity,
            directoryPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    ContextCompat.startActivity(activity, intent, null)
                } else {
                    directoryPickerLauncher.launch(Uri.parse(Environment.getExternalStorageDirectory().toString()))
                }
            } else {
                val writePermission = ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                val readPermission = ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )

                if (writePermission != PackageManager.PERMISSION_GRANTED ||
                    readPermission != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ),
                        STORAGE_PERMISSION_CODE
                    )
                } else {
                    directoryPickerLauncher.launch(Uri.parse(Environment.getExternalStorageDirectory().toString()))
                }
            }
        }
    }

}
/*
 *    Copyright 2020, Petr Laštovička as Lasta apps, All rights reserved
 *
 *     This file is part of Bakalari extension.
 *
 *     Bakalari extension is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Bakalari extension is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Bakalari extension.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package cz.lastaapps.bakalariextension.ui.attachment

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.FileUtils
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.MyToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**Downloads attachment and checks if file can be downloaded in this location
 * different version for android 10+ and older versions*/
class AttachmentDownload {

    companion object {

        private val TAG = AttachmentDownload::class.java.simpleName
        const val ATTACHMENT_DOWNLOAD_CHANEL = "ATTACHMENT_DOWNLOAD_CHANEL"

        /**@return if file with name given exists*/
        fun exists(context: Context, fileName: String): Boolean {

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val sett = MySettings.withAppContext()
                val dirUri = Uri.parse(sett.getDownloadLocation())
                val pickedDir = DocumentFile.fromTreeUri(context, dirUri)

                if (pickedDir == null || !pickedDir.exists() || !pickedDir.canWrite()) {
                    Log.e(TAG, "Even directory does not exist")
                    false

                } else {

                    pickedDir.findFile(fileName) ?: return false
                    true
                }
            } else {

                val sett = MySettings.withAppContext()
                File(sett.getDownloadLocation(), fileName).exists()
            }.also {
                Log.i(TAG, "File exists: $it")
            }
        }

        /**@return if user has privileges to write to this file*/
        fun accessible(context: Context, fileName: String): Boolean {
            if (!exists(context, fileName))
                return true

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val sett = MySettings.withAppContext()
                val dirUri = Uri.parse(sett.getDownloadLocation())
                val pickedDir = DocumentFile.fromTreeUri(context, dirUri)

                if (pickedDir == null || !pickedDir.exists() || !pickedDir.canWrite()) {
                    Log.e(TAG, "Cannot access the whole directory")
                    false
                } else {

                    val file = pickedDir.findFile(fileName) ?: return true
                    file.canWrite()
                }

            } else {

                val sett = MySettings.withAppContext()
                File(sett.getDownloadLocation(), fileName).canWrite()
            }.also {
                Log.i(TAG, "File is accessible: $it")
            }
        }

        /**Downloads attachment with info given*/
        fun download(activity: Activity, fileName: String, mime: String, id: String) {
            //if storage is writable
            if (checkPermission(activity)) {

                CoroutineScope(Dispatchers.IO).launch {

                    //cannot download, no internet at the moment
                    if (!CheckInternet.check(false)) {
                        MyToast.makeText(
                            activity,
                            R.string.attachment_error_no_internet,
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    /*
                    * for Android Q+ file is downloaded into cache directory (source uri)
                    * and then moved into actual file (target uri)
                    * on older version source uri == target uri
                    */

                    //target file uri
                    val targetUri = getTargetUri(activity, fileName, mime)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        targetUri ?: return@launch
                    }

                    val sourceUri = getFileUri(activity, fileName) ?: return@launch

                    //sets up download request
                    val request =
                        DownloadManager.Request(Uri.parse("${ConnMgr.getAPIUrl()}/3/komens/attachment/${id}"))

                            .setTitle(fileName) // Title of the Download Notification
                            .setDescription(activity.getString(R.string.attachment_downloading_subtitle)) // Description of the Download Notification
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Visibility of the download Notification
                            .setDestinationUri(sourceUri)
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)
                            .addRequestHeader(
                                "Authorization",
                                "Bearer ${ConnMgr.getValidAccessToken()}"
                            )

                    //adds file to system media store (gallery for images)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                        request.allowScanningByMediaScanner()

                    //starts download
                    val downloadManager =
                        activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val downloadId = downloadManager.enqueue(request)

                    Log.i(TAG, "Starting $fileName download, id[$downloadId]")

                    withContext(Dispatchers.Main) {

                        //shows download started info
                        Toast.makeText(
                            App.context,
                            R.string.attachment_download_started,
                            Toast.LENGTH_LONG
                        ).show()

                        //observes for the end of download
                        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                        App.context.registerReceiver(
                            DownloadReceiver(
                                downloadId,
                                fileName,
                                mime,
                                sourceUri,
                                targetUri
                            ), filter
                        )
                    }
                }
            }
        }

        /**if app can use storage*/
        private fun checkPermission(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M until Build.VERSION_CODES.Q) {

                //needed only after permission check was added and before filesystem changed in Android 10
                return if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i(TAG, "Write storage permission granted")
                    true
                } else {
                    Log.i(TAG, "Write storage permission not granted")
                    Toast.makeText(activity, R.string.permission_required, Toast.LENGTH_LONG)
                        .show()
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
                    )
                    false
                }
            } else {
                Log.i(TAG, "Write storage permission not needed")
                return true
            }
        }

        /**@return the file uri to download file to*/
        private fun getFileUri(context: Context, fileName: String): Uri? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                File(context.externalCacheDir, fileName)
                    .toUri()

            } else {

                val sett = MySettings.withAppContext()
                File(sett.getDownloadLocation(), fileName)
                    .toUri()
            }
        }

        /**@return the uri of the final file*/
        fun getTargetUri(context: Context, fileName: String, mime: String): Uri? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val sett = MySettings.withAppContext()
                val dirUri = Uri.parse(sett.getDownloadLocation())
                val pickedDir = DocumentFile.fromTreeUri(context, dirUri)

                if (pickedDir == null || !pickedDir.exists() || !pickedDir.canWrite()) {
                    MyToast.makeText(context, R.string.attachment_choose_again, Toast.LENGTH_LONG)
                        .show()
                    Log.e(TAG, "Failed to obtain directory")
                    return null
                }

                val file = pickedDir.findFile(fileName) ?: pickedDir.createFile(
                    mime, fileName
                )

                if (file == null || !file.exists()) {
                    Log.e(TAG, "Failed to create file")
                    MyToast.makeText(context, R.string.attachment_choose_again, Toast.LENGTH_LONG)
                        .show()
                    return null
                }

                Log.i(TAG, "New file uri ${file.uri}")

                return file.uri
            } else {
                return getFileUri(context, fileName)
            }
        }

        /**executes action after download is finished*/
        private class DownloadReceiver(
            val downloadId: Long,
            val fileName: String,
            val mime: String,
            val sourceUri: Uri,
            val targetUri: Uri?
        ) : BroadcastReceiver() {

            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                val reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                if (downloadId == reference) {

                    Log.i(TAG, "Download finished [$downloadId]")

                    //removes itself from memory
                    context.unregisterReceiver(this)

                    /*val manager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    var uri = manager.getUriForDownloadedFile(downloadId)*/

                    //uri of (the cache file and then of) the final file
                    var uri = sourceUri

                    //moves cached file into final location
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (!moveToSharedStorage(context, uri, targetUri!!)) {
                            Toast.makeText(
                                context,
                                R.string.attachment_error_file_system,
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }
                        uri = targetUri
                    }

                    //probably useless
                    //notifyMediaService(context, uri, mime)

                    Log.i(TAG, "Uri: $uri")

                    //shows notification with opens downloaded file
                    val fileIntent = getIntent(uri, mime)

                    val notification = createNotification(context, fileIntent, fileName)

                    val mNotificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mNotificationManager.notify(fileName.hashCode(), notification)
                }
            }
        }

        /**moves file from cache*/
        @RequiresApi(Build.VERSION_CODES.Q)
        private fun moveToSharedStorage(context: Context, source: Uri, target: Uri): Boolean {

            var toReturn: Boolean
            try {
                val sourceFile = source.toFile()
                FileUtils.copy(
                    sourceFile.inputStream(),
                    context.contentResolver.openOutputStream(target)!!
                )

                toReturn = true
            } catch (e: java.lang.Exception) {
                e.printStackTrace()

                toReturn = false
            } finally {
                try {
                    //clears cache
                    source.toFile().delete()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }

            return toReturn
        }

        /**@return intent witch opens the file downloaded*/
        fun getIntent(uri: Uri, mime: String): Intent {

            // Open file with user selected app
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, mime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            return intent
        }

        /**creates notification with pending intent*/
        private fun createNotification(
            context: Context,
            intent: Intent,
            fileName: String
        ): Notification {
            val pendingIntent = PendingIntent.getActivity(
                context,
                fileName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            //generates Notification object
            val title = fileName
            val subtitle = context.getString(R.string.attachment_downloading_subtitle_done)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val builder = Notification.Builder(
                    context,
                    TTNotifyService.NOTIFICATION_CHANEL_ID
                )
                    .setContentTitle(title)
                    .setContentText(subtitle)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOnlyAlertOnce(true)
                builder.build()
            } else {
                val builder = NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(subtitle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.build()
            }
        }
    }
}
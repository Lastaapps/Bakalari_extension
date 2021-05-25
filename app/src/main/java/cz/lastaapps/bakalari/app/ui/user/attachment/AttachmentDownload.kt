/*
 *    Copyright 2021, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalari.app.ui.user.attachment

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import cz.lastaapps.bakalari.api.core.ConnMgr
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.start.login.impl.getChannelId
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.platform.CheckInternet
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import cz.lastaapps.bakalari.tools.MyToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream


/**Downloads attachment and checks if file can be downloaded in this location
 * different version for android 10+ and older versions*/
object AttachmentDownload {

    private val TAG = AttachmentDownload::class.java.simpleName
    const val ATTACHMENT_DOWNLOAD_CHANEL = "ATTACHMENT_DOWNLOAD_CHANEL"

    /**@return if file with name given exists*/
    fun exists(context: Context, fileName: String): Boolean {

        val sett = MySettings.withAppContext()
        val dirUri = Uri.parse(sett.getDownloadLocation())
        val pickedDir = DocumentFile.fromTreeUri(context, dirUri)

        return if (pickedDir == null || !pickedDir.exists() || !pickedDir.canWrite()) {
            Log.e(TAG, "Even directory does not exist")
            false

        } else {

            pickedDir.findFile(fileName) ?: return false
            true
        }.also {
            Log.i(TAG, "File exists: $it")
        }
    }

    /**@return if user has privileges to write to this file*/
    fun accessible(context: Context, fileName: String): Boolean {
        if (!exists(context, fileName))
            return true

        val sett = MySettings.withAppContext()
        val dirUri = Uri.parse(sett.getDownloadLocation())
        val pickedDir = DocumentFile.fromTreeUri(context, dirUri)

        return if (pickedDir == null || !pickedDir.exists() || !pickedDir.canWrite()) {
            Log.e(TAG, "Cannot access the whole directory")
            false
        } else {

            pickedDir.findFile(fileName)?.canWrite() ?: true
        }
    }

    /**Downloads attachment with info given*/
    fun download(
        account: BakalariAccount,
        activity: Activity,
        fileName: String,
        mime: String,
        id: String
    ) {
        //if storage is writable
        if (checkPermission(activity)) {

            val appContext = activity.applicationContext
            CoroutineScope(Dispatchers.IO).launch {

                //cannot download, no internet at the moment
                if (!CheckInternet.check(account.getAPIUrl(), false)) {
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
                val targetUri = getTargetUri(activity, fileName, mime) ?: return@launch

                val cacheUri = getCacheUri(activity, fileName) ?: return@launch

                //sets up download request
                val request =
                    DownloadManager.Request(Uri.parse("${account.getAPIUrl()}/3/komens/attachment/${id}"))

                        .setTitle(fileName) // Title of the Download Notification
                        .setDescription(activity.getString(R.string.attachment_downloading_subtitle)) // Description of the Download Notification
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Visibility of the download Notification
                        .setDestinationUri(cacheUri)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                        .addRequestHeader(
                            "Authorization",
                            "Bearer ${
                                ConnMgr.getValidAccessToken(
                                    activity.applicationContext,
                                    account
                                )
                            }"
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
                        appContext,
                        R.string.attachment_download_started,
                        Toast.LENGTH_LONG
                    ).show()

                    //observes for the end of download
                    val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                    appContext.registerReceiver(
                        DownloadReceiver(
                            account,
                            downloadId,
                            fileName,
                            mime,
                            cacheUri,
                            targetUri
                        ), filter
                    )
                }
            }
        }
    }

    /**if app can use storage*/
    @SuppressLint("NewApi")
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

                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    //asks for permission
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
                    )
                } else {
                    //asks user to grant permission in settings
                    AlertDialog.Builder(activity)
                        .setNegativeButton(R.string.permission_ignore) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setPositiveButton(R.string.permission_open_setting) { dialog, _ ->
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts(
                                "package",
                                activity.packageName,
                                null
                            )
                            intent.data = uri
                            activity.startActivity(intent)
                            dialog.dismiss()
                        }
                        .setMessage(R.string.permission_disabled)
                        .setCancelable(true)
                        .create()
                        .show()
                }

                false
            }
        } else {
            Log.i(TAG, "Write storage permission not needed")
            return true
        }
    }

    /**@return the file uri to download file to*/
    private fun getCacheUri(context: Context, fileName: String): Uri? {
        return File(context.externalCacheDir, fileName).toUri()
    }

    /**@return the uri of the final file*/
    fun getTargetUri(context: Context, fileName: String, mime: String): Uri? {
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

        return file.uri
    }


    /**executes action after download is finished*/
    private class DownloadReceiver(
        val account: BakalariAccount,
        val downloadId: Long,
        val fileName: String,
        val mime: String,
        val cacheUri: Uri,
        val targetUri: Uri?
    ) : BroadcastReceiver() {

        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (downloadId == reference) {

                Log.i(TAG, "Download finished [$downloadId]")

                //removes itself from memory
                context.unregisterReceiver(this)

                Log.i(TAG, "Cache uri: $cacheUri")
                Log.i(TAG, "Target uri: $targetUri")

                val action = intent.action
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {

                    val query = DownloadManager.Query()
                    query.setFilterById(reference)
                    val c: Cursor = downloadManager.query(query)

                    if (c.moveToFirst()) {
                        val columnIndex: Int = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        when (c.getInt(columnIndex)) {
                            DownloadManager.STATUS_SUCCESSFUL -> downlandSucceed(
                                context,
                                account,
                                cacheUri,
                                targetUri!!,
                                fileName,
                                mime
                            )
                            else -> downloadFailed(
                                context,
                                cacheUri,
                                targetUri!!,
                                fileName,
                                mime
                            )
                        }
                    }
                }
            }
        }
    }

    private fun downlandSucceed(
        context: Context,
        account: BakalariAccount,
        cacheUri: Uri,
        targetUri: Uri,
        fileName: String,
        mime: String
    ) {

        //moves cached file into final location
        if (!moveToSharedStorage(context, cacheUri, targetUri)) {
            Toast.makeText(
                context,
                R.string.attachment_error_file_system,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        //probably useless
        //notifyMediaService(context, uri, mime)

        onSuccessUI(context, account, targetUri, fileName, mime)
    }

    /**moves file from cache*/
    private fun moveToSharedStorage(context: Context, source: Uri, target: Uri): Boolean {

        var toReturn: Boolean
        try {
            context.contentResolver.apply {
                copyFile(openInputStream(source)!!, openOutputStream(target)!!)
            }

            toReturn = true
        } catch (e: Exception) {
            e.printStackTrace()

            try {
                context.contentResolver.delete(target, "", arrayOf<String>())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            toReturn = false
        } finally {
            try {
                //clears cache
                source.toFile().delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return toReturn
    }

    private fun copyFile(
        source: InputStream,
        dest: OutputStream
    ) {
        val buf = ByteArray(1024)
        var len = 0
        while (source.read(buf).also { len = it } > 0) {
            dest.write(buf, 0, len)
        }
        source.close()
        dest.close()
    }

    /**@return intent witch opens the file downloaded*/
    fun getIntent(uri: Uri, mime: String): Intent {

        // Open file with user selected app
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mime)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return intent
    }

    private fun onSuccessUI(
        context: Context,
        account: BakalariAccount,
        targetUri: Uri,
        fileName: String,
        mime: String
    ) {

        //shows notification with opens downloaded file
        val fileIntent = getIntent(targetUri, mime)

        val notification = createNotification(context, account, fileIntent, fileName)

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(getNotificationId(fileName), notification)

        context.sendBroadcast(Intent(MainActivity.ATTACHMENT_DOWNLOADED).apply {
            putExtra(MainActivity.ATTACHMENT_DOWNLOADED_FILENAME, fileName)
            putExtra(MainActivity.ATTACHMENT_DOWNLOADED_INTENT, fileIntent)
            putExtra(MainActivity.ATTACHMENT_DOWNLOADED_URI, targetUri)
        })
    }

    /**creates notification with pending intent*/
    private fun createNotification(
        context: Context,
        account: BakalariAccount,
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

        val builder = NotificationCompat.Builder(
            context,
            getChannelId(
                account.uuid,
                context.getString(R.string.channel_attachment_downloading_id)
            )
        )
            .setContentTitle(title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
        return builder.build()
    }

    private fun downloadFailed(
        context: Context,
        cacheUri: Uri,
        targetUri: Uri,
        fileName: String,
        mime: String
    ) {

        Log.e(TAG, "Download failed")

        deleteFiles(context, cacheUri, targetUri)

        val text =
            String.format(context.getString(R.string.attachment_download_failed), fileName)
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    /**deletes cache and target if it contains nothing*/
    @SuppressLint("Recycle")
    private fun deleteFiles(context: Context, cacheUri: Uri, targetUri: Uri) {
        //deletes cache
        cacheUri.toFile().deleteOnExit()

        //deletes the final file if there is nothing written in it (overwrite protection)
        var size = 0L

        context.contentResolver.query(
            targetUri, null, null, null, null
        )?.let {
            it.moveToFirst()
            size = it.getLong(it.getColumnIndex(OpenableColumns.SIZE))
            it.close()
        }

        if (size <= 0) {
            context.contentResolver.delete(targetUri, null, null)
        }
    }

    fun getNotificationId(fileName: String) = fileName.hashCode()
}
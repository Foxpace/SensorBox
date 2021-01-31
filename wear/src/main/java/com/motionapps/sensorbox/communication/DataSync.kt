package com.motionapps.sensorbox.communication

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Log
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.motionapps.sensorservices.R
import com.motionapps.wearoslib.WearOsConstants
import java.io.File

object DataSync {
    private const val TAG = "DataSync"
    private val permittedValues = arrayOf("ACG.csv", "ACC.csv", "GYRO.csv", "MAGNET.csv", "HEART_RATE.csv", "GPS.csv", "extra.json")

    /**
     * deletes all the files in internal storage
     *
     * @param context
     */

    fun deleteAllFolders(context: Context) {
        if (isSync(context)) {
            val root = File(context.filesDir, context.getString(R.string.app_name)) // root
            val folders = root.listFiles() // measurement folders
            if (folders != null) {
                for (folder in folders) {
                    if(folder.isFile){
                        continue
                    }

                    val measurementFiles = folder.listFiles() // measurement files in folder
                    if (measurementFiles != null) {
                        for (file in measurementFiles) {
                            val path = file.absolutePath
                            val parts = path.split("/".toRegex()).toTypedArray()
                            if (file.delete() && parts[parts.size - 1] in permittedValues) {
                                Log.w(TAG, file.name + " has been deleted")
                            }
                        }
                    }
                    if (folder.delete()) {
                        Log.w(TAG, folder.name + " has been deleted")
                    }
                }
            }
        }
    }

    /**
     * deletes specific folder in internal storage
     *
     * @param context
     * @param folderToDelete - folder name
     */
    fun deleteFolder(context: Context, folderToDelete: String) {
        if (isSync(context)) {
            val root = File(context.filesDir, context.getString(R.string.app_name)) // root // main folder in internal storage
            val folders = root.listFiles()
            if (folders != null) {
                for (f in folders) {
                    if(folderToDelete in f.absolutePath){ // measurement folders
                        val files = f.listFiles()
                        if (files != null){
                            for(file in files){ // measurement file
                                file.delete()
                                Log.w(TAG, "${file.absolutePath} was deleted")
                            }
                            f.delete()
                            Log.w(TAG, "${f.absolutePath} was deleted")
                            break
                        }

                    }
                }
            }
        }
    }


    /**
     * Creates status report of the internal storage
     *
     * @param context
     * @return
     */
    fun dataAvailable(context: Context): String {
        if (isSync(context)) {
            val root = File(context.filesDir, context.getString(R.string.app_name)) // root // root
            val folders = root.listFiles()
            if (folders != null) { // measurement folders
                var count = 0 // total count of folders
                var totalCount = 0 // total count of files
                var size = 0.0 // size of them KB
                for (folder in folders) {
                    count++
                    val measurementFile = folder.listFiles() // measurement files
                    if (measurementFile != null) {
                        for (file in measurementFile) {
                            size += file.length() / 1024.0
                            totalCount++
                        }
                    }
                }
                val line = "$count|" + "%.03f".format( size / 1024.0) + "|$totalCount|${getFreeMemory(context)}"
                return line.replace(",", ".")
            }
        }
        return "0|0|0|" + getFreeMemory(context)
    }

    /**
     * Paths are divided by |
     * paths is constructed "measurementFolder/measurementFile", not whole paths with internal storage
     * @param context
     * @return string filled with paths
     */
    fun getPaths(context: Context): String {
        val stringBuilder = StringBuilder()
        if (isSync(context)) {
            val root = File(context.filesDir, context.getString(R.string.app_name)) // root // root folder
            val folders = root.listFiles() // measurement folders
            if (folders != null) {
                for (folder in folders) {

                    if(folder.isFile){
                        continue
                    }

                    val measurementFile = folder.listFiles() // measurement files
                    if (measurementFile != null) {
                        for (file in measurementFile) {
                            val path = file.absolutePath
                            val parts = path.split("/".toRegex()).toTypedArray()
                            if(parts[parts.size - 1] in permittedValues){
                                stringBuilder.append(parts[parts.size - 2]).append("/")
                                    .append(parts[parts.size - 1]).append("|")
                            }
                        }
                    }
                }
                return stringBuilder.toString()
            }
        }
        return ""
    }

    /**
     * @param context
     * @return true if any folder exists in internalStorage
     */
    private fun isSync(context: Context): Boolean {
        val folder = File(context.filesDir, context.getString(R.string.app_name))
        if(folder.exists()){
            val files = folder.listFiles()
            if (files != null) {
                return files.isNotEmpty()
            }
        }
        return false
    }

    /**
     * Sends file as asset to the phone
     *
     * @param context
     * @param path to the file
     * @param statusListener callback if everything went ok
     */
    fun sendFile(context: Context, path: String?, statusListener: StatusListener) {
        if (isSync(context)) {
            val root = File(context.filesDir, context.getString(R.string.app_name)) // root
            val folders = root.listFiles() // root file
            if (folders != null) {
                for (folder in folders) {
                    val measurementFiles = folder.listFiles() // measurement folders
                    if (measurementFiles != null) {
                        for (file in measurementFiles) { // measurement files - csv, ...
                            if (file.absolutePath.contains(path!!)) { // finding path

                                // adding to the DataClient
                                val asset = Asset.createFromUri(Uri.fromFile(file))
                                val dataMap = PutDataMapRequest.create(WearOsConstants.FILE_TO_TRANSFER_PATH)
                                dataMap.dataMap.putAsset(WearOsConstants.FILE_TO_TRANSFER_PATH, asset)

                                val request = dataMap.asPutDataRequest()
                                request.setUrgent()

                                val putTask = Wearable.getDataClient(context).putDataItem(request)
                                putTask.addOnSuccessListener {
                                    statusListener.onStatusChange(true)
                                    Log.i(TAG, file.name + " was successfully sent")
                                }
                                putTask.addOnFailureListener { e: Exception ->
                                    statusListener.onStatusChange(false)
                                    e.printStackTrace()
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * After the end of the synchronization of the files, the file can be stored in dataClient
     * if the same file would be required to move again, the event would not be triggered on the
     * phone side. This is why at the end of the synchronization, the null is placed in the path
     * of the dataClient.
     *
     * @param context
     */
    fun nullAsset(context: Context?) {
        val dataMap = PutDataMapRequest.create(WearOsConstants.FILE_TO_TRANSFER_PATH)
        dataMap.dataMap.putAsset(WearOsConstants.FILE_TO_TRANSFER_PATH, null) // null put into the asset
        val request = dataMap.asPutDataRequest()

        request.setUrgent()
        val putTask = Wearable.getDataClient(context!!).putDataItem(request)
        putTask.addOnSuccessListener { Log.i(TAG, "nullAsset: successful") }
        putTask.addOnFailureListener { e: Exception ->
            Log.e(TAG, "nullAsset: failed")
            Log.e(TAG, "nullAsset: " + e.message)
        }
    }

    /**
     * @param context
     * @return size of free internal storage in MB
     */
    private fun getFreeMemory(context: Context): Float {
        val stat = StatFs(context.getExternalFilesDir("")?.absolutePath)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        return bytesAvailable / (1024f * 1024f)
    }

    /**
     * some functions require callback - sends if everything is ok
     *
     */
    interface StatusListener {
        fun onStatusChange(status: Boolean)
    }
}
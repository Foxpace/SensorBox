package com.motionapps.sensorservices.handlers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.balda.flipper.DocumentFileCompat
import com.balda.flipper.OperationFailedException
import com.balda.flipper.Root
import com.balda.flipper.StorageManagerCompat
import com.motionapps.sensorservices.R
import es.dmoral.toasty.Toasty
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Storage functions to create, delete folders and files
 */
object StorageHandler {
    /**
     * changes milliseconds from the beginning of the epoch to string based on the formatting
     *
     * @param milliSeconds - System.currentMillis()
     * @param stringFormat - "dd. MM. yyyy HH:mm:ss"
     * @return string with formatted date
     */
    fun getDate(milliSeconds: Long, stringFormat: String ="dd. MM. yyyy HH:mm:ss"): String {
        val formatter = SimpleDateFormat(stringFormat, Locale.getDefault())
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }


    /**
     * creates mainFolder, which is placed as root directory
     *
     * @param context
     * @param intent - from ActivityResult, when user picks the directory
     * @return - boolean if everything is ok
     */
    fun createMainFolder(context: Context, intent: Intent?): Boolean{
        val root: Root?
        StorageManagerCompat(context).also {
            root = if (intent == null) {
                it.getRoot(StorageManagerCompat.DEF_MAIN_ROOT)
            } else {
                it.deleteRoot(StorageManagerCompat.DEF_MAIN_ROOT)
                it.addRoot(context, StorageManagerCompat.DEF_MAIN_ROOT, intent)
            }
        }
            val f: DocumentFile = root?.toRootDirectory(context) ?: return false
            try {
                f.findFile(context.getString(R.string.app_name))?.let {
                    if(it.exists()){
                        return true
                    }
                }

                f.createDirectory(context.getString(R.string.app_name))
                return true
            } catch (e: OperationFailedException) {
                e.printStackTrace()
                Toasty.error(context, context.getString(R.string.intro_error), Toasty.LENGTH_LONG, true).show()
            }
        return false
    }

    /**
     * checks existence of the main folder
     *
     * @param context
     * @return true if exists
     */
    fun isFolder(context: Context): Boolean {
        val manager = StorageManagerCompat(context)
        manager.getRoot(StorageManagerCompat.DEF_MAIN_ROOT)?.let{
            val f: DocumentFile = it.toRootDirectory(context)
            return DocumentFileCompat.peekSubFolder(f, context.getString(R.string.app_name)) != null
        }
        return false
    }

    /**
     * checks if everything is ok from permission perspective and if the folder exists
     *
     * @param context
     * @return true if exists
     */
    fun isAccess(context: Context): Boolean{
        return when {
            Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT -> {
                isFolder(context)
            }
            Build.VERSION_CODES.M <= Build.VERSION.SDK_INT -> {
                isFolder(context) && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                isFolder(context)
            }
        }
    }

    /**
     *
     * @param context
     * @return whole path to main folder in string format
     */
    fun getFolderName(context: Context): String {
        val manager = StorageManagerCompat(context)
        manager.getRoot(StorageManagerCompat.DEF_MAIN_ROOT)?.let{

            val f: DocumentFile = it.toRootDirectory(context)
            val documentFile: DocumentFile? = DocumentFileCompat.peekSubFolder(f, context.getString(
                R.string.app_name
            ))

            if (documentFile != null) {
                val file = File(documentFile.uri.path!!)
                val split = file.path.split(":".toRegex()).toTypedArray()
                return when {
                    split.size >= 2 -> reversePath(split[1])
                    split.isNotEmpty() -> reversePath(split[0])
                    else -> context.getString(R.string.no_path)
                }
            }

            return context.getString(R.string.no_path)
        }
        return context.getString(R.string.no_path)
    }

    /**
     *
     *
     * @param path - path to main folder
     * @return reversed path, because DocumentFile is reversed
     */
    private fun reversePath(path: String): String {
        val parts = path.split("/".toRegex()).toTypedArray()
        val stringBuilder = StringBuilder()
        for (i in parts.indices.reversed()) {
            stringBuilder.append(parts[i]).append("/")
        }
        return stringBuilder.toString()
    }

    /**
     * creates internal measurement folder
     *
     * @param context
     * @param folderName - measurement name
     * @return - if everything is ok
     */
    fun createInternalStorageMeasurementFolder(context: Context, folderName: String): Boolean{
        val mainFolder = File(context.filesDir, context.getString(R.string.app_name))
        if(!mainFolder.exists()){
            mainFolder.mkdirs()
        }

        val folder = File(mainFolder, folderName)
        return folder.mkdirs()
    }

    /**
     * creates measurement folder in phone
     *
     * @param context
     * @param folderName - measurement name
     * @return - if everything is ok
     */
    fun createFolderMeasurement(context: Context, folderName: String): Boolean  {
        val manager = StorageManagerCompat(context)
        val root = manager.getRoot(StorageManagerCompat.DEF_MAIN_ROOT)
        if (root != null) {
            val f = root.toRootDirectory(context)
            if (f != null) {
                val subFolder =
                    DocumentFileCompat.peekSubFolder(f, context.getString(R.string.app_name)) // main folder
                if (subFolder != null) {
                    if (DocumentFileCompat.peekSubFolder(subFolder, folderName) == null) { // measurement folder
                        subFolder.createDirectory(folderName)
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * creates file in measurement folder
     *
     * @param context
     * @param folderName - name of the folder
     * @param mimeOfNewFile - json, txt, csv
     * @param nameOfNewFile - name of the file
     * @return - outputStream to store data
     */
    @Throws(IOException::class)
    fun createFileInFolder(
        context: Context,
        folderName: String,
        mimeOfNewFile: String,
        nameOfNewFile: String
//        stringToSave: String
    ): OutputStream? {
        val manager = StorageManagerCompat(context)
        val root = manager.getRoot(StorageManagerCompat.DEF_MAIN_ROOT)
        if (root != null) {
            val f = root.toRootDirectory(context)
            if (f != null) {
                val subFolder = DocumentFileCompat.peekSubFolder(f, context.getString(R.string.app_name)) // main folder
                if (subFolder != null) {
                    val recordFolder = DocumentFileCompat.peekSubFolder(subFolder, folderName) // measurement folder
                    if (recordFolder != null) {
                        val documentNewFile = recordFolder.createFile(mimeOfNewFile, nameOfNewFile) // file
                        if (documentNewFile != null) {
                            return context.contentResolver.openOutputStream(documentNewFile.uri)
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * created file in internal storage - creates own folder in internal storage, where all the measurements are stored
     * name should be "SensorBox"
     * @param context
     * @param folderName - folder to use
     * @param nameOfFile - name of the file
     * @return - outputStream to store data
     */
    fun createFileInInternalFolder(context: Context, folderName: String, nameOfFile: String): OutputStream {
        val mainFolder = File(context.filesDir, context.getString(R.string.app_name))
        if(!mainFolder.exists()){
            mainFolder.mkdirs()
        }

        val folder = File(mainFolder, folderName)
        if(!folder.exists()){
            folder.mkdirs()
        }

        val file = File(folder, nameOfFile)
        return FileOutputStream(file)
    }

    /**
     * deletes whole folder by name
     *
     * @param context
     * @param deleteFolder - name of the folder to delete
     * @return if everything is ok
     */
    fun deleteByNameOfFolder(
        context: Context,
        deleteFolder: String
    ): Boolean {
        val manager = StorageManagerCompat(context)
        val root = manager.getRoot(StorageManagerCompat.DEF_MAIN_ROOT)
        if (root != null) {
            val f = root.toRootDirectory(context)
            if (f != null) {
                val subFolder =
                    DocumentFileCompat.peekSubFolder(f, context.getString(R.string.app_name))
                if (subFolder != null) {
                    DocumentFileCompat.peekSubFolder(subFolder, deleteFolder)?.let {
                        return it.delete() // deletes everything inside too
                    }
                }
            }
        }
        return false
    }
}
package com.motionapps.wearoslib

import android.content.Context
import android.util.Log
import com.balda.flipper.DocumentFileCompat
import com.balda.flipper.StorageManagerCompat
import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Functions to store files
 */
object WearOsStorageHandler {

    private const val TAG = "WearOsHandler"

    /**
     * InputStream is stored into specific folder
     *
     * @param context
     * @param inputStream - to store
     * @param mainFolderName - SensorBox in our case
     * @param folderName - folder of the measurement
     * @param fileName - name of the file, where the data will be stored
     */
    fun storeInputStream(
        context: Context,
        inputStream: InputStream,
        mainFolderName: String,
        folderName: String,
        fileName: String
    ) {

        // checks main folder
        if (!createFolderMeasurement(context, mainFolderName, folderName)) {
            Log.i(TAG, "$folderName exists")
        } else {
            Log.i(TAG, "$folderName was created")
        }

        val mime =
            when {
                ".txt" in folderName -> {
                    "txt"
                }
                ".json" in folderName -> {
                    "json"
                }
                else -> {
                    "csv"
                }
            }

        // creates file
        createFileInFolder(
            context,
            mainFolderName,
            folderName,
            mime,
            "WEAR_$fileName"
        )?.let { outputStream ->

            // rewrites inputStream into OutputStream
            val bufferSize = 1024
            val buffer = CharArray(bufferSize)
            val reader: Reader = InputStreamReader(inputStream, StandardCharsets.UTF_8)
            while (reader.read(buffer) >= 0) {
                outputStream.write(buffer.concatToString().toByteArray())
            }

            outputStream.flush()
            outputStream.close()
        }

        inputStream.close()
    }

    /**
     * Creates measurement folder in main folder
     *
     * @param context
     * @param mainFolderName - SensorBox
     * @param folderName
     * @return - boolean if it was created
     */
    private fun createFolderMeasurement(
        context: Context,
        mainFolderName: String,
        folderName: String
    ): Boolean {
        val manager = StorageManagerCompat(context)
        val root = manager.getRoot(StorageManagerCompat.DEF_MAIN_ROOT) ?: return false // root access
        val f = root.toRootDirectory(context) ?: return false
        val subFolder = DocumentFileCompat.peekSubFolder(f, mainFolderName) ?: return false // find main folder

        if (DocumentFileCompat.peekSubFolder(
                subFolder,
                folderName
            ) == null
        ) { // create our measurement folder, if it does not exists
            subFolder.createDirectory(folderName)
            return true
        }
        return false
    }

    /**
     * Creates file into which the data will be stored
     * It also deletes file, if it exists
     * @param context
     * @param folderMain - SensorBox
     * @param folderName - ENDLESS_...
     * @param mimeOfNewFile - json, csv, txt, ...
     * @param nameOfNewFile - ACG.csv, ...
     * @return OutputStream, which can be used to store data
     */
    @Throws(IOException::class)
    private fun createFileInFolder(
        context: Context,
        folderMain: String,
        folderName: String,
        mimeOfNewFile: String,
        nameOfNewFile: String
    ): OutputStream? {

        val manager = StorageManagerCompat(context)
        val root = manager.getRoot(StorageManagerCompat.DEF_MAIN_ROOT) ?: return null // root access
        val f = root.toRootDirectory(context) ?: return null
        var subFolder = DocumentFileCompat.peekSubFolder(f, folderMain) // SensorBox folder
        if(subFolder == null || !subFolder.exists()){
            subFolder = f.createDirectory("SensorBox") ?: return null
        }
        var recordFolder = DocumentFileCompat.peekSubFolder(subFolder, folderName)// measurementFolder
        if(recordFolder == null || !recordFolder.exists()){
            recordFolder = f.createDirectory(folderName) ?: return null
        }
        recordFolder.findFile(nameOfNewFile)?.delete() // deletes previous stored data
        recordFolder.createFile(mimeOfNewFile, nameOfNewFile)?.let {
            return context.contentResolver.openOutputStream(it.uri) // creates new file
        }

        return null
    }
}
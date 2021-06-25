package com.motionapps.wearoslib

import android.app.Service
import android.content.*
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.ExecutionException

/**
 * Communicates with Wear Os device and downloads data from it
 * 1. service asks for all paths to all files in Wearable
 * 2. Wearable packs them together and sends them back to MsgListener and then to this service
 * 3. Service sends request for one specific file and awaits DataClient event
 * 4. Wearable creates asset from file and put it into DataClient, which will create event at phone side
 * 5. Service receives DataClient event, gets asset and through InputStream and OutputStream new file is created
 * 6. if all data are downloaded - service sends message to delete all the data on the Wearable side
 */
class WearOsSyncService : Service(), DataClient.OnDataChangedListener, WearOsListener {


    private var receiverRegistered = false
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action == WearOsConstants.STOP_SYNC) {
                stopSelf()
            // paths are obtained from Wear os
            } else if (intent.action == WearOsConstants.WEAR_SEND_PATHS) {

                val data: String? = intent.getStringExtra(WearOsConstants.WEAR_SEND_PATHS_EXTRA)
                if (data != null) {
                    pathsToExport = data.split("|") // they are divided by |

                    if (pathsToExport!!.size - 1 != totalCount) { // number of them must equal with status report
                        Toasty.error(context, R.string.sync_failed_restart, Toasty.LENGTH_LONG, true)
                            .show()
                        stopSelf()
                    } else {
                        sendMsgToSendFile() // requests first file to send
                    }
                } else {
                    Toasty.error(context, R.string.sync_failed_restart, Toasty.LENGTH_LONG, true)
                        .show()
                    stopSelf()
                }
            }
        }
    }

    private val wearOsHandler = WearOsHandler()
    private var dataClient: DataClient? = null
    private var job: Job? = null

    private var pathsToExport: List<String>? = null // list of files to download
    private var totalCount = -1
    private var counter = 0
    var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            running = true
            totalCount = intent.getIntExtra(
                WearOsConstants.NUMBER_OF_FILES, 0
            )
            startForeground(
                NOTIFICATION_ID, WearOsNotify.createProgressNotification(
                    this, totalCount, 0, NotificationCompat.PRIORITY_HIGH
                )
            )

            broadcastRegistration()
            // sets up data client
            dataClient = Wearable.getDataClient(this)
            dataClient?.addListener(this)

            // independently searches for the Wear Os device
            job = CoroutineScope(Dispatchers.Main).launch {
                wearOsHandler.searchForWearOs(
                    this@WearOsSyncService,
                    this@WearOsSyncService, WearOsConstants.WEAR_APP_CAPABILITY
                )
            }
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun broadcastRegistration() {
        if (!receiverRegistered) {
            val intentFilter = IntentFilter(WearOsConstants.STOP_SYNC)
            intentFilter.addAction(WearOsConstants.WEAR_SEND_PATHS)
            registerReceiver(broadcastReceiver, intentFilter)
            receiverRegistered = true
        }
    }

    /**
     * if the Wear Os device becomes available, the service sends message request for paths
     *
     * @param wearOsStates
     */
    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        if (wearOsStates is WearOsStates.PresenceResult) {
            if (wearOsStates.present) {
                wearOsHandler.sendMsg(
                    this,
                    WearOsConstants.WEAR_MESSAGE_PATH,
                    WearOsConstants.WEAR_SEND_PATHS // paths in Wearable device
                )
            } else {
                stopSelf()
            }
        }
    }

    /**
     * sends request for the specific file and awaits the DataClient event
     *
     */
    private fun sendMsgToSendFile() {
        if (pathsToExport!![counter] != "") {
            wearOsHandler.sendMsg(
                this,
                WearOsConstants.WEAR_MESSAGE_PATH,
                "${WearOsConstants.WEAR_SEND_FILE};${pathsToExport!![counter]}"
            )
        }

    }

    inner class WearOsSyncServiceBinder : Binder() {
        fun getService(): WearOsSyncService {
            return this@WearOsSyncService
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        return WearOsSyncServiceBinder()
    }

    /**
     *
     * Gets event, when Wearable device puts file into the DataClient
     * @param buffer - events to iterate through
     */
    override fun onDataChanged(buffer: DataEventBuffer) {

        for (event in buffer) {
            // dataevent must be changed, path must equal and we have to have list of paths to export
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearOsConstants.FILE_TO_TRANSFER_PATH && pathsToExport != null) {
                // obtaining asset
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val asset: Asset =
                    dataMapItem.dataMap.getAsset(WearOsConstants.FILE_TO_TRANSFER_PATH) ?: return

                // folder and file strings "folder/file" string are received from Wearable
                val folder = pathsToExport!![counter].split("/")[0]
                val file = pathsToExport!![counter].split("/")[1]

                // async job
                job = CoroutineScope(Dispatchers.IO).launch {
                    val inputStream = // obtaining inputStream from the asset
                        try {
                            Tasks.await(
                                Wearable.getDataClient(this@WearOsSyncService).getFdForAsset(asset)
                            ).inputStream
                        } catch (e: ExecutionException) {
                            e.printStackTrace()
                            null
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                            null
                        } catch (e: IOException) {
                            e.printStackTrace()
                            null
                        }

                    if (inputStream != null) {
                        // storing data
                        WearOsStorageHandler.storeInputStream(
                            this@WearOsSyncService,
                            inputStream,
                            getString(R.string.app_name),
                            folder,
                            file
                        )
                    }

                    // updates notification and sends request for new file
                    withContext(Dispatchers.Main) {
                        WearOsNotify.updateNotification(
                            this@WearOsSyncService,
                            NOTIFICATION_ID,
                            WearOsNotify.createProgressNotification(
                                this@WearOsSyncService,
                                totalCount,
                                ++counter,
                                NotificationCompat.PRIORITY_HIGH
                            )
                        )

                        // at the end, we delete all the data on Wearable side
                        if (counter == totalCount) {
                            wearOsHandler.sendMsg(
                                this@WearOsSyncService,
                                WearOsConstants.WEAR_MESSAGE_PATH,
                                WearOsConstants.DELETE_ALL_MEASUREMENTS
                            )
                            stopSelf()
                        } else {
                            sendMsgToSendFile()
                        }
                    }
                }
            } else if (pathsToExport == null) {
                Log.w(TAG, "Folder has been set to null probably")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false

        if (receiverRegistered) {
            unregisterReceiver(broadcastReceiver)
        }

        dataClient?.removeListener(this)

        job?.cancel()
        wearOsHandler.onDestroy()
        stopForeground(true)
    }

    companion object {

        private const val NOTIFICATION_ID = 659674
        private const val TAG = "WearOsSyncService"

    }


}
package com.rakshakmesh.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import android.os.Looper
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {

    private lateinit var connectionsClient: ConnectionsClient

    private val serviceId = "com.rakshakmesh.app"
    private val strategy = Strategy.P2P_CLUSTER

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // =========================================================
// RESCUE GATEWAY
// =========================================================

    private val backendUrl =
        "https://rakshakmesh-api.onrender.com/api/v1/sos"
    private var gatewayMode by mutableStateOf(false)
    private var gatewaySyncing by mutableStateOf(false)

    private var connectedEndpointIds =
        mutableStateMapOf<String, Boolean>()

    private val discoveredEndpoints =
        mutableStateMapOf<String, String>()

    private var meshRunning by mutableStateOf(false)

    private var sosList by mutableStateOf(
        listOf<JSONObject>()
    )

    // =========================================================
    // PERMISSION HANDLER
    // =========================================================

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            if (permissions.values.all { it }) {
                startMesh()
            } else {
                Toast.makeText(
                    this,
                    "Permissions required for Mesh",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // =========================================================
    // ACTIVITY
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectionsClient =
            Nearby.getConnectionsClient(this)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        loadSosList()

        setContent {
            RakshakMeshUI()
        }
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    private fun requestMeshPermissions() {

        val permissions =
            mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= 31) {

            permissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        }

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            permissions.add(
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        }

        permissions.add(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        permissionLauncher.launch(
            permissions.toTypedArray()
        )
    }

    private fun hasMeshPermissions(): Boolean {

        val bluetoothGranted =
            if (android.os.Build.VERSION.SDK_INT >= 31) {

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED &&

                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED &&

                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                        ) == PackageManager.PERMISSION_GRANTED

            } else {
                true
            }

        val wifiGranted =
            if (android.os.Build.VERSION.SDK_INT >= 33) {

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) == PackageManager.PERMISSION_GRANTED

            } else {
                true
            }

        val locationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return bluetoothGranted &&
                wifiGranted &&
                locationGranted
    }

    // =========================================================
    // START MESH
    // =========================================================

    private fun startMesh() {

        if (!hasMeshPermissions()) {
            requestMeshPermissions()
            return
        }

        meshRunning = true
        discoveredEndpoints.clear()

        connectionsClient.startAdvertising(
            "RakshakMesh Device",
            serviceId,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder()
                .setStrategy(strategy)
                .build()
        )

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            DiscoveryOptions.Builder()
                .setStrategy(strategy)
                .build()
        )

        Toast.makeText(
            this,
            "Mesh started 📡",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // DISCOVERY
    // =========================================================

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {

                discoveredEndpoints[
                    endpointId
                ] = info.endpointName

                Toast.makeText(
                    this@MainActivity,
                    "Found: ${info.endpointName}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onEndpointLost(
                endpointId: String
            ) {

                discoveredEndpoints.remove(
                    endpointId
                )
            }
        }

    // =========================================================
    // CONNECTION
    // =========================================================

    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {

                connectionsClient.acceptConnection(
                    endpointId,
                    payloadCallback
                )
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {

                if (
                    result.status.statusCode ==
                    ConnectionsStatusCodes.STATUS_OK
                ) {

                    connectedEndpointIds[
                        endpointId
                    ] = true

                    Toast.makeText(
                        this@MainActivity,
                        "CONNECTED 🟢",
                        Toast.LENGTH_SHORT
                    ).show()

                    // =================================================
                    // IMPORTANT:
                    // A NEW CONNECTION TRIGGERS SYNCHRONIZATION
                    // =================================================

                    sendSyncRequest(endpointId)

                } else {

                    Toast.makeText(
                        this@MainActivity,
                        "Connection failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onDisconnected(
                endpointId: String
            ) {

                connectedEndpointIds.remove(
                    endpointId
                )

                Toast.makeText(
                    this@MainActivity,
                    "Device disconnected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // =========================================================
    // CONNECT
    // =========================================================

    private fun connectToDevice(
        endpointId: String
    ) {

        if (!hasMeshPermissions()) {
            requestMeshPermissions()
            return
        }

        connectionsClient.requestConnection(
            "RakshakMesh Device",
            endpointId,
            connectionLifecycleCallback
        )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Connection requested...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Connection failed: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // CREATE SOS
    // =========================================================

    private fun getCurrentLocation(
        onLocationReceived: (Double?, Double?) -> Unit
    ) {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                "Location permission not granted",
                Toast.LENGTH_SHORT
            ).show()

            onLocationReceived(null, null)
            return
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(5000)
            .setDurationMillis(10000)
            .build()

        fusedLocationClient
            .getCurrentLocation(request, null)
            .addOnSuccessListener { location ->

                if (location != null) {

                    println(
                        "📍 CURRENT LOCATION: " +
                                "${location.latitude}, ${location.longitude}"
                    )

                    onLocationReceived(
                        location.latitude,
                        location.longitude
                    )

                } else {

                    println("❌ LOCATION RESULT IS NULL")

                    Toast.makeText(
                        this,
                        "Unable to get current location",
                        Toast.LENGTH_SHORT
                    ).show()

                    onLocationReceived(null, null)
                }
            }
            .addOnFailureListener { error ->

                println(
                    "❌ LOCATION ERROR: ${error.message}"
                )

                Toast.makeText(
                    this,
                    "Location error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()

                onLocationReceived(null, null)
            }
    }
    private fun createSos(
        peopleCount: String,
        condition: String,
        message: String
    ) {

        getCurrentLocation { latitude, longitude ->

            val sos = JSONObject()

            sos.put("sosId", UUID.randomUUID().toString())
            sos.put("message", message)
            sos.put("peopleCount", peopleCount)
            sos.put("condition", condition)

            sos.put(
                "latitude",
                latitude ?: JSONObject.NULL
            )

            sos.put(
                "longitude",
                longitude ?: JSONObject.NULL
            )

            sos.put(
                "timestamp",
                System.currentTimeMillis()
            )

            sos.put("status", "PENDING")

            saveSosLocally(sos)

            sendLatestSos()

            runOnUiThread {
                Toast.makeText(
                    this,
                    "🚨 SOS created with location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // SEND LATEST SOS
    // =========================================================

    private fun sendLatestSos() {

        if (connectedEndpointIds.isEmpty()) {

            Toast.makeText(
                this,
                "No connected devices",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (sosList.isEmpty()) {

            Toast.makeText(
                this,
                "Create an SOS first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val latestSos =
            JSONObject(
                sosList.first().toString()
            )

        latestSos.put(
            "type",
            "SOS"
        )

        val payload =
            Payload.fromBytes(
                latestSos
                    .toString()
                    .toByteArray(Charsets.UTF_8)
            )

        connectedEndpointIds.keys.forEach { endpointId ->

            connectionsClient.sendPayload(
                endpointId,
                payload
            )
        }

        Toast.makeText(
            this,
            "🚨 SOS SENT TO MESH",
            Toast.LENGTH_LONG
        ).show()
    }

    // =========================================================
// GATEWAY → BACKEND
// =========================================================

    private fun syncGatewayToServer() {

        if (sosList.isEmpty()) {
            Toast.makeText(
                this,
                "No SOS reports to upload",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (gatewaySyncing) return

        gatewaySyncing = true

        Toast.makeText(
            this,
            "📡 Gateway syncing...",
            Toast.LENGTH_SHORT
        ).show()

        Thread {

            var successCount = 0

            sosList.forEach { sos ->

                try {

                    val url =
                        URL(backendUrl)

                    val connection =
                        url.openConnection()
                                as HttpURLConnection

                    connection.requestMethod = "POST"
                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )

                    connection.doOutput = true
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val json =
                        sos.toString()

                    connection.outputStream.use { output ->
                        output.write(
                            json.toByteArray(
                                Charsets.UTF_8
                            )
                        )
                    }

                    val responseCode =
                        connection.responseCode

                    if (responseCode in 200..299) {
                        successCount++
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "❌ Server returned: $responseCode",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    connection.disconnect()

                } catch (e: Exception) {

                    e.printStackTrace()

                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "❌ Gateway error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            runOnUiThread {

                gatewaySyncing = false

                Toast.makeText(
                    this,
                    "📡 Gateway syncing...",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }.start()
    }
    // =========================================================
    // STORE-AND-FORWARD SYNC
    // =========================================================

    /*
     * When a connection is established:
     *
     * Device A → "Here are the SOS IDs I have"
     *
     * Device B compares those IDs with its own local store.
     *
     * B then sends the SOS messages that A does not have.
     *
     * This is the foundation of delayed store-and-forward.
     */

    private fun sendSyncRequest(
        endpointId: String
    ) {

        val ids =
            JSONArray()

        sosList.forEach { sos ->

            ids.put(
                sos.optString("sosId")
            )
        }

        val syncMessage =
            JSONObject()

        syncMessage.put(
            "type",
            "SYNC"
        )

        syncMessage.put(
            "sosIds",
            ids
        )

        val payload =
            Payload.fromBytes(
                syncMessage
                    .toString()
                    .toByteArray(Charsets.UTF_8)
            )

        connectionsClient.sendPayload(
            endpointId,
            payload
        )

        Toast.makeText(
            this,
            "🔄 Sync started",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // SEND MISSING SOS
    // =========================================================

    private fun sendMissingSos(
        endpointId: String,
        remoteIds: JSONArray
    ) {

        val remoteIdSet =
            mutableSetOf<String>()

        for (i in 0 until remoteIds.length()) {

            remoteIdSet.add(
                remoteIds.optString(i)
            )
        }

        var sentCount = 0

        sosList.forEach { sos ->

            val sosId =
                sos.optString("sosId")

            /*
             * Only send SOS if the other device
             * does NOT already have it.
             */

            if (
                sosId.isNotEmpty() &&
                !remoteIdSet.contains(sosId)
            ) {

                val sosToSend =
                    JSONObject(
                        sos.toString()
                    )

                sosToSend.put(
                    "type",
                    "SOS"
                )

                val payload =
                    Payload.fromBytes(
                        sosToSend
                            .toString()
                            .toByteArray(Charsets.UTF_8)
                    )

                connectionsClient.sendPayload(
                    endpointId,
                    payload
                )

                sentCount++
            }
        }

        if (sentCount > 0) {

            Toast.makeText(
                this,
                "📤 Sent $sentCount missing SOS",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // RECEIVE PAYLOAD
    // =========================================================

    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {

                if (
                    payload.type !=
                    Payload.Type.BYTES
                ) {
                    return
                }

                val bytes =
                    payload.asBytes()
                        ?: return

                val jsonString =
                    String(
                        bytes,
                        Charsets.UTF_8
                    )

                try {

                    val receivedMessage =
                        JSONObject(jsonString)

                    val messageType =
                        receivedMessage.optString(
                            "type",
                            "SOS"
                        )

                    // =================================================
                    // SYNC MESSAGE
                    // =================================================

                    if (
                        messageType ==
                        "SYNC"
                    ) {

                        val remoteIds =
                            receivedMessage.optJSONArray(
                                "sosIds"
                            ) ?: JSONArray()

                        /*
                         * The other device has told us
                         * which SOS messages it already owns.
                         *
                         * Send everything it is missing.
                         */

                        sendMissingSos(
                            endpointId,
                            remoteIds
                        )

                        return
                    }

                    // =================================================
                    // SOS MESSAGE
                    // =================================================

                    if (
                        messageType ==
                        "SOS"
                    ) {

                        receiveSos(
                            receivedMessage,
                            endpointId
                        )

                        return
                    }

                } catch (e: Exception) {

                    e.printStackTrace()
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
                // Transfer progress
            }
        }

    // =========================================================
    // RECEIVE + STORE SOS
    // =========================================================

    private fun receiveSos(
        receivedSos: JSONObject,
        senderEndpointId: String
    ) {

        val sosId =
            receivedSos.optString(
                "sosId"
            )

        if (sosId.isEmpty()) {
            return
        }

        /*
         * DUPLICATE CHECK
         *
         * If this device already has the SOS,
         * do absolutely nothing.
         */

        val alreadyExists =
            sosList.any {

                it.optString(
                    "sosId"
                ) == sosId
            }

        if (alreadyExists) {

            return
        }

        /*
         * Store the SOS locally.
         */

        val storedSos =
            JSONObject(
                receivedSos.toString()
            )

        storedSos.put(
            "type",
            "SOS"
        )

        storedSos.put(
            "status",
            "RECEIVED"
        )

        storedSos.put(
            "receivedFrom",
            senderEndpointId
        )

        /*
         * Update relay history.
         */

        val relayHistory =
            storedSos.optJSONArray(
                "relayHistory"
            ) ?: JSONArray()

        relayHistory.put(
            senderEndpointId
        )

        storedSos.put(
            "relayHistory",
            relayHistory
        )

        /*
         * Add to persistent local storage.
         */

        runOnUiThread {

            sosList =
                listOf(
                    storedSos
                ) + sosList

            saveSosList()

            Toast.makeText(
                this@MainActivity,
                "🚨 SOS RECEIVED & STORED!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // STORAGE
    // =========================================================

    private fun saveSosLocally(sos: JSONObject) {
        runOnUiThread {
            sosList = listOf(sos) + sosList
            saveSosList()
        }
    }

    private fun saveSosList() {

        val array =
            JSONArray()

        sosList.forEach {
            array.put(it)
        }

        getSharedPreferences(
            "RakshakMesh",
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                "sosList",
                array.toString()
            )
            .apply()
    }

    private fun loadSosList() {

        val saved =
            getSharedPreferences(
                "RakshakMesh",
                Context.MODE_PRIVATE
            )
                .getString(
                    "sosList",
                    "[]"
                )

        val array =
            JSONArray(saved)

        val list =
            mutableListOf<JSONObject>()

        for (
        i in 0 until array.length()
        ) {

            list.add(
                array.getJSONObject(i)
            )
        }

        sosList = list
    }

    // =========================================================
    // STOP MESH
    // =========================================================

    private fun stopMesh() {

        connectionsClient
            .stopAdvertising()

        connectionsClient
            .stopDiscovery()

        connectionsClient
            .stopAllEndpoints()

        discoveredEndpoints.clear()
        connectedEndpointIds.clear()

        meshRunning = false

        Toast.makeText(
            this,
            "Mesh stopped",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // UI
    // =========================================================

    @Composable
    fun RakshakMeshUI() {

        var peopleCount by remember {
            mutableStateOf("")
        }

        var condition by remember {
            mutableStateOf("")
        }

        var message by remember {
            mutableStateOf("")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            Text(
                "🚨 RakshakMesh",
                style =
                    MaterialTheme.typography
                        .headlineMedium
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                if (
                    connectedEndpointIds
                        .isNotEmpty()
                )
                    "🟢 ${connectedEndpointIds.size} DEVICE(S) CONNECTED"
                else
                    "🔴 NO DEVICE CONNECTED"
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Button(
                onClick = {

                    if (meshRunning) {
                        stopMesh()
                    } else {
                        startMesh()
                    }

                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    if (meshRunning)
                        "STOP MESH"
                    else
                        "START MESH"
                )
            }

            Spacer(
                Modifier.height(12.dp)
            )

            Button(
                onClick = {
                    gatewayMode = !gatewayMode
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (gatewayMode)
                        "📡 GATEWAY MODE: ON"
                    else
                        "📡 ENABLE GATEWAY MODE"
                )
            }

            if (gatewayMode) {

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    "🌐 Backend: $backendUrl",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Button(
                    onClick = {
                        syncGatewayToServer()
                    },
                    enabled = !gatewaySyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        if (gatewaySyncing)
                            "SYNCING..."
                        else
                            "📤 SYNC STORED SOS TO RESCUE CENTER"
                    )
                }
            }


            Spacer(
                Modifier.height(16.dp)
            )

            Text(
                "Nearby Devices",
                style =
                    MaterialTheme.typography
                        .titleMedium
            )

            discoveredEndpoints
                .forEach {
                        (endpointId, name) ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 5.dp
                                ),
                        horizontalArrangement =
                            Arrangement
                                .SpaceBetween
                    ) {

                        Text(name)

                        Button(
                            onClick = {
                                connectToDevice(
                                    endpointId
                                )
                            }
                        ) {

                            Text("CONNECT")
                        }
                    }
                }

            Spacer(
                Modifier.height(16.dp)
            )

            OutlinedTextField(
                value = peopleCount,
                onValueChange = {
                    peopleCount = it
                },
                label = {
                    Text("People Count")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = condition,
                onValueChange = {
                    condition = it
                },
                label = {
                    Text("Condition")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                },
                label = {
                    Text("Emergency Message")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Button(
                onClick = {

                    createSos(
                        peopleCount,
                        condition,
                        message
                    )

                    peopleCount = ""
                    condition = ""
                    message = ""

                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("CREATE SOS")
            }

            Spacer(
                Modifier.height(8.dp)
            )

            if (
                connectedEndpointIds
                    .isNotEmpty()
            ) {

                Button(
                    onClick = {
                        sendLatestSos()
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "🚨 SEND LATEST SOS"
                    )
                }
            }

            Spacer(
                Modifier.height(16.dp)
            )

            Text(
                "Stored SOS Reports",
                style =
                    MaterialTheme.typography
                        .titleMedium
            )

            Spacer(
                Modifier.height(8.dp)
            )

            LazyColumn {

                items(sosList) { sos ->

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 5.dp
                                )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                )
                        ) {

                            Text(
                                "🚨 ${
                                    sos.optString(
                                        "status"
                                    )
                                }"
                            )

                            Text(
                                "People: ${
                                    sos.optString(
                                        "peopleCount"
                                    )
                                }"
                            )

                            Text(
                                "Condition: ${
                                    sos.optString(
                                        "condition"
                                    )
                                }"
                            )

                            Text(
                                sos.optString(
                                    "message"
                                )
                            )

                            Text(
                                "SOS ID: ${
                                    sos.optString(
                                        "sosId"
                                    )
                                }"
                            )
                        }
                    }
                }
            }
        }
    }
}

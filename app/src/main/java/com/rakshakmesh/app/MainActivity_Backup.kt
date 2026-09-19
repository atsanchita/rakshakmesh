package com.rakshakmesh.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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

class MainActivity_Backup : ComponentActivity() {

    private lateinit var connectionsClient: ConnectionsClient

    private val serviceId = "com.rakshakmesh.app"
    private val strategy = Strategy.P2P_CLUSTER

    private var connectedEndpointId by mutableStateOf<String?>(null)
    private var meshRunning by mutableStateOf(false)

    private val discoveredEndpoints =
        mutableStateMapOf<String, String>()

    private var sosList by mutableStateOf(listOf<JSONObject>())

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted = permissions.values.all { it }

            if (granted) {
                startMesh()
            } else {
                Toast.makeText(
                    this,
                    "Permissions required for Mesh",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectionsClient = Nearby.getConnectionsClient(this)

        loadSosList()

        setContent {
            RakshakMeshUI()
        }
    }

    // ---------------------------------------------------------
    // PERMISSIONS
    // ---------------------------------------------------------

    private fun requestMeshPermissions() {

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= 31) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        permissionLauncher.launch(permissions.toTypedArray())
    }

    // ---------------------------------------------------------
    // START MESH
    // ---------------------------------------------------------

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
        ).addOnSuccessListener {

            Toast.makeText(
                this,
                "Advertising started",
                Toast.LENGTH_SHORT
            ).show()

        }.addOnFailureListener {

            Toast.makeText(
                this,
                "Advertising failed: ${it.message}",
                Toast.LENGTH_LONG
            ).show()
        }

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            DiscoveryOptions.Builder()
                .setStrategy(strategy)
                .build()
        ).addOnSuccessListener {

            Toast.makeText(
                this,
                "Discovery started",
                Toast.LENGTH_SHORT
            ).show()

        }.addOnFailureListener {

            Toast.makeText(
                this,
                "Discovery failed: ${it.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------------------------------------------------------
    // DISCOVERY
    // ---------------------------------------------------------

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {

                discoveredEndpoints[endpointId] =
                    info.endpointName

                Toast.makeText(
                    this@MainActivity_Backup,
                    "Found: ${info.endpointName}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onEndpointLost(endpointId: String) {

                discoveredEndpoints.remove(endpointId)
            }
        }

    // ---------------------------------------------------------
    // CONNECTION
    // ---------------------------------------------------------

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

                if (result.status.statusCode ==
                    ConnectionsStatusCodes.STATUS_OK
                ) {

                    connectedEndpointId = endpointId

                    Toast.makeText(
                        this@MainActivity_Backup,
                        "CONNECTED! 🚀",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    Toast.makeText(
                        this@MainActivity_Backup,
                        "Connection failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onDisconnected(endpointId: String) {

                if (connectedEndpointId == endpointId) {
                    connectedEndpointId = null
                }

                Toast.makeText(
                    this@MainActivity_Backup,
                    "Device disconnected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // ---------------------------------------------------------
    // CONNECT TO DEVICE
    // ---------------------------------------------------------

    private fun connectToDevice(endpointId: String) {

        if (!hasMeshPermissions()) {
            requestMeshPermissions()
            return
        }

        connectionsClient.requestConnection(
            "RakshakMesh Device",
            endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {

            Toast.makeText(
                this,
                "Connection requested...",
                Toast.LENGTH_SHORT
            ).show()

        }.addOnFailureListener {

            Toast.makeText(
                this,
                "Connection request failed: ${it.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------------------------------------------------------
    // SEND SOS
    // ---------------------------------------------------------

    private fun sendLatestSos() {

        val endpointId = connectedEndpointId

        if (endpointId == null) {

            Toast.makeText(
                this,
                "No connected device",
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

        val latestSos = sosList.first()

        val jsonString = latestSos.toString()

        val payload =
            Payload.fromBytes(
                jsonString.toByteArray(Charsets.UTF_8)
            )

        connectionsClient
            .sendPayload(endpointId, payload)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "🚨 SOS SENT!",
                    Toast.LENGTH_LONG
                ).show()

            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "SOS sending failed: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ---------------------------------------------------------
    // RECEIVE SOS
    // ---------------------------------------------------------

    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {

                if (payload.type != Payload.Type.BYTES) {
                    return
                }

                val bytes = payload.asBytes() ?: return

                val jsonString =
                    String(bytes, Charsets.UTF_8)

                try {

                    val receivedSos =
                        JSONObject(jsonString)

                    val receivedId =
                        receivedSos.optString("sosId")

                    val alreadyExists =
                        sosList.any {
                            it.optString("sosId") == receivedId
                        }

                    if (!alreadyExists) {

                        receivedSos.put(
                            "status",
                            "RECEIVED"
                        )

                        sosList =
                            listOf(receivedSos) + sosList

                        saveSosList()

                        runOnUiThread {

                            Toast.makeText(
                                this@MainActivity_Backup,
                                "🚨 SOS RECEIVED!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                } catch (e: Exception) {

                    e.printStackTrace()
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
                // Transfer progress can be handled later
            }
        }

    // ---------------------------------------------------------
    // SOS LOCAL STORAGE
    // ---------------------------------------------------------

    private fun saveSos(
        peopleCount: String,
        condition: String,
        message: String
    ) {

        val sos = JSONObject()

        sos.put(
            "sosId",
            UUID.randomUUID().toString()
        )

        sos.put(
            "message",
            message
        )

        sos.put(
            "peopleCount",
            peopleCount
        )

        sos.put(
            "condition",
            condition
        )

        sos.put(
            "latitude",
            null
        )

        sos.put(
            "longitude",
            null
        )

        sos.put(
            "timestamp",
            System.currentTimeMillis()
        )

        sos.put(
            "status",
            "PENDING"
        )

        sosList =
            listOf(sos) + sosList

        saveSosList()

        Toast.makeText(
            this,
            "SOS saved locally 🚨",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveSosList() {

        val array = JSONArray()

        sosList.forEach {
            array.put(it)
        }

        getSharedPreferences(
            "RakshakMesh",
            MODE_PRIVATE
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
                MODE_PRIVATE
            )
                .getString(
                    "sosList",
                    "[]"
                )

        val array = JSONArray(saved)

        val list = mutableListOf<JSONObject>()

        for (i in 0 until array.length()) {
            list.add(array.getJSONObject(i))
        }

        sosList = list
    }

    // ---------------------------------------------------------
    // STOP MESH
    // ---------------------------------------------------------

    private fun stopMesh() {

        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()

        discoveredEndpoints.clear()
        connectedEndpointId = null
        meshRunning = false

        Toast.makeText(
            this,
            "Mesh stopped",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ---------------------------------------------------------
    // PERMISSION CHECK
    // ---------------------------------------------------------

    private fun hasMeshPermissions(): Boolean {

        val bluetoothGranted =
            if (Build.VERSION.SDK_INT >= 31) {

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
            if (Build.VERSION.SDK_INT >= 33) {

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

    // ---------------------------------------------------------
    // UI
    // ---------------------------------------------------------

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
                text = "🚨 RakshakMesh",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text =
                    if (connectedEndpointId != null)
                        "🟢 CONNECTED"
                    else
                        "🔴 NOT CONNECTED"
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = {

                    if (meshRunning) {
                        stopMesh()
                    } else {
                        startMesh()
                    }

                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    if (meshRunning)
                        "STOP MESH"
                    else
                        "START MESH"
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                "Nearby Devices",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            discoveredEndpoints.forEach { (endpointId, name) ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(name)

                    Button(
                        onClick = {
                            connectToDevice(endpointId)
                        }
                    ) {
                        Text("CONNECT")
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = peopleCount,
                onValueChange = {
                    peopleCount = it
                },
                label = {
                    Text("People Count")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = condition,
                onValueChange = {
                    condition = it
                },
                label = {
                    Text("Condition")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                },
                label = {
                    Text("Emergency Message")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = {

                    saveSos(
                        peopleCount,
                        condition,
                        message
                    )

                    peopleCount = ""
                    condition = ""
                    message = ""

                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("CREATE SOS")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (connectedEndpointId != null) {

                Button(
                    onClick = {
                        sendLatestSos()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text("🚨 SEND LATEST SOS")
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                "Stored SOS Reports",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LazyColumn {

                items(sosList) { sos ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {

                            Text(
                                "🚨 ${sos.optString("status")}"
                            )

                            Text(
                                "People: ${sos.optString("peopleCount")}"
                            )

                            Text(
                                "Condition: ${sos.optString("condition")}"
                            )

                            Text(
                                sos.optString("message")
                            )
                        }
                    }
                }
            }
        }
    }
}
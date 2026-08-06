package com.padlink.gamepad

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private lateinit var webView: WebView

    private val REQUEST_PERMS = 42

    private val hidDescriptor: ByteArray = byteArrayOf(
        0x05, 0x01,
        0x09, 0x05,
        0xA1.toByte(), 0x01,
        0xA1.toByte(), 0x00,
        0x05, 0x09,
        0x19, 0x01,
        0x29, 0x11,
        0x15, 0x00,
        0x25, 0x01,
        0x75, 0x01,
        0x95, 0x11,
        0x81, 0x02,
        0x75, 0x07,
        0x95, 0x01,
        0x81, 0x03,
        0x05, 0x01,
        0x09, 0x30,
        0x09, 0x31,
        0x09, 0x32,
        0x09, 0x35,
        0x15, 0x81.toByte(),
        0x25, 0x7F,
        0x75, 0x08,
        0x95, 0x04,
        0x81, 0x02,
        0xC0.toByte(),
        0xC0.toByte()
    )

    private val BUTTON_BITS = mapOf(
        "SELECT" to 0, "L3" to 1, "R3" to 2, "START" to 3,
        "DUP" to 4, "DRIGHT" to 5, "DDOWN" to 6, "DLEFT" to 7,
        "L2" to 8, "R2" to 9, "L1" to 10, "R1" to 11,
        "TRIANGLE" to 12, "CIRCLE" to 13, "CROSS" to 14, "SQUARE" to 15,
        "PS" to 16
    )

    private var buttonBits = 0
    private var leftX = 0
    private var leftY = 0
    private var rightX = 0
    private var rightY = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.addJavascriptInterface(JsBridge(), "Android")
        webView.loadUrl("file:///android_asset/controller.html")
        setContentView(webView)

        requestBtPermissionsIfNeeded()
    }

    private fun requestBtPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (needed.isEmpty()) {
            initHid()
        } else {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQUEST_PERMS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initHid()
            } else {
                Toast.makeText(this, "Permissions Bluetooth refusees", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initHid() {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Active le Bluetooth puis relance l'app", Toast.LENGTH_LONG).show()
            return
        }

        adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                bluetoothHidDevice = proxy as BluetoothHidDevice
                registerHidApp()
            }

            override fun onServiceDisconnected(profile: Int) {
                bluetoothHidDevice = null
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "Pad Link Gamepad",
            "Manette virtuelle via telephone",
            "PadLink",
            BluetoothHidDevice.SUBCLASS1_GAMEPAD,
            hidDescriptor
        )

        bluetoothHidDevice?.registerApp(
            sdp, null, null, mainExecutor,
            object : BluetoothHidDevice.Callback() {
                override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                    runOnUiThread {
                        if (registered) {
                            Toast.makeText(this@MainActivity,
                                "Pret. Cherche 'Pad Link Gamepad' dans le Bluetooth du PC/Mac.",
                                Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity,
                                "Ce telephone ne supporte pas le mode manette Bluetooth (HID Device).",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                    runOnUiThread {
                        when (state) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                connectedDevice = device
                                webView.evaluateJavascript("onNativeStatus('connected')", null)
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                connectedDevice = null
                                webView.evaluateJavascript("onNativeStatus('disconnected')", null)
                            }
                        }
                    }
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendReport() {
        val device = connectedDevice ?: return
        val report = byteArrayOf(
            (buttonBits and 0xFF).toByte(),
            ((buttonBits shr 8) and 0xFF).toByte(),
            ((buttonBits shr 16) and 0x01).toByte(),
            leftX.toByte(),
            leftY.toByte(),
            rightX.toByte(),
            rightY.toByte()
        )
        try {
            bluetoothHidDevice?.sendReport(device, 0, report)
        } catch (e: Exception) {
            Log.e("PadLink", "Echec envoi rapport HID", e)
        }
    }

    inner class JsBridge {
        @JavascriptInterface
        fun onButton(name: String, pressed: Boolean) {
            BUTTON_BITS[name]?.let { bit ->
                buttonBits = if (pressed) buttonBits or (1 shl bit) else buttonBits and (1 shl bit).inv()
            }
            sendReport()
        }

        @JavascriptInterface
        fun onStick(axis: String, x: Float, y: Float) {
            val ix = (x.coerceIn(-1f, 1f) * 127).toInt()
            val iy = (y.coerceIn(-1f, 1f) * 127).toInt()
            if (axis == "left") { leftX = ix; leftY = iy } else { rightX = ix; rightY = iy }
            sendReport()
        }

        @JavascriptInterface
        fun isConnected(): Boolean = connectedDevice != null
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        bluetoothHidDevice?.let {
            connectedDevice?.let { d -> it.disconnect(d) }
            it.unregisterApp()
        }
        super.onDestroy()
    }
}

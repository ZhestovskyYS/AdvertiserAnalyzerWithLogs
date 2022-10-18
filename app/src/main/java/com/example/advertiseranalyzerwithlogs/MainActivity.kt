package com.example.advertiseranalyzerwithlogs

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.example.advertiseranalyzerwithlogs.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::bind)
    private val logsStateFlow = MutableStateFlow(listOf(""))
    private val rvOutputList = logsStateFlow.value.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
            adapter = RVAdapter(rvOutputList)
        }
        startScanning()
        binding.clearButton.setOnClickListener {
            rvOutputList.clear()
            logsStateFlow.value = rvOutputList
            binding.recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun startScanning() {
        val bluetoothManager =
            applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val scanner = bluetoothAdapter.bluetoothLeScanner

        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
        )
        for (permission in permissions)
            if (ActivityCompat.checkSelfPermission( this, permission) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, permissions, 1)
        scanner.startScan(bleScannerCallback)
    }

    private val bleScannerCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result?.rssi?.let { it in -75..-45 } == true) {
                result.scanRecord?.getManufacturerSpecificData(0x004c)?.let {
                    val manData: ByteArray = if (it.size > 17) {
                        it.toMutableList().run {
                            slice(
                                size - 17 until size
                            )
                        }.toByteArray()
                    } else {
                        it
                    }
                    if (manData.count() >= 17 && manData[0].toUByte() == 1.toUByte()) {

                        insertOutput("Found signal: ${it.toList()}")
                        Log.i("TEST", it.toList().toString())
                        // We have found an apple background advertisement
                        var bytesAsBinary = "";
                        var bytesAsBinaryFormatted = "";
                        for (byteIndex in 1..16) {
                            var byteAsUnsignedInt = manData[byteIndex].toInt()
                            if (byteAsUnsignedInt < 0) {
                                byteAsUnsignedInt += 256
                            }
                            val binaryString =
                                String.format("%8s", Integer.toBinaryString(byteAsUnsignedInt))
                                    .replace(" ", "0")
                            bytesAsBinary += binaryString
                            bytesAsBinaryFormatted += binaryString + " "
                        }

                        Log.e("Bits", bytesAsBinary)
                        insertOutput("Recognised Bits: $bytesAsBinary")

                        val encodedBits = mutableListOf<UByte>().apply {
                            bytesAsBinary.drop(64).dropLast(17).forEach { stringBit ->
                                add(stringBit.digitToInt().toUByte())
                            }
                        }
                        HammingEcc.decodeBits(encodedBits)?.let { decodedBits ->
                            HammingEcc.bitsToBytes(decodedBits).toMutableList().apply {
                                removeFirst()
                                let { bytes ->
                                    val major: UShort = (
                                            (bytes[0].toUInt() shl 8).toUShort() +
                                                    bytes[1].toUShort()
                                            ).toUShort()

                                    val minor: UShort = (
                                            (bytes[2].toUInt() shl 8).toUShort() +
                                                    bytes[3].toUShort()
                                            ).toUShort()
                                    insertOutput("Found: Major = $major, Minor = $minor")
                                    Log.e("TEST", "major = $major, minor = $minor")
                                }
                            }
                        }
                    }
                }


            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG, "onBatchScanResults:${results.toString()}")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanFailed: $errorCode")
        }
    }

    private fun insertOutput(outputString: String) {
        rvOutputList.add(outputString)
        logsStateFlow.value = rvOutputList
        binding.recyclerView.adapter?.notifyItemInserted(rvOutputList.lastIndex)
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
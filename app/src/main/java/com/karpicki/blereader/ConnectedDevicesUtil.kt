package com.karpicki.blereader

import android.bluetooth.BluetoothDevice

class ConnectedDevicesUtil {
    companion object {
        private fun find(connectedDevices: ArrayList<BluetoothDevice>, device: BluetoothDevice): BluetoothDevice? {
            return connectedDevices.find {
                    connected -> connected.address.equals(device.address, true)
            }
        }

        fun remember(connectedDevices: ArrayList<BluetoothDevice>, device: BluetoothDevice): ArrayList<BluetoothDevice> {
            val found = find(connectedDevices, device)
            if (found == null) {
                connectedDevices.add(device)
            }
            return connectedDevices
        }

        fun isConnected(connectedDevices: ArrayList<BluetoothDevice>, device: BluetoothDevice): Boolean {
            return (find(connectedDevices, device) != null)
        }

        fun forget(connectedDevices: ArrayList<BluetoothDevice>, device: BluetoothDevice) : ArrayList<BluetoothDevice> {
            val found = find(connectedDevices, device)
            if (found != null) {
                connectedDevices.remove(found)
            }
            return connectedDevices
        }
    }
}
package com.karpicki.blereader

import android.bluetooth.BluetoothGatt

class Message {

    var gatt: BluetoothGatt
    var value: Int

    constructor(gatt: BluetoothGatt, value: Int) {
        this.gatt = gatt
        this.value = value
    }
}
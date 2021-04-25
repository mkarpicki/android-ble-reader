package com.karpicki.blereader

import java.util.*

class AllowedItem(
    var address: String,
    var label: String,
    var tsField: String,
    var serviceUUID: UUID,
    var characteristicsUUID: UUID,
    var type: String
    ){
}
package com.karpicki.blereader

class MessageList {
    companion object {
        private var list: ArrayList<Message> = ArrayList()
        fun get(): ArrayList<Message> {
            return list
        }
    }
}
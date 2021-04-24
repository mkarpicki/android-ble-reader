package com.karpicki.blereader

class MessageQueue {
    companion object {
        private var list: ArrayList<Message> = ArrayList()

        fun size(): Int {
            return list.size
        }
        fun insert(message: Message) {
            list.add(message)
        }
        fun get(): Message? {
            if (size() > 0) {
               return list.removeAt(0)
            }
            return null
        }
    }
}
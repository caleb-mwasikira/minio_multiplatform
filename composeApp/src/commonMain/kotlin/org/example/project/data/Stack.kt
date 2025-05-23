package org.example.project.data


class Stack<T : Any> {
    private val items = mutableListOf<T>()

    fun pushMany(vararg newItems: T): UInt {
        var numInserted = 0u
        for (newItem in newItems) {
            val result = push(newItem)
            if (!result) {
                println("Error pushing new item to stack")
                break
            }
            numInserted++
        }
        return numInserted
    }

    fun push(newItem: T?): Boolean {
        newItem?.let {
            return items.add(newItem)
        }
        return false
    }

    fun pop(): T? {
        return items.removeLastOrNull()
    }

    fun isNotEmpty(): Boolean {
        return items.isNotEmpty()
    }

    fun clear() {
        return items.clear()
    }
}
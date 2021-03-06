package com.epam.drill.admin.agent

import com.epam.drill.admin.common.serialization.*
import io.ktor.util.*
import kotlinx.serialization.*


typealias MessageType = com.epam.drill.common.MessageType

typealias Message = com.epam.drill.common.Message

sealed class AgentMessage {
    abstract val type: MessageType
    abstract val destination: String

    abstract val text: String
    abstract val bytes: ByteArray
}

@Serializable
data class JsonMessage(
    override val type: MessageType,
    override val destination: String = "",
    override val text: String = ""
) : AgentMessage() {
    override val bytes: ByteArray get() = text.decodeBase64Bytes()

    override fun toString() = "Json(type=$type,destination=$destination,text=$text)"
}

class BinaryMessage(
    val message: Message
) : AgentMessage() {
    override val type: MessageType get() = message.type
    override val destination: String get() = message.destination

    override val text get() = bytes.decodeToString()
    override val bytes get() = message.data

    override fun toString() = "Binary(type=$type,destination=$destination,size=${bytes.size})"
}

fun Any.toJsonMessage(topic: String): JsonMessage = JsonMessage(
    type = MessageType.MESSAGE,
    destination = topic,
    text = this as? String ?: jsonStringify()
)

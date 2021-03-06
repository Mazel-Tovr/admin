package com.epam.drill.admin.plugins.coverage

import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.serialization.builtins.*

@Suppress("unused")
class TestAgentPart constructor(
    id: String,
    agentContext: AgentContext
) : AgentPart<String, String>(id, agentContext) {

    override val serDe: SerDe<String> = SerDe(String.serializer())

    override fun on() {
        send("xx")
    }

    override fun off() {
    }

    override val confSerializer: kotlinx.serialization.KSerializer<String>
        get() = TODO()

    override fun initPlugin() {
        println("Plugin $id initialized.")
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        TODO()
    }

    override suspend fun doAction(action: String): Any {
        println(action)
        return "action"
    }
}

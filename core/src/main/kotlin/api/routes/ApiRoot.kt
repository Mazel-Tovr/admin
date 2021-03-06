@file:Suppress("unused")

package com.epam.drill.admin.api.routes

import de.nielsfalk.ktor.swagger.version.shared.*
import io.ktor.locations.*

@Location("/{prefix}")
class ApiRoot(val prefix: String = "api") {
    companion object {
        const val SYSTEM = "System operations"
        const val AGENT = "Agent Endpoints"
        const val AGENT_PLUGIN = "Agent Plugin Endpoints"
        const val SERVICE_GROUP = "Service Group Endpoints"
    }

    @Group(SYSTEM)
    @Location("/login")
    data class Login(val parent: ApiRoot)

    @Group(SYSTEM)
    @Location("/version")
    data class Version(val parent: ApiRoot)

    @Group(AGENT)
    @Location("/agents")
    data class Agents(val parent: ApiRoot) {

        @Group(AGENT)
        @Location("/{agentId}")
        data class Agent(val parent: Agents, val agentId: String)

        @Group(AGENT)
        @Location("/{agentId}/info")
        data class AgentInfo(val parent: Agents, val agentId: String)

        @Group(AGENT)
        @Location("/{agentId}/toggle")
        data class ToggleAgent(val parent: Agents, val agentId: String)

        @Group(AGENT)
        @Location("/{agentId}/logging")
        data class AgentLogging(val parent: Agents, val agentId: String)

        @Group(AGENT)
        @Location("/{agentId}/system-settings")
        data class SystemSettings(val parent: Agents, val agentId: String)

        @Group(AGENT_PLUGIN)
        @Location("/{agentId}/plugins")
        data class Plugins(val parent: Agents, val agentId: String)

        @Group(AGENT_PLUGIN)
        @Location("/{agentId}/plugins/{pluginId}")
        data class Plugin(val parent: Agents, val agentId: String, val pluginId: String)

        @Group(AGENT_PLUGIN)
        @Location("/{agentId}/plugins/{pluginId}/dispatch-action")
        data class DispatchPluginAction(val parent: Agents, val agentId: String, val pluginId: String)

        @Group(AGENT_PLUGIN)
        @Location("/{agentId}/plugins/{pluginId}/toggle")
        data class TogglePlugin(val parent: Agents, val agentId: String, val pluginId: String)

        @Group(AGENT_PLUGIN)
        @Location("/{agentId}/plugins/{pluginId}/data/{dataType}")
        data class PluginData(val parent: Agents, val agentId: String, val pluginId: String, val dataType: String)
    }

    @Group(SERVICE_GROUP)
    @Location("/service-groups/{serviceGroupId}")
    data class ServiceGroup(val parent: ApiRoot, val serviceGroupId: String) {
        @Group(SERVICE_GROUP)
        @Location("/system-settings")
        data class SystemSettings(val parent: ServiceGroup)

        @Group(SERVICE_GROUP)
        @Location("/plugins")
        data class Plugins(val parent: ServiceGroup)

        @Group(SERVICE_GROUP)
        @Location("/plugins/{pluginId}")
        data class Plugin(val parent: ServiceGroup, val pluginId: String) {
            @Group(SERVICE_GROUP)
            @Location("/dispatch-action")
            data class DispatchAction(val parent: Plugin)

            @Group(SERVICE_GROUP)
            @Location("/data/{dataType}")
            data class Data(val parent: Plugin, val dataType: String)
        }
    }
}

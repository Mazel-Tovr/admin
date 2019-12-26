package com.epam.drill.endpoints


import com.epam.drill.admin.servicegroup.*
import com.epam.drill.agentmanager.*
import com.epam.drill.common.*
import com.epam.drill.endpoints.agent.*
import com.epam.drill.plugins.*
import com.epam.drill.router.*
import com.epam.drill.storage.*
import com.epam.drill.util.*
import io.ktor.application.*
import kotlinx.coroutines.*
import org.kodein.di.*
import org.kodein.di.generic.*


class ServerWsTopics(override val kodein: Kodein) : KodeinAware {
    private val wsTopic: WsTopic by instance()
    private val serviceGroupManager: ServiceGroupManager by instance()
    private val agentManager: AgentManager by instance()
    private val plugins: Plugins by instance()
    private val app: Application by instance()
    private val sessionStorage: SessionStorage by instance()
    private val notificationsManager: NotificationsManager by instance()

    init {

        runBlocking {
            agentManager.agentStorage.onUpdate += update(mutableSetOf()) { storage ->
                val destination = app.toLocation(WsRoutes.GetAllAgents())
                sessionStorage.sendTo(
                    destination,
                    storage.values.map { it.agent }.sortedWith(compareBy(AgentInfo::id)).toMutableSet()
                        .toAgentInfosWebSocket(agentManager)
                )

            }
            agentManager.agentStorage.onAdd += add(mutableSetOf()) { k, v ->
                val destination = app.toLocation(WsRoutes.GetAgent(k))
                if (sessionStorage.exists(destination)) {
                    sessionStorage.sendTo(
                        destination,
                        v.agent.toAgentInfoWebSocket(agentManager)
                    )

                }
            }

            agentManager.agentStorage.onRemove += remove(mutableSetOf()) { k ->
                val destination = app.toLocation(WsRoutes.GetAgent(k))
                if (sessionStorage.exists(destination))
                    sessionStorage.sendTo(destination, "", WsMessageType.DELETE)
            }

            wsTopic {
                topic<WsRoutes.GetAllAgents> {
                    agentManager.agentStorage.values.map { it.agent }.sortedWith(compareBy(AgentInfo::id))
                        .toMutableSet()
                        .toAgentInfosWebSocket(agentManager)

                }

                topic<WsRoutes.ServiceGroup> { (groupId) -> serviceGroupManager[groupId] }

                topic<WsRoutes.GetAgent> { (agentId) ->
                    agentManager.getOrNull(agentId)?.toAgentInfoWebSocket(agentManager)
                }

                topic<WsRoutes.GetAgentBuilds> { payload ->
                    agentManager.adminData(payload.agentId).buildManager.buildVersionsJson
                }

                topic<WsRoutes.GetAllPlugins> {
                    plugins.map { (_, dp) -> dp.pluginBean }
                        .toAllPluginsWebSocket(agentManager.agentStorage.values.map { it.agent }.toMutableSet())
                }

                topic<WsRoutes.GetPluginInfo> { payload ->
                    val installedPluginBeanIds = agentManager
                        .getAllInstalledPluginBeanIds(payload.agentId)
                    plugins.getAllPluginBeans().map { plug ->
                        val pluginWebSocket = plug.toPluginWebSocket()
                        if (plug partOf installedPluginBeanIds) {
                            pluginWebSocket.relation = "Installed"
                        }
                        pluginWebSocket
                    }
                }

                topic<WsRoutes.GetPluginConfig> { payload ->
                    agentManager.getOrNull(payload.agent)?.plugins?.find { it.id == payload.plugin }
                        ?.config?.let { json.parseJson(it) } ?: ""
                }

                topic<WsRoutes.GetNotifications> {
                    notificationsManager.allNotifications.sortedByDescending { it.date }
                }

                topic<WsRoutes.GetBuilds> { (agentId) ->
                    agentManager.adminData(agentId).buildManager.summaries.sortedByDescending { it.addedDate }
                }
            }

        }
    }
}

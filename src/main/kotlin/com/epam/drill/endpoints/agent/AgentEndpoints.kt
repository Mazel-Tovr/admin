package com.epam.drill.endpoints.agent


import com.epam.drill.agentmanager.*
import com.epam.drill.common.*
import com.epam.drill.endpoints.*
import com.epam.drill.router.*
import com.epam.drill.util.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.*
import org.kodein.di.*
import org.kodein.di.generic.*

class AgentEndpoints(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentManager: AgentManager by instance()

    init {
        app.routing {

            authenticate {
                post<Routes.Api.UpdateAgentConfig> { (agentId) ->
                    if (agentManager.agentSession(agentId) != null) {
                        val au = call.parse(AgentInfoWebSocketSingle.serializer())
                        agentManager.updateAgent(agentId, au)
                        call.respond(HttpStatusCode.OK, "agent '$agentId' was updated")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "agent '$agentId' not found")
                    }
                }
            }

            authenticate {
                post<Routes.Api.Agent.RegisterAgent> { ll ->
                    val agentId = ll.agentId
                    val agInfo = agentManager[agentId]
                    if (agInfo != null) {
                        val regInfo = call.parse(AgentRegistrationInfo.serializer())
                        val bv = agInfo.buildVersion
                        val alias = regInfo.buildAlias
                        val au = AgentInfoWebSocketSingle(
                            id = agentId,
                            name = regInfo.name,
                            group = regInfo.group,
                            status = AgentStatus.ONLINE,
                            description = regInfo.description,
                            buildVersion = bv,
                            buildAlias = alias,
                            buildVersions = agInfo.buildVersions.toHashSet()
                        ).apply {
                            val oldVersion = buildVersions.find { it.id == bv }
                            if (oldVersion != null) {
                                oldVersion.name = alias
                            } else {
                                buildVersions.add(AgentBuildVersionJson(bv, alias))
                            }
                        }
                        agInfo.apply {
                            name = au.name
                            groupName = au.group
                            description = au.description
                            buildAlias = au.buildVersions.firstOrNull { it.id == this.buildVersion }?.name ?: ""
                            buildVersions.replaceAll(au.buildVersions)
                            status = au.status
                        }
                        agentManager.sync(agInfo, true)
                        call.respond(HttpStatusCode.OK, "Agent '$agentId' has been registered")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Agent '$agentId' not found")
                    }
                }
            }

            authenticate {
                post<Routes.Api.Agent.UnregisterAgent> { ll ->
                    val agentId = ll.agentId
                    val agInfo = agentManager[agentId]
                    if (agInfo != null) {
                        agentManager.resetAgent(agInfo)
                        call.respond(HttpStatusCode.OK, "Agent '$agentId' has been unregistered")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Agent '$agentId' not found")
                    }
                }
            }
        }
    }
}

@Serializable
data class AgentRegistrationInfo(
    val name: String,
    val description: String,
    val buildAlias: String,
    val group: String = ""
)
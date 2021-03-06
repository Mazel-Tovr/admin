package com.epam.drill.admin.service

import com.epam.drill.admin.api.agent.*
import com.epam.drill.admin.api.routes.*
import com.epam.drill.admin.endpoints.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.*
import mu.*
import org.kodein.di.*
import org.kodein.di.generic.*

const val agentIsBusyMessage =
    "Sorry, this agent is busy at the moment. Please try again later"
internal class RequestValidator(override val kodein: Kodein) : KodeinAware {
    private val logger = KotlinLogging.logger { }

    val app by instance<Application>()
    private val am by instance<AgentManager>()

    init {
        app.routing {
            intercept(ApplicationCallPipeline.Call) {
                if (context is RoutingApplicationCall) {
                    val agentId = context.parameters["agentId"]



                    if (agentId != null) {
                        val agentInfo = am.getOrNull(agentId)
                        when(agentInfo?.status) {
                            null -> {
                                if (am.allEntries().none { it.agent.serviceGroup == agentId }) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ValidationResponse("Agent '$agentId' not found")
                                    )
                                    return@intercept finish()
                                }
                            }
                            AgentStatus.BUSY -> {
                                val agentPath = locations.href(
                                    ApiRoot().let(ApiRoot::Agents).let { ApiRoot.Agents.Agent(it, agentId) }
                                )
                                with(context.request) {
                                    if (path() != agentPath && httpMethod != HttpMethod.Post) {
                                        logger.info { "Agent status is busy" }

                                        call.respond(
                                            HttpStatusCode.BadRequest,
                                            ValidationResponse(agentIsBusyMessage)
                                        )
                                        return@intercept finish()
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

}

@Serializable
data class ValidationResponse(val message: String)

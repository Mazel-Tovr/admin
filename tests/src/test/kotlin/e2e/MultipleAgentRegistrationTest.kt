package com.epam.drill.admin.e2e

import com.epam.drill.admin.api.agent.*
import com.epam.drill.e2e.*
import io.kotlintest.*
import io.ktor.http.*
import kotlin.test.*


class MultipleAgentRegistrationTest : E2ETest() {

    private val agentIdPrefix = "parallelRegister"

    @Test
    fun `4 Agents should be registered in parallel`() {
        createSimpleAppWithUIConnection {
            repeat(4) {
                connectAgent(AgentWrap("$agentIdPrefix$it", "0.1.$it")) { ui, agent ->
                    ui.getAgent()?.status shouldBe AgentStatus.NOT_REGISTERED
                    register("$agentIdPrefix$it") { status, _ ->
                        status shouldBe HttpStatusCode.OK
                    }
                    ui.getAgent()?.status shouldBe AgentStatus.BUSY
                    agent.`get-set-packages-prefixes`()
                    agent.`get-load-classes-datas`()
                    ui.getAgent()?.status shouldBe AgentStatus.ONLINE
                }
            }
        }
    }

}

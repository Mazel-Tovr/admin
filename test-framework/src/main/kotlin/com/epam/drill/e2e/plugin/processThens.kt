@file:Suppress("UNCHECKED_CAST")

package com.epam.drill.e2e.plugin

import com.epam.drill.admin.api.agent.*
import com.epam.drill.e2e.*
import kotlinx.coroutines.*
import org.apache.bcel.classfile.*
import java.io.*
import kotlin.reflect.*
import kotlin.test.*

fun AdminTest.processThens(
    psClass: KClass<out PluginStreams>,
    thens: MutableList<ThenAgentAsyncStruct>,
    pluginId: String,
    agentStreamDebug: Boolean,
    ui: AdminUiChannels,
    pluginMeta: com.epam.drill.common.PluginMetadata,
    globLaunch: Job
) {
    thens.forEach { (ag, build, it) ->
        val classes = File("./build/classes/java/${build.name}")
            .walkTopDown()
            .filter { it.extension == "class" }
            .toList()
            .toTypedArray()
        engine.handleWebSocketConversation("/ws/plugins/${pluginMeta.id}?token=${globToken}") { uiIncoming, ut ->
            val st = psClass.java.constructors.single().newInstance() as PluginStreams
            val pluginTestInfo = PluginTestContext(
                ag.id,
                pluginId,
                ag.buildVersion,
                globToken,
                classes.size,
                engine,
                asyncEngine.context
            )
            st.info = pluginTestInfo
            st.app = engine.application
            with(st) { queued(uiIncoming, ut) }
            engine.handleWebSocketConversation(
                "/agent/attach",
                wsRequestRequiredParams(ag)
            ) { inp, out ->
                val apply = Agent(
                    application,
                    ag.id,
                    inp,
                    out,
                    agentStreamDebug
                ).apply { queued() }
                apply.getHeaders()
                apply.`get-set-packages-prefixes`()
                val bcelClasses = classes.map {
                    it.inputStream().use { fs -> ClassParser(fs, "").parse() }
                }
                val classMap: Map<String, ByteArray> = bcelClasses.associate {
                    it.className.replace(".", "/") to it.bytes
                }
                loadPlugin(
                    apply,
                    ag,
                    classMap,
                    pluginId,
                    agentStreamDebug,
                    out,
                    st,
                    pluginTestInfo,
                    pluginMeta,
                    build,
                    true
                )
                for (i in 1..3) {
                    if (ui.getAgent()?.status == AgentStatus.BUSY) {
                        break
                    }
                }
                assertEquals(AgentStatus.ONLINE, ui.getAgent()?.status)
                it(pluginTestInfo, st, build)
                while (globLaunch.isActive)
                    delay(100)
            }
        }
    }
}

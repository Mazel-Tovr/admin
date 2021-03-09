/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.admin.plugin

import com.epam.drill.admin.api.websocket.*
import com.epam.drill.admin.cache.*
import com.epam.drill.admin.cache.impl.*
import com.epam.drill.admin.cache.type.*
import com.epam.drill.admin.config.*
import com.epam.drill.admin.plugins.*
import com.epam.drill.admin.store.*
import com.epam.drill.admin.websocket.*
import io.ktor.application.*
import io.ktor.config.*
import mu.*

class PluginCaches(
    app: Application,
    private val cacheService: CacheService,
    private val plugins: Plugins,
    private val pluginStores: PluginStores
) {
    private val logger = KotlinLogging.logger {}

    private val config: ApplicationConfig = app.drillConfig.config("cache")

    private val enabled: Boolean = false//config.propertyOrNull("enabled")?.getString()?.toBoolean() ?: true

    init {
        logger.info { "cache.enabled=$enabled" }
        println("CACHE IS DISABLE")
    }

    internal fun get(
        pluginId: String,
        subscription: Subscription?,
        replace: Boolean = false
    ): Cache<Any, FrontMessage> = if (enabled) {
        cacheService.pluginCacheFor(subscription, pluginId, replace)
    } else NullCache.castUnchecked()

    //TODO aggregate plugin data
    internal suspend fun getData(
        agentId: String,
        buildVersion: String,
        type: String
    ): Any? = plugins.keys.firstOrNull()?.let { pluginId ->
        retrieveMessage(
            pluginId,
            AgentSubscription(agentId, buildVersion),
            "/data/$type"
        )
    }.takeIf { it != "" }

    internal suspend fun retrieveMessage(
        pluginId: String,
        subscription: Subscription?,
        destination: String
    ): FrontMessage = get(pluginId, subscription).let { cache ->
        cache[destination] ?: run {
            val messageKey = subscription.toKey(destination)
            val classLoader = plugins[pluginId]?.run {
                pluginClass.classLoader
            } ?: Thread.currentThread().contextClassLoader
            val messageFromStore = pluginStores[pluginId].readMessage(messageKey, classLoader) ?: ""
            messageFromStore.also { cache[destination] = it }
        }
    }
}

class PluginSessions(plugins: Plugins) {

    private val sessionCaches: Map<String, SessionStorage> = plugins.mapValues { SessionStorage() }

    operator fun get(pluginId: String): SessionStorage = sessionCaches.getValue(pluginId)
}

private data class AgentKey(val pluginId: String, val agentId: String)
private data class GroupKey(val pluginId: String, val groupId: String)

private fun CacheService.pluginCacheFor(
    subscription: Subscription?,
    pluginId: String,
    replace: Boolean
): Cache<Any, FrontMessage> = when (subscription) {
    is AgentSubscription -> getOrCreate(
        id = AgentKey(pluginId, subscription.agentId),
        qualifier = subscription.buildVersion ?: "",
        replace = replace
    )
    is GroupSubscription -> getOrCreate(GroupKey(pluginId, subscription.groupId))
    null -> getOrCreate(pluginId)
}

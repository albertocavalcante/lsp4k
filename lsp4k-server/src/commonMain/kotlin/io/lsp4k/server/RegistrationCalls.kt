package io.lsp4k.server

import io.lsp4k.protocol.LspMethods
import io.lsp4k.protocol.Registration
import io.lsp4k.protocol.RegistrationParams
import io.lsp4k.protocol.Unregistration
import io.lsp4k.protocol.UnregistrationParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlin.concurrent.Volatile
import kotlin.time.Duration

/**
 * Dynamic capability registration (`client/registerCapability` and `client/unregisterCapability`).
 *
 * Register only capabilities whose client capability sets `dynamicRegistration`; read them from
 * [LanguageServer.initializeParams]. [register] generates a unique registration id when none is given.
 */
public class RegistrationCalls internal constructor(
    private val sender: ClientSender,
) {
    /** Sends `client/registerCapability`. */
    public suspend fun registerCapability(
        params: RegistrationParams,
        timeout: Duration = DEFAULT_REQUEST_TIMEOUT,
    ) {
        sender.request(LspMethods.CLIENT_REGISTER_CAPABILITY, params, RegistrationParams.serializer(), timeout)
    }

    /** Sends `client/unregisterCapability`. */
    public suspend fun unregisterCapability(
        params: UnregistrationParams,
        timeout: Duration = DEFAULT_REQUEST_TIMEOUT,
    ) {
        sender.request(LspMethods.CLIENT_UNREGISTER_CAPABILITY, params, UnregistrationParams.serializer(), timeout)
    }

    /** Registers [method] with raw [registerOptions] and returns a handle that can unregister it. */
    public suspend fun register(
        method: String,
        registerOptions: JsonElement? = null,
        id: String? = null,
    ): CapabilityRegistration {
        val registration = Registration(id ?: sender.nextId("lsp4k-registration"), method, registerOptions)
        registerCapability(RegistrationParams(listOf(registration)))
        return CapabilityRegistration(registration.id, method, this)
    }

    /** Registers [method] with [registerOptions] encoded by [serializer] and returns a handle that can unregister it. */
    public suspend fun <T> register(
        method: String,
        registerOptions: T,
        serializer: KSerializer<T>,
        id: String? = null,
    ): CapabilityRegistration = register(method, wireJson.encodeToJsonElement(serializer, registerOptions), id)
}

/**
 * A capability registered with [RegistrationCalls.register].
 *
 * @property id The registration id.
 * @property method The registered method.
 */
public class CapabilityRegistration internal constructor(
    public val id: String,
    public val method: String,
    private val calls: RegistrationCalls,
) {
    @Volatile
    private var registered = true

    /** Whether [unregister] has not completed yet. */
    public val isRegistered: Boolean get() = registered

    /** Sends `client/unregisterCapability` for this registration; later calls do nothing. */
    public suspend fun unregister() {
        if (!registered) return
        calls.unregisterCapability(UnregistrationParams(listOf(Unregistration(id, method))))
        registered = false
    }
}

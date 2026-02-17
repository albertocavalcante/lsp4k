package io.lsp4k.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * General parameters to register for a capability.
 */
@Serializable
public data class Registration(
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again.
     */
    val id: String,
    /**
     * The method / capability to register for.
     */
    val method: String,
    /**
     * Options necessary for the registration.
     */
    val registerOptions: JsonElement? = null,
)

/**
 * Parameters for the client/registerCapability request.
 */
@Serializable
public data class RegistrationParams(
    /**
     * The registrations.
     */
    val registrations: List<Registration>,
)

/**
 * General parameters to unregister a capability.
 */
@Serializable
public data class Unregistration(
    /**
     * The id used to unregister the request or notification.
     */
    val id: String,
    /**
     * The method / capability to unregister for.
     */
    val method: String,
)

/**
 * Parameters for the client/unregisterCapability request.
 */
@Serializable
public data class UnregistrationParams(
    /**
     * The unregistrations.
     * Note: The JSON property name is "unregisterations" due to a typo in the LSP specification.
     */
    @SerialName("unregisterations")
    val unregistrations: List<Unregistration>,
)

/**
 * A configuration item as part of a configuration request.
 */
@Serializable
public data class ConfigurationItem(
    /**
     * The scope to get the configuration section for.
     */
    val scopeUri: String? = null,
    /**
     * The configuration section asked for.
     */
    val section: String? = null,
)

/**
 * Parameters for the workspace/configuration request.
 */
@Serializable
public data class ConfigurationParams(
    /**
     * The configuration items to ask for.
     */
    val items: List<ConfigurationItem>,
)

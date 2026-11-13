package io.lsp4k.server

import io.lsp4k.protocol.InitializeParams
import io.lsp4k.protocol.PositionEncodingKind

/** The server preference used when none is given: byte offsets first, then code points, then the mandatory UTF-16. */
public val DEFAULT_POSITION_ENCODING_PREFERENCE: List<PositionEncodingKind> =
    listOf(PositionEncodingKind.UTF8, PositionEncodingKind.UTF32, PositionEncodingKind.UTF16)

/**
 * Picks the position encoding for a session.
 *
 * The first entry of [serverPreference] that the client lists in `general.positionEncodings` ([clientOffered]) wins.
 * UTF-16 is the protocol default that every client and server must support, so it is the result when the client
 * lists nothing (null or empty) and when no preferred encoding is offered. A server that supports only some
 * encodings passes just those; UTF-16 is still the fallback.
 */
public fun negotiatePositionEncoding(
    clientOffered: List<PositionEncodingKind>?,
    serverPreference: List<PositionEncodingKind> = DEFAULT_POSITION_ENCODING_PREFERENCE,
): PositionEncodingKind {
    if (clientOffered.isNullOrEmpty()) return PositionEncodingKind.UTF16
    return serverPreference.firstOrNull { it in clientOffered } ?: PositionEncodingKind.UTF16
}

/** Negotiates the position encoding from the client capabilities of these `initialize` parameters. */
public fun InitializeParams.negotiatePositionEncoding(
    serverPreference: List<PositionEncodingKind> = DEFAULT_POSITION_ENCODING_PREFERENCE,
): PositionEncodingKind = negotiatePositionEncoding(capabilities.general?.positionEncodings, serverPreference)

/**
 * Negotiates the position encoding in the default `initialize` result: the first of [preference] that the client
 * lists in `general.positionEncodings` is advertised as `positionEncoding`, UTF-16 when none matches.
 * Read the outcome from [LanguageServer.positionEncoding]. A handler given to
 * [LanguageServerBuilder.onInitialize] builds its own result; negotiate there with [InitializeParams.negotiatePositionEncoding].
 */
public fun LanguageServerBuilder.negotiatePositionEncoding(preference: List<PositionEncodingKind>) {
    positionEncodingPreference = preference.ifEmpty { DEFAULT_POSITION_ENCODING_PREFERENCE }
}

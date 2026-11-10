package es.igarciadelamo.txoka.domain.profile

/**
 * What the device does on the next NFC tap.
 *
 * Two devices both polling and both publishing cannot decide which one is the card, so the role is
 * explicit. It is session scoped: it lives in memory and starts on [Send] every launch.
 */
enum class ShareRole {
    /** Advertises the card over HCE and still polls, so a return card can be read. */
    Send,

    /** Only polls, so the other phone always wins as the card. */
    Receive,
}

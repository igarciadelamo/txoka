package es.igarciadelamo.txoka.domain.protocol

/**
 * Whether the card still travels in a Type 4 Tag file once encoded as vCard. Above the limit HCE
 * advertises nothing, so writers ask here first instead of discovering it when a phone is tapped.
 */
fun VCardPayload.fitsInTag(): Boolean = NdefMessage.fits(VCardSerializer.encode(this))

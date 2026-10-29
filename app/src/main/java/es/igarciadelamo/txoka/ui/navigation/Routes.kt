package es.igarciadelamo.txoka.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ShareRoute

@Serializable
data object ReceivedRoute

@Serializable
data class ReceivedDetailRoute(val id: Long)

@Serializable
data object SettingsRoute

@Serializable
data object ProfileEditorRoute

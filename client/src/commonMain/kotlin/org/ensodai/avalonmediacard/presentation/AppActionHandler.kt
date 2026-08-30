package org.ensodai.avalonmediacard.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.rpc.ActionRpcService
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.core.openUrl
import org.ensodai.avalonmediacard.presentation.navigation.NavigationAvalon

class AppActionHandler(
    private val actionRpcService: ActionRpcService,
    private val navigation: NavigationAvalon,
    private val dialogManager: DialogManager,
    private val scope: CoroutineScope,
    private val onStatusChange: (String) -> Unit
) {
    fun handleAction(action: Action) {
        when (action) {
            is ActionNavigate -> {
                when (val screen = action.screen) {
                    is Screen.Dashboard, is Screen.Movies, is Screen.TvShows, is Screen.Trends -> navigation.navigateToDashboard()
                    is Screen.PluginHome -> navigation.navigateToPluginHome(screen.pluginId)
                    is Screen.MovieDetails -> navigation.navigateToDetails(screen.key)
                    is Screen.TvShowDetails -> navigation.navigateToDetails(screen.key)
                    is Screen.Person -> navigation.navigateToPerson(screen.key, screen.personName)
                    is Screen.Dynamic -> navigation.navigateToDynamic(screen.screenId, screen.title, screen.params)
                    is Screen.MediaList -> navigation.navigateToMediaList(screen.key, screen.listType, screen.title)
                    is Screen.Integrations -> navigation.navigateToIntegrations()
                    Screen.MyCollection -> navigation.navigateToMyCollection()
                    is Screen.CustomList -> navigation.navigateToCustomList(screen.listId, screen.title)
                    is Screen.Search -> navigation.navigateToSearch(screen.initialQuery)
                    is Screen.Admin -> navigation.navigateToAdmin()
                    is Screen.Settings -> navigation.navigateToSettings()
                }
            }

            is ActionPlayVideo -> {
                onStatusChange("Playing video: ${action.title} -> ${action.url}")
            }

            is ActionOpenUrl -> {
                openUrl(action.url)
            }

            is ServerAction -> {
                scope.launch {
                    try {
                        actionRpcService.handleAction(action)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        onStatusChange("Execution error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

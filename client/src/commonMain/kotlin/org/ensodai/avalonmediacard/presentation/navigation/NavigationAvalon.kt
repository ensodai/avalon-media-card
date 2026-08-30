package org.ensodai.avalonmediacard.presentation.navigation

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.ui.navigation.Navigation
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen


class NavigationAvalon(
    private val navController: AvalonNavController<ScreenKey>
) : Navigation {
    override fun navigateToDashboard() {
        navController.navigate(ScreenKey(Screen.Dashboard))
    }

    override fun navigateToPluginHome(pluginId: String) {
        navController.navigate(ScreenKey(Screen.PluginHome(pluginId)))
    }

    override fun navigateToDetails(key: MediaKey) {
        navController.navigate(ScreenKey(Screen.Details(key)))
    }

    override fun navigateToPerson(key: MediaKey, personName: String) {
        navController.navigate(ScreenKey(Screen.Person(key, personName)))
    }

    override fun navigateToDynamic(screenId: String, title: String, params: Map<String, String>) {
        navController.navigate(ScreenKey(Screen.Dynamic(screenId, title, params)))
    }

    override fun navigateToMediaList(key: MediaKey, listType: String, title: String) {
        navController.navigate(ScreenKey(Screen.MediaList(key, listType, title)))
    }

    override fun navigateToIntegrations() {
        navController.navigate(ScreenKey(Screen.Integrations))
    }

    override fun navigateToSettings() {
        navController.navigate(ScreenKey(Screen.Settings))
    }

    override fun navigateToAdmin() {
        navController.navigate(ScreenKey(Screen.Admin))
    }

    override fun navigateToMyCollection() {
        navController.navigate(ScreenKey(Screen.MyCollection))
    }

    override fun navigateToCustomList(listId: kotlin.uuid.Uuid, title: String) {
        navController.navigate(ScreenKey(Screen.CustomList(listId, title)))
    }

    override fun navigateToSearch(initialQuery: String) {
        navController.navigate(ScreenKey(Screen.Search(initialQuery)))
    }

    override fun navigateTo(screen: Screen) {
        navController.navigate(ScreenKey(screen))
    }

    override fun navigateBack() {
        navController.pop()
    }
}

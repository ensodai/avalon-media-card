package org.ensodai.avalonmediacard.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.slot.GlobalManifest
import org.ensodai.avalonmediacard.contract.slot.ScreenManifest
import org.koin.core.annotation.Single

@Single
class GlobalManifestRepository(private val sduiRpcService: SduiRpcService) {
    private val logger = AppLogging.logger("GlobalManifestRepository")
    private val _manifest = MutableStateFlow<GlobalManifest?>(null)
    val manifest = _manifest.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    suspend fun refreshManifest() {
        try {
            val loadedManifest = sduiRpcService.getGlobalManifest()
            logger.d { "Global Manifest Loaded successfully! Available screen keys: ${loadedManifest.screens.keys}" }
            
            _manifest.value = loadedManifest
            _isLoaded.value = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "ERROR loading Global Manifest: ${e.message}" }
            // Even on error we might want to let the app load, just without skeleton
            _isLoaded.value = true
        }
    }

    fun getScreenManifest(screenName: String): ScreenManifest? {
        return _manifest.value?.screens?.get(screenName)
    }

    fun clear() {
        _manifest.value = null
        _isLoaded.value = false
    }
}

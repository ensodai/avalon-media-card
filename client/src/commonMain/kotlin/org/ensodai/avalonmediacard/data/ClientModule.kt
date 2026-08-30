package org.ensodai.avalonmediacard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.ensodai.avalonmediacard.contract.rpc.ActionRpcService
import org.ensodai.avalonmediacard.contract.rpc.AdminRpcService
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService
import org.ensodai.avalonmediacard.contract.rpc.PlaybackRpcService
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.rpc.TelemetryRpcService
import org.ensodai.avalonmediacard.contract.rpc.UserSettingsRpcService
import org.ensodai.avalonmediacard.data.rpc.ReconnectingActionRpcService
import org.ensodai.avalonmediacard.data.rpc.ReconnectingAdminRpcService
import org.ensodai.avalonmediacard.data.rpc.ReconnectingAuthRpcService
import org.ensodai.avalonmediacard.data.rpc.ReconnectingPlaybackRpcService
import org.ensodai.avalonmediacard.data.rpc.ReconnectingSduiRpcService
import org.ensodai.avalonmediacard.data.rpc.ReconnectingTelemetryRpcService
import org.ensodai.avalonmediacard.data.rpc.ReconnectingUserSettingsRpcService
import org.ensodai.avalonmediacard.data.rpc.RpcCallExecutor
import org.ensodai.avalonmediacard.data.rpc.RpcConnectionManager
import org.ensodai.avalonmediacard.presentation.DialogManager
import org.ensodai.avalonmediacard.presentation.telemetry.TelemetryTracker
import org.ensodai.avalonmediacard.presentation.telemetry.TelemetryTrackerImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("org.ensodai.avalonmediacard")
class AppClientModule {

    @Single
    fun dataStore() = createDataStore()

    @Single
    fun tokenStorage(datastore: DataStore<Preferences>) = TokenStorage(datastore)

    @Single
    fun appSettingsStorage(datastore: DataStore<Preferences>) = AppSettingsStorage(datastore)

    @Single
    fun rpcConnectionManager(tokenStorage: TokenStorage) = RpcConnectionManager(tokenStorage)

    @Single
    fun rpcCallExecutor(manager: RpcConnectionManager) = RpcCallExecutor(manager)

    @Single
    fun provideSduiRpcService(rpcConnectionManager: RpcConnectionManager, executor: RpcCallExecutor): SduiRpcService =
        ReconnectingSduiRpcService(rpcConnectionManager, executor)

    @Single
    fun provideActionRpcService(rpcConnectionManager: RpcConnectionManager, executor: RpcCallExecutor): ActionRpcService =
        ReconnectingActionRpcService(rpcConnectionManager, executor)

    @Single
    fun provideAdminRpcService(rpcConnectionManager: RpcConnectionManager, executor: RpcCallExecutor): AdminRpcService =
        ReconnectingAdminRpcService(rpcConnectionManager, executor)

    @Single
    fun provideAuthRpcService(rpcConnectionManager: RpcConnectionManager, executor: RpcCallExecutor): AuthRpcService =
        ReconnectingAuthRpcService(rpcConnectionManager, executor)

    @Single
    fun provideTelemetryRpcService(rpcConnectionManager: RpcConnectionManager, executor: RpcCallExecutor): TelemetryRpcService =
        ReconnectingTelemetryRpcService(rpcConnectionManager, executor)

    @Single
    fun providePlaybackRpcService(rpcConnectionManager: RpcConnectionManager, executor: RpcCallExecutor): PlaybackRpcService =
        ReconnectingPlaybackRpcService(rpcConnectionManager, executor)

    @Single
    fun provideUserSettingsRpcService(rpcConnectionManager: RpcConnectionManager, executor: RpcCallExecutor): UserSettingsRpcService =
        ReconnectingUserSettingsRpcService(rpcConnectionManager, executor)

    @Single
    fun telemetryTracker(telemetryRpcService: TelemetryRpcService): TelemetryTracker =
        TelemetryTrackerImpl(telemetryRpcService)

    @Single
    fun dialogManager() = DialogManager()
}

@org.koin.core.annotation.KoinApplication(modules = [AppClientModule::class])
object AppKoinConfig

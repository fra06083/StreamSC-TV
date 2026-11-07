package it.fra06083.streamsctv

import android.app.Application
import android.content.Context
import it.fra06083.streamsctv.data.ProfileStore
import it.fra06083.streamsctv.data.ProviderPrefs
import it.fra06083.streamsctv.ext.ExtensionManager
import it.fra06083.streamsctv.ext.ProviderRepository
import it.fra06083.streamsctv.net.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    lateinit var profiles: ProfileStore
        private set
    lateinit var providerPrefs: ProviderPrefs
        private set
    lateinit var extensions: ExtensionManager
        private set
    lateinit var repository: ProviderRepository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        Http.init(this)
        profiles = ProfileStore(this)
        providerPrefs = ProviderPrefs(this)
        extensions = ExtensionManager(this)
        repository = ProviderRepository(this, extensions)
        try {
            extensions.ensureBundledInstalled()
        } catch (_: Throwable) {
        }
    }
}

val Context.app: App get() = applicationContext as App
val androidx.fragment.app.Fragment.app: App get() = App.instance
val app: App get() = App.instance


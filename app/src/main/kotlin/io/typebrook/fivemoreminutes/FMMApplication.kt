package io.typebrook.fivemoreminutes

import android.app.Application
import com.facebook.stetho.Stetho
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
import io.realm.Realm
import io.typebrook.fmmcore.redux.MapMiddleware
import io.typebrook.fmmcore.redux.State
import io.typebrook.fmmcore.redux.reducer
import tw.geothings.rekotlin.Action
import tw.geothings.rekotlin.Store

/**
 * Created by pham on 2017/9/20.
 */

val mainStore = Store(
        reducer = ::reducer,
        state = State(),
        middleware = listOf(MapMiddleware()::handle)
)
val realm: Realm get() = Realm.getDefaultInstance()

infix fun Store<State>.dispatch(action: Action) = dispatchFunction(action)

class FMMApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Realm.init(this)
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build())
    }
}
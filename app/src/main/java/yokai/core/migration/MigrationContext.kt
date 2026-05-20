package yokai.core.migration


import org.koin.core.context.GlobalContext

class MigrationContext(val dryRun: Boolean) {

    inline fun <reified T : Any> get(): T? {
        return GlobalContext.get().getOrNull<T>()
    }
}

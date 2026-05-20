package yokai.util.koin

import org.koin.core.context.GlobalContext

inline fun <reified T : Any> injectLazy(
    qualifier: org.koin.core.qualifier.Qualifier? = null,
    noinline parameters: org.koin.core.parameter.ParametersDefinition? = null,
): Lazy<T> = lazy { GlobalContext.get().get<T>(qualifier, parameters) }

inline fun <reified T : Any> get(
    qualifier: org.koin.core.qualifier.Qualifier? = null,
    noinline parameters: org.koin.core.parameter.ParametersDefinition? = null,
): T = GlobalContext.get().get<T>(qualifier, parameters)

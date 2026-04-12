package exh.recs

import android.os.Bundle
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController

class RecommendsViewController(
    bundle: Bundle? = null,
) : BaseComposeController(bundle) {

    constructor(args: RecommendsScreen.Args) : this(
        Bundle().apply { putSerializable(ARGS_KEY, args) },
    )

    private val screenArgs: RecommendsScreen.Args
        get() = requireNotNull(args.getSerializable(ARGS_KEY) as? RecommendsScreen.Args)

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = RecommendsScreen(screenArgs),
        )
    }

    companion object {
        private const val ARGS_KEY = "recs_args"
    }
}

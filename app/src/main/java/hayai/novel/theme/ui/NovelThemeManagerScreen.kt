package hayai.novel.theme.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.core.enterAlwaysAppBarScrollBehavior
import yokai.util.Screen

/**
 * Voyager screen wrapping [NovelThemeManagerContent] inside the standard Yokai scaffold for
 * app-level navigation. The reader-side entry uses a bottom sheet that hosts the same content
 * directly without this scaffold.
 */
class NovelThemeManagerScreen : Screen() {

    @Composable
    override fun Content() {
        val onBackPress = LocalBackPress.currentOrThrow
        val screenModel = rememberScreenModel { NovelThemeManagerScreenModel() }

        YokaiScaffold(
            onNavigationIconClicked = onBackPress,
            title = "Reader themes",
            appBarType = AppBarType.SMALL,
            scrollBehavior = enterAlwaysAppBarScrollBehavior(),
        ) { innerPadding ->
            NovelThemeManagerContent(
                screenModel = screenModel,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

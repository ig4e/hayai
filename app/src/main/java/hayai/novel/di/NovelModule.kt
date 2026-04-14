package hayai.novel.di

import hayai.novel.plugin.NovelPluginManager
import hayai.novel.repo.NovelRepoRepositoryImpl
import hayai.novel.repo.NovelRepoRepository
import hayai.novel.repo.interactor.CreateNovelRepo
import hayai.novel.repo.interactor.DeleteNovelRepo
import hayai.novel.repo.interactor.GetNovelRepo
import android.app.Application
import org.koin.dsl.module

fun novelModule() = module {
    single<NovelRepoRepository> { NovelRepoRepositoryImpl(get()) }

    single { GetNovelRepo(get()) }
    single { CreateNovelRepo(get()) }
    single { DeleteNovelRepo(get()) }

    single {
        NovelPluginManager(
            context = get<Application>(),
            repoRepository = get(),
            networkHelper = get(),
        )
    }
}

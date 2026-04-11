package exh.di

import org.koin.dsl.module

fun exhModule() = module {
    // Preferences
    single { exh.source.ExhPreferences(get()) }
    single { exh.pref.DelegateSourcePreferences(get()) }

    // Metadata repository - wire these based on what the foundation agent creates
    // single<MangaMetadataRepository> { MangaMetadataRepositoryImpl(get()) }

    // EH Update Helper
    // single { EHentaiUpdateHelper(get(), get()) }
}

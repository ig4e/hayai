package exh.di

import org.koin.dsl.module
import yokai.data.manga.metadata.MangaMetadataRepositoryImpl
import yokai.domain.manga.metadata.MangaMetadataRepository

fun exhModule() = module {
    // Preferences
    single { exh.source.ExhPreferences(get()) }
    single { exh.pref.DelegateSourcePreferences(get()) }

    // Metadata repository
    single<MangaMetadataRepository> { MangaMetadataRepositoryImpl(get()) }

    // EH Update Helper
    single { exh.eh.EHentaiUpdateHelper(get()) }
}

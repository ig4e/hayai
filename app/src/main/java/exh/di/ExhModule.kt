package exh.di

import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.GetFlatMetadataByIdInteractor
import exh.metadata.GetMangaIdInteractor
import exh.metadata.InsertFlatMetadataInteractor
import org.koin.dsl.module
import yokai.data.manga.metadata.MangaMetadataRepositoryImpl
import yokai.domain.manga.metadata.MangaMetadataRepository

fun exhModule() = module {
    // Preferences
    single { exh.source.ExhPreferences(get()) }
    single { exh.pref.DelegateSourcePreferences(get()) }

    // Metadata repository
    single<MangaMetadataRepository> { MangaMetadataRepositoryImpl(get()) }

    // MetadataSource DI — resolved via Injekt.get() in MetadataSource interface
    factory<MetadataSource.GetMangaId> { GetMangaIdInteractor(get()) }
    factory<MetadataSource.InsertFlatMetadata> { InsertFlatMetadataInteractor(get()) }
    factory<MetadataSource.GetFlatMetadataById> { GetFlatMetadataByIdInteractor(get()) }

    // EH Update Helper
    single { exh.eh.EHentaiUpdateHelper(get()) }
}

package com.example.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Channel::class, Favorite::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tv_app_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val PRELOADED_CHANNELS = listOf(
            Channel(
                id = "ar_tn",
                name = "Todo Noticias (TN)",
                streamUrl = "https://live-tn.strm.madzen.com.ar/live/tn/chunks.m3u8",
                country = "Argentina",
                category = "Noticias",
                description = "Canal de televisión de noticias líder de Argentina. Transmisiones las 24 horas con la mejor cobertura informativa en vivo.",
                logoUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=120"
            ),
            Channel(
                id = "ar_c26",
                name = "Canal 26",
                streamUrl = "https://c26.strm.madzen.com.ar/live/c26/chunks.m3u8",
                country = "Argentina",
                category = "Noticias",
                description = "Información minuto a minuto de lo que sucede en Argentina y a nivel global. Reportajes, análisis y debate.",
                logoUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=120"
            ),
            Channel(
                id = "ar_tvp",
                name = "TV Pública",
                streamUrl = "https://television-publica.strm.madzen.com.ar/live/tvp/chunks.m3u8",
                country = "Argentina",
                category = "Nacional",
                description = "Emisora televisiva pública argentina. Programas educativos, deportivos, música folclórica y series nacionales.",
                logoUrl = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=120"
            ),
            Channel(
                id = "cr_multimedios",
                name = "Multimedios CR",
                streamUrl = "https://multimedios-cr.live.tbsc.mx/live/multimedioscr/playlist.m3u8",
                country = "Costa Rica",
                category = "General",
                description = "Canal internacional costarricense con variedad de entretenimiento, informativos estelares y debate deportivo.",
                logoUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=120"
            ),
            Channel(
                id = "cr_columbia",
                name = "Columbia TV",
                streamUrl = "https://tv.columbia.co.cr:1936/live/live/playlist.m3u8",
                country = "Costa Rica",
                category = "Noticias",
                description = "Columbia TV Costa Rica. Cobertura completa de la farándula nacional, noticias impactantes y deportes en vivo.",
                logoUrl = "https://images.unsplash.com/photo-1598257006458-087169a1f08d?w=120"
            ),
            Channel(
                id = "pe_usmptv",
                name = "USMP TV",
                streamUrl = "https://592f444855de9.streamlock.net/usmptv/smil:usmptv.smil/playlist.m3u8",
                country = "Perú",
                category = "Educativo",
                description = "Televisión de la Universidad de San Martín de Porres. Contenido académico interactivo, cultural e histórico peruano de alta calidad.",
                logoUrl = "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=120"
            )
        )
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.channelDao()
                    for (channel in PRELOADED_CHANNELS) {
                        dao.insertChannel(channel)
                    }
                }
            }
        }
    }
}

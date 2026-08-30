package org.ensodai.avalonmediacard.plugin.recommendation

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.TelemetryEvent
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.plugins.*
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.DashboardGenerator
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RealUserDatabaseTest {

    private val json = Json { ignoreUnknownKeys = true }

    // Realistic mock user preference vector
    private val rawVectorJson =
        """{"genreWeights":{"18":0.9999959087262524,"27":0.19230768742265134,"9648":0.12334752991495083,"10759":0.9999999998876807,"10765":0.9999999961568297},"directorWeights":{"3688382":0.9965232730632511,"2275503":0.40614210238542814,"1191094":0.024401259360465773,"489":0.8776893848814407,"936212":0.5582341060293121,"168309":0.11696779908157079,"3590481":0.7167645031545123,"28741":0.9252397728252376,"1505073":0.5369678003045593,"151172":0.4888355365380955,"12180":0.44340153245005026,"3122176":0.020962232689011685,"131770":0.43316937010781986,"2474506":0.3758875468920368,"19142":0.2435246269421879},"actorWeights":{"1512452":0.9358705744894377,"5294":0.7756269403477702,"45407":0.5592659853179298,"5725467":0.3665894478656909,"212934":0.23348334142326166,"1576786":0.14890366968239224,"64670":0.0959320653881712,"72482":0.062460291292004984,"1709431":0.04101685337866326,"4848549":0.027106550146434195,"2391":1.0,"52886":0.13194379927025418,"1656863":0.08526108806836699,"81839":0.053845311173206165,"1443478":0.03661866984988936,"76029":0.024233576717994122,"34985":0.016102891871785794,"140114":0.01073034811135503,"5916":0.04866723665384405,"64295":0.016035130172533038,"5614677":0.010685450319606835,"1896":0.25119100168971087,"29446":0.9999999999999913,"2957":0.9999999988532565,"9807":0.9999989030449133,"17782":0.9998379287320872,"74071":0.9953714188260755,"74070":0.9572303678717028,"822":0.8264265095976653,"5888":0.6127583793243854,"17628":0.5818054095825413,"113711":0.41720948113772194,"1583307":0.2633423645907971,"1243496":0.16980508987542817,"56614":0.10904430658084382,"20049":0.07028614848602283,"119759":0.14584640310615177,"166807":0.09401088687619798,"69574":0.05821157348842759,"122876":0.040227310026710046,"51965":0.02659133689102706,"69576":0.017655532642693105,"33355":0.01175853720490868,"207997":0.36924874387897033,"13526":0.23521157269243648,"17005":0.14691444272760262,"883":0.09661233264807985,"1218926":0.06289326402873788,"56455":0.04011375609986127,"56825":0.027288773986548137,"59713":0.018114368073306703,"117659":0.01206218080358329,"529003":0.03810896194409081,"20664":0.04376954499694652,"108895":0.07302412455319823,"6195":0.04781646270435388,"3118207":0.0315342944077977,"2520942":0.020903164539764336,"1562430":0.01390572557718445,"133212":0.0279082922107042,"1331023":0.25662077961453644,"1754577":0.1634191003735931,"88466":0.1050417014553251,"1649803":0.0682503194379201,"1226076":0.044747702139678255,"66497":0.019592767112798344,"79072":0.013039907292563237,"2995046":0.44611256081931,"1681375":0.2862196983348295,"1221867":0.18206648604613285,"40451":0.015585885039207987,"50668":0.6780745707162775,"51798":0.45057613724792006,"587":0.07797769743438657,"77209":0.033534634883677306,"1581":0.02236896070118369,"74423":0.014873337791439293,"20239":0.010779941568369885,"1980651":0.31709613522827934,"2191082":0.20165730081707217,"75129":0.1289822905424631,"1522920":0.08339403001893457,"228704":0.05446138995217265,"1203030":0.02372799486403556,"78962":0.01576965010949698,"113505":0.010260155910859374,"1246":0.09621307384986909,"540":0.050698060366882326,"133656":0.2870938914188489,"550869":0.1826189764937085,"129706":0.11706899850623942,"132947":0.0758702642571332,"577381":0.04964305737869112,"1110487":0.032720961923624343,"52716":0.021681369722672185,"1288294":0.014419555544522735,"83933":0.27700577626624084,"1241453":0.16895247516119172,"625":0.11505998970268463,"1765827":0.07459924271201109,"34923":0.029301065179567036,"12199":0.012937010927042384,"82541":0.12067294293581793,"1015824":0.07508483964670432,"5692794":0.013807395679197797,"2939203":0.16161874309003768,"1932292":0.06753369257711071,"1656170":0.04428647189101809,"968742":0.029237706894793013,"2939206":0.01939548890920604,"1921268":0.012909494725706029,"9145":0.29592718755149944,"77222":0.18820821623407505,"62909":0.12056722603315351,"54682":0.1456856530895415,"70827":0.09390984492629681,"86542":0.061172604375882636,"58161":0.04018575781208155,"20628":0.026564215795958145,"1077916":0.01763768593940789,"70830":0.011746724896309646,"1228214":0.05511934324980053,"1744731":0.03627195032958569,"1149338":0.02400678328025188,"1060570":0.015953419970637348,"1496302":0.01063130772202423},"eraWeights":{"1990s":0.9999999741546164},"recentWatchedIds":["1083381","71365","580"],"sessionBingeVector":{"28":0.8,"35":0.6,"53":0.4,"80":0.2,"16":0.8,"878":1.0,"10770":0.4,"27":0.2,"9648":0.2}}"""

    @Test
    fun `test Visual Dashboard Generation for Realistic User Profile`() = runBlocking {
        val realVector = json.decodeFromString(AffinityVector.serializer(), rawVectorJson)

        // Создаем контекст и генератор
        val pluginContext = createRealUserContext()
        val generator = DashboardGenerator(pluginContext)

        // Генерируем полки рекомендаций
        val sections = generator.generate(realVector)

        println("\n=======================================================================")
        println(" 🚀 ДАШБОРД РЕКОМЕНДАЦИЙ (ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ)")
        println("=======================================================================")
        println(" 🛸 Любимые жанры: ${realVector.genreWeights}")
        println(" 🎭 Любимые актеры (Топ-5): ${realVector.actorWeights.entries.sortedByDescending { it.value }.take(5)}")
        println(
            " 📽️ Любимые режиссеры (Топ-3): ${
                realVector.directorWeights.entries.sortedByDescending { it.value }.take(3)
            }"
        )
        println(" 📌 Всего сформировано полок: ${sections.size}")
        println("-----------------------------------------------------------------------")
        sections.forEachIndexed { i, section ->
            println("[Полка #${i + 1}] [${section.type}] \"${section.title}\"")
            println("   📝 Описание: ${section.description}")
            println("   🎯 TMDB Params: ${section.queryParams}")
            println("   ⭐ Relevance Score: ${section.weight}")
            println()
        }
        println("=======================================================================\n")
    }

    private fun createRealUserContext(): PluginContext {
        val catalog = FileSystemMediaCatalog(json)

        return PluginContext(
            pluginDir = ".",
            logger = DefaultPluginLogger("RealUserDbTest"),
            httpClient = HttpClient(MockEngine { respondOk("") }),
            catalog = catalog,
            userMovies = object : UserMovieProvider {
                override suspend fun getUserMovies(userId: Uuid): List<UserMovieItem> =
                    emptyList()

                override fun observeUserMovies(userId: Uuid): Flow<List<UserMovieItem>> =
                    emptyFlow()

                override suspend fun updateUserMovie(item: UserMovieItem): Boolean =
                    false

                override suspend fun deleteUserMovie(userId: Uuid, mediaId: String): Boolean = false
                override suspend fun getUserEpisodes(
                    userId: Uuid,
                    mediaId: String
                ): List<UserEpisodeItem> = emptyList()

                override suspend fun updateUserEpisode(item: UserEpisodeItem): Boolean =
                    false

                override suspend fun notifyUpdate() {}
            },
            userCustomLists = object : UserCustomListProvider {
                override fun observeUserLists(userId: Uuid): Flow<List<CustomListInfo>> = emptyFlow()
                override fun observeListItems(listId: Uuid): Flow<List<MediaKey>> = emptyFlow()
                override suspend fun getCustomListsWithStatus(
                    userId: Uuid,
                    mediaKey: MediaKey
                ): List<CustomListStatus> = emptyList()

                override suspend fun toggleList(userId: Uuid, listId: String, mediaKey: MediaKey) {}
                override suspend fun createList(userId: Uuid, listName: String, mediaKey: MediaKey) {}
            },
            userEpisodes = object : UserEpisodeProvider {
                override suspend fun getEpisodesProgress(
                    userId: Uuid,
                    mediaId: String,
                    catalogId: String
                ): List<UserEpisodeProgress> = emptyList()

                override suspend fun saveEpisodeProgress(
                    userId: Uuid,
                    catalogId: String,
                    mediaId: String,
                    season: Int,
                    episode: Int,
                    progressSeconds: Long,
                    durationSeconds: Long,
                    isWatched: Boolean
                ) {
                }
            },
            torrentMappings = object : TorrentMappingProvider {
                override suspend fun getMappingsByHash(torrentHash: String): List<TorrentMapping> = emptyList()
                override suspend fun getMappingsByMediaId(mediaId: String): List<TorrentMapping> = emptyList()
                override suspend fun saveMapping(
                    torrentHash: String,
                    filePath: String,
                    seasons: List<Int>?,
                    episodes: List<Int>?,
                    isAbsolute: Boolean,
                    isManual: Boolean,
                    mediaId: String?,
                    fileIndex: Int?,
                    fileSize: Long?
                ): TorrentMapping = TODO()

                override suspend fun clearMappingsByMediaId(mediaId: String) {}
            },
            settings = object : PluginSettings {
                override suspend fun getString(key: String): String? = null
                override suspend fun setString(key: String, value: String) {}
                override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = false
                override suspend fun setBoolean(key: String, value: Boolean) {}
                override fun observeString(key: String, defaultValue: String?): Flow<String?> = emptyFlow()
                override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> = emptyFlow()
            },
            userMediaBindings = object : UserMediaBindingProvider {
                override suspend fun getBinding(userId: Uuid, mediaId: String, sourceType: String): String? = null
                override suspend fun saveBinding(userId: Uuid, mediaId: String, sourceType: String, sourceId: String) {}
                override suspend fun deleteBinding(userId: Uuid, mediaId: String, sourceType: String) {}
            },
            recommendations = object : RecommendationEngineRegistrar {
                override fun registerEngine(engine: RecommendationEngine) {}
                override fun unregisterEngine() {}
            },
            telemetry = object : TelemetryProvider {
                override suspend fun getUserEvents(userId: Uuid, limit: Int): List<TelemetryEvent> = emptyList()
            },
            affinityStore = object : AffinityVectorStore {
                override val vectorUpdates: Flow<Uuid> = emptyFlow()
                override suspend fun getVector(userId: Uuid): AffinityVector? = null
                override suspend fun saveVector(userId: Uuid, vector: AffinityVector, eventCount: Int) {}
                override suspend fun getPendingUsers(limit: Int): List<Uuid> = emptyList()
                override suspend fun getUserEventCount(userId: Uuid): Int = 0
                override suspend fun getCachedEventCount(userId: Uuid): Int? = null
            },
            genreDictionary = object : GenreDictionaryProvider {
                override suspend fun getLocalizedGenres(language: String): Map<String, String> = mapOf(
                    "878" to "Фантастика",
                    "10765" to "Sci-Fi & Фэнтези",
                    "10759" to "Боевик & Приключения",
                    "18" to "Драма",
                    "28" to "Боевик"
                )
            },
            integrationManager = object : org.ensodai.avalonmediacard.contract.plugins.IntegrationSettingsManager {
                override suspend fun getTmdbToken(userId: kotlin.uuid.Uuid?): org.ensodai.avalonmediacard.contract.plugins.ResolvedIntegrationSetting? = null
                override suspend fun getTorrServerHost(userId: kotlin.uuid.Uuid?): org.ensodai.avalonmediacard.contract.plugins.ResolvedIntegrationSetting? = null
                override suspend fun getTorrServerAuth(userId: kotlin.uuid.Uuid?): String? = null
            },
            userSettings = object : org.ensodai.avalonmediacard.contract.plugins.UserPluginSettings {
                override suspend fun getString(userId: kotlin.uuid.Uuid, key: String): String? = null
                override suspend fun setString(userId: kotlin.uuid.Uuid, key: String, value: String) {}
                override suspend fun getBoolean(userId: kotlin.uuid.Uuid, key: String, defaultValue: Boolean): Boolean = defaultValue
                override suspend fun setBoolean(userId: kotlin.uuid.Uuid, key: String, value: Boolean) {}
                override fun observeString(userId: kotlin.uuid.Uuid, key: String, defaultValue: String?): kotlinx.coroutines.flow.Flow<String?> = kotlinx.coroutines.flow.flowOf(defaultValue)
                override fun observeBoolean(userId: kotlin.uuid.Uuid, key: String, defaultValue: Boolean): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(defaultValue)
            }
        )
    }
}

package com.example.data.model

enum class JokeSource(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val shortDescription: String
) {
    ANEKDOT_RU(
        id = "anekdot_ru",
        displayName = "Anekdot.ru",
        baseUrl = "https://www.anekdot.ru",
        shortDescription = "Анекдоты из России — старейший архив и поиск"
    ),
    NEKDO_RU(
        id = "nekdo_ru",
        displayName = "Nekdo.ru",
        baseUrl = "https://nekdo.ru",
        shortDescription = "Большая база анекдотов по темам и ключевым словам"
    ),
    ANEKDOTY_RU(
        id = "anekdoty_ru",
        displayName = "Anekdoty.ru",
        baseUrl = "https://anekdoty.ru",
        shortDescription = "Отборные рубрики и рейтинговые подборки"
    ),
    BANEKS_RU(
        id = "baneks_ru",
        displayName = "Baneks.ru",
        baseUrl = "https://baneks.ru",
        shortDescription = "Классические анекдоты категории Б"
    ),
    RZHUNEMOGU_RU(
        id = "rzhunemogu_ru",
        displayName = "РжуНеМогу",
        baseUrl = "http://rzhunemogu.ru",
        shortDescription = "Случайные народные анекдоты через JSON API"
    );

    companion object {
        fun fromId(id: String): JokeSource =
            entries.firstOrNull { it.id == id } ?: ANEKDOT_RU
    }
}

data class JokeItem(
    val id: String,
    val text: String,
    val source: JokeSource,
    val sourceUrl: String,
    val matchedQuery: String = "",
    val categoryLabel: String = "",
    val relevanceScore: Int = 0,
    val fetchedAt: Long = System.currentTimeMillis()
)

sealed interface SourceState {
    data object Idle : SourceState
    data object Loading : SourceState
    data class Success(val count: Int, val durationMs: Long) : SourceState
    data class Error(val message: String) : SourceState
}

data class SourceStatus(
    val source: JokeSource,
    val state: SourceState
)

data class AggregatedJokeResponse(
    val jokes: List<JokeItem>,
    val sourceStatuses: List<SourceStatus>,
    val query: String,
    val page: Int
)

data class PopularTopic(
    val label: String,
    val query: String
)

val PopularRussianTopics = listOf(
    PopularTopic("Штирлиц", "Штирлиц"),
    PopularTopic("Программисты", "программист"),
    PopularTopic("Вовочка", "Вовочка"),
    PopularTopic("Врачи", "врач"),
    PopularTopic("Работа и офис", "работа"),
    PopularTopic("Муж и жена", "жена"),
    PopularTopic("Студенты", "студент"),
    PopularTopic("Коты", "кот"),
    PopularTopic("Авто и ГАИ", "ГАИ"),
    PopularTopic("Одесса", "Рабинович"),
    PopularTopic("Деньги", "зарплата"),
    PopularTopic("Рыбалка", "рыбалка")
)

package com.example.data.remote

import java.util.Locale

object RussianQueryMatcher {

    private val russianLocale = Locale("ru", "RU")

    private val commonEndings = listOf(
        "иями", "ями", "ами", "ого", "ему", "ому", "ыми", "ими",
        "ей", "ой", "ий", "ый", "ая", "яя", "ое", "ее", "ые", "ие",
        "ов", "ев", "ам", "ям", "ах", "ях", "ом", "ем", "ую", "юю",
        "ть", "ти", "ет", "ют", "ит", "ат", "ят", "ал", "ил", "ла", "ли",
        "а", "я", "о", "е", "ы", "и", "у", "ю", "ь"
    )

    private val stopWords = setOf(
        "про", "как", "для", "или", "что", "это", "все", "где", "когда",
        "анекдот", "анекдоты", "шутка", "шутки", "смешной", "смешные"
    )

    fun extractQueryStems(query: String): List<String> {
        val cleaned = query.lowercase(russianLocale)
            .replace('ё', 'е')
            .replace(Regex("[^а-яa-z0-9\\s-]"), " ")
        val words = cleaned.split(Regex("\\s+"))
            .map { it.trim('-') }
            .filter { it.length >= 2 && it !in stopWords }

        if (words.isEmpty()) {
            val raw = cleaned.trim()
            return if (raw.isNotEmpty()) listOf(stemWord(raw)) else emptyList()
        }
        return words.map { stemWord(it) }.distinct()
    }

    fun stemWord(word: String): String {
        val w = word.lowercase(russianLocale).replace('ё', 'е')
        if (w.length <= 3) return w
        for (ending in commonEndings) {
            if (w.endsWith(ending) && w.length - ending.length >= 3) {
                return w.dropLast(ending.length)
            }
        }
        return w
    }

    fun computeRelevanceScore(jokeText: String, rawQuery: String, stems: List<String>): Int {
        if (rawQuery.isBlank()) return 10
        val normalizedText = jokeText.lowercase(russianLocale).replace('ё', 'е')
        val normalizedQuery = rawQuery.trim().lowercase(russianLocale).replace('ё', 'е')

        var score = 0
        if (normalizedQuery.length >= 3 && normalizedText.contains(normalizedQuery)) {
            score += 100
        }

        if (stems.isEmpty()) return score

        var matchedStems = 0
        for (stem in stems) {
            if (normalizedText.contains(stem)) {
                matchedStems++
                score += 35
            }
        }

        if (matchedStems == stems.size && stems.size > 1) {
            score += 50
        }
        return score
    }

    fun findHighlightRanges(text: String, query: String): List<IntRange> {
        val stems = extractQueryStems(query).filter { it.length >= 3 }
        if (stems.isEmpty()) return emptyList()

        val normalized = text.lowercase(russianLocale).replace('ё', 'е')
        val wordRegex = Regex("[а-яa-z0-9ё]+", RegexOption.IGNORE_CASE)
        val ranges = mutableListOf<IntRange>()

        for (match in wordRegex.findAll(normalized)) {
            val token = match.value
            if (stems.any { stem -> token.startsWith(stem) || (stem.length >= 4 && token.contains(stem)) }) {
                ranges.add(match.range)
            }
        }
        return ranges
    }

    data class TopicRoute(
        val label: String,
        val anekdotySlug: String?,
        val nekdoSlug: String?
    )

    private val topicMappings: List<Pair<List<String>, TopicRoute>> = listOf(
        listOf("программист", "айти", "компьютер", "хакер", "сисадмин", "код", "баг") to
            TopicRoute("Программисты и IT", "pro-programmistov", "computer"),
        listOf("штирлиц", "мюллер", "борман", "гестап") to
            TopicRoute("Штирлиц", "pro-shtirlica", "shtirlic"),
        listOf("вовочк", "школ", "учител", "урок", "дневник") to
            TopicRoute("Вовочка и школа", "pro-vovochku", "vovochka"),
        listOf("врач", "доктор", "медиц", "больниц", "пациент", "аптек", "хирург", "психиатр", "стоматолог") to
            TopicRoute("Врачи и медицина", "pro-vrachey", "doctor"),
        listOf("работ", "офис", "начальник", "шеф", "директор", "собеседован", "коллег", "бухгалтер") to
            TopicRoute("Работа и карьера", "pro-rabotu", "work"),
        listOf("семь", "муж", "жен", "свадьб", "брак", "любов") to
            TopicRoute("Семья и отношения", "pro-semyu", "family"),
        listOf("тещ", "свекров") to
            TopicRoute("Тёща", "pro-teshu", "family"),
        listOf("студент", "экзамен", "сесси", "общаг", "профессор", "лекци", "институт", "универ") to
            TopicRoute("Студенты", "pro-studentov", "student"),
        listOf("кот", "кошк", "собак", "попуга", "животн", "медвед", "заяц", "волк", "зоопарк") to
            TopicRoute("Животные", "pro-zhivotnyh", "animals"),
        listOf("рыбалк", "рыбак", "охот") to
            TopicRoute("Рыбалка и охота", "pro-rybalku", "hunting"),
        listOf("авто", "гаи", "гибдд", "машин", "водител", "инспектор", "такси", "дорог", "пробк") to
            TopicRoute("Авто и ГАИ", "pro-avto", "auto"),
        listOf("деньг", "зарплат", "финанс", "банк", "кредит", "богат", "кризис") to
            TopicRoute("Деньги и финансы", "pro-dengi", "money"),
        listOf("одесс", "рабинович", "сарочк", "евре", "мойш", "абрам") to
            TopicRoute("Одесский юмор", "pro-evreev", "jew"),
        listOf("арми", "солдат", "прапорщик", "генерал", "военком") to
            TopicRoute("Армия", "pro-armiyu", "army"),
        listOf("блондинк", "подруг", "девушк") to
            TopicRoute("Девушки", "pro-blondinok", "girl"),
        listOf("друз", "бар", "пив", "застол") to
            TopicRoute("Друзья", "pro-druzey", "alko"),
        listOf("коротк", "мини") to
            TopicRoute("Короткие", "korotkie", "short")
    )

    fun resolveTopicRoute(query: String): TopicRoute? {
        val normalized = query.lowercase(russianLocale).replace('ё', 'е')
        val stems = extractQueryStems(query)
        for ((keywords, route) in topicMappings) {
            if (keywords.any { kw ->
                    normalized.contains(kw) || stems.any { stem -> kw.startsWith(stem) || stem.startsWith(kw) }
                }
            ) {
                return route
            }
        }
        return null
    }
}

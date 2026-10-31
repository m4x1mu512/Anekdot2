package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.HtmlJokeParser
import com.example.data.remote.RussianQueryMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.Charset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("АнекдотПоиск", appName)
    }

    @Test
    fun `parse AnekdotRu HTML with nested divs and br line breaks`() {
        val sampleHtml = """
            <html><body>
            <div class="topicbox" data-id="1245678">
                <div class="date">31 октября</div>
                <div class="text">- Штирлиц, откуда у вас русская клавиатура?<br>- Нашел в лесу, группенфюрер!</div>
                <div class="rates"><span>+152</span></div>
            </div>
            </body></html>
        """.trimIndent()

        val jokes = HtmlJokeParser.parseAnekdotRuHtml(
            html = sampleHtml,
            query = "Штирлиц",
            fallbackUrl = "https://www.anekdot.ru/search/?kw=Штирлиц"
        )
        assertEquals(1, jokes.size)
        assertEquals("https://www.anekdot.ru/id/1245678/", jokes.first().sourceUrl)
        assertTrue(jokes.first().text.contains("— Штирлиц, откуда у вас русская клавиатура?"))
        assertTrue(jokes.first().text.contains("\n— Нашел в лесу, группенфюрер!"))
        assertTrue(jokes.first().relevanceScore > 0)
    }

    @Test
    fun `decode Windows-1251 bytes and parse Rzhunemogu JSON`() {
        val cp1251 = Charset.forName("windows-1251")
        val rawJson = "{\"content\":\"Сидят два программиста и обсуждают новый код в продакшене.\"}"
        val encodedBytes = rawJson.toByteArray(cp1251)

        val decoded = HtmlJokeParser.decodeBytes(encodedBytes, contentTypeHeader = null)
        val parsed = HtmlJokeParser.parseRzhunemoguResponse(decoded, query = "программист")

        assertEquals(1, parsed.size)
        assertEquals("Сидят два программиста и обсуждают новый код в продакшене.", parsed.first().text)
        assertTrue(parsed.first().relevanceScore > 0)
    }

    @Test
    fun `match Russian word inflections and resolve topic routes`() {
        val stems = RussianQueryMatcher.extractQueryStems("программисты")
        val score = RussianQueryMatcher.computeRelevanceScore(
            jokeText = "Жена отправляет программиста в магазин за батоном хлеба.",
            rawQuery = "программисты",
            stems = stems
        )
        assertTrue(score > 0)

        val route = RussianQueryMatcher.resolveTopicRoute("Вовочка")
        assertEquals("pro-vovochku", route?.anekdotySlug)
        assertEquals("vovochka", route?.nekdoSlug)
    }
}

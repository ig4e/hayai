package eu.kanade.tachiyomi.util.lang

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StringExtensionsTest {

    @Test
    fun `containsFuzzy matches acronym style queries`() {
        containsFuzzy("Global Search", "gs") shouldBe true
        containsFuzzy("Recent Updates", "ru") shouldBe true
    }

    @Test
    fun `containsFuzzy matches split token queries`() {
        containsFuzzy("English Local Source", "eng src") shouldBe true
        containsFuzzy("Default Category", "def cat") shouldBe true
    }

    @Test
    fun `containsFuzzy rejects unrelated text`() {
        containsFuzzy("MangaDex Extension", "zzz") shouldBe false
        containsFuzzy("Default Category", "repo auth") shouldBe false
    }
}

package com.github.ashutoshgngwr.noice.model

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class PresetTest {

  @Test
  fun sharedLinkRoundTripPreservesTheRecipe() {
    val states = arrayOf(Preset.PlayerState("brownian_noise", 25, 300))
    val link = Preset("old-id", "Offline mix", states).toUri().toString()
    val imported = Preset.fromSharedLink(" $link ")
    assertEquals("Offline mix", imported.name)
    assertEquals(states.toList(), imported.playerStates.toList())
    assertNotEquals("old-id", imported.id)
  }

  @Test
  fun sharedLinkRejectsInvalidInputs() {
    val valid = """{"soundKey":"brownian_noise","volume":20,"timePeriod":300}"""
    fun link(states: String, base: String = "noice://preset") =
      "$base?name=Imported&playerStates=${Uri.encode(states)}"
    val invalid = listOf(
      link("[$valid]", "https://example.org/noice/preset"),
      link("not json"),
      link("""[{soundKey:"brownian_noise",volume:20,timePeriod:300}]"""),
      link("[$valid] []"),
      link("[]"),
      link("[$valid,$valid]"),
      link("[${valid.replace("brownian_noise", "not_a_sound")}]"),
      link("[${valid.replace("20", "26")}]"),
      link("[${valid.replace("300", "29")}]"),
      link("""[{"soundKey":"brownian_noise","timePeriod":300}]""")
    )
    invalid.forEach { text ->
      assertThrows(IllegalArgumentException::class.java) { Preset.fromSharedLink(text) }
    }
  }

  @Test
  fun testEquality() {
    val preset1 = Preset.from("test-0", listOf(
      mockk(relaxed = true) {
        every { soundKey } returns "test"
        every { volume } returns 15
        every { timePeriod } returns 30
      }
    ))

    val preset2 = Preset.from("test-1", listOf(
      mockk(relaxed = true) {
        every { soundKey } returns "test-x"
        every { volume } returns 15
        every { timePeriod } returns 30
      }
    ))

    val preset3 = Preset.from("test-2", listOf(
      mockk(relaxed = true) {
        every { soundKey } returns "test"
        every { volume } returns 20
        every { timePeriod } returns 30
      }
    ))

    val preset4 = Preset.from("test-2", listOf(
      mockk(relaxed = true) {
        every { soundKey } returns "test"
        every { volume } returns 15
        every { timePeriod } returns 40
      }
    ))

    val preset5 = Preset.from("test-2", listOf(
      mockk(relaxed = true) {
        every { soundKey } returns "test"
        every { volume } returns 15
        every { timePeriod } returns 30
      }
    ))

    assertNotEquals(preset1, preset2)
    assertNotEquals(preset1, preset3)
    assertNotEquals(preset1, preset4)
    assertEquals(preset1, preset5)
  }

  @Test
  fun testFromUri() {
    val presetUri = "noice://preset?name=test&playerStates=%5B%7B%22soundKey%22%3A%22test-1%22%2C" +
      "%22timePeriod%22%3A300%2C%22volume%22%3A4%7D%2C%7B%22soundKey%22%3A%22test-2%22%2C%22timeP" +
      "eriod%22%3A300%2C%22volume%22%3A4%7D%5D"

    val expectedOutput = arrayOf(
      Preset.PlayerState("test-1", 4, 300),
      Preset.PlayerState("test-2", 4, 300)
    )

    val preset = Preset.from(Uri.parse(presetUri))
    assertEquals("test", preset.name)
    assertEquals(expectedOutput.size, preset.playerStates.size)
    for (i in expectedOutput.indices) {
      assertEquals(expectedOutput[i], preset.playerStates[i])
    }
  }

  @Test
  fun testToUri() {
    val preset = Preset.from("test", listOf(
      mockk(relaxed = true) {
        every { soundKey } returns "test"
        every { volume } returns 15
        every { timePeriod } returns 30
      }
    ))

    val uri = preset.toUri()
    assertEquals(preset.name, uri.getQueryParameter(Preset.URI_NAME_PARAM))

    val playerStatesJSON = """[{
      "soundKey": "test",
      "volume": 15,
      "timePeriod": 30
    }]""".trimIndent()

    JSONAssert.assertEquals(
      playerStatesJSON,
      uri.getQueryParameter(Preset.URI_PLAYER_STATES_PARAM),
      true
    )
  }
}

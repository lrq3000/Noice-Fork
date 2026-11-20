package com.github.ashutoshgngwr.noice.model

import android.net.Uri
import com.github.ashutoshgngwr.noice.playback.Player
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.StringReader
import java.util.*

/**
 * [Preset] represents a snapshot of Player Manager at any given time. It holds states of various
 * players and is used for serializing the state to JSON and persisting it on disk.
 */
class Preset(
  @Expose val id: String,
  @Expose var name: String,
  @Expose val playerStates: Array<PlayerState>
) {

  companion object {
    private const val URI_SCHEME = "https"
    private const val URI_AUTHORITY = "ashutoshgngwr.github.io"
    private const val URI_PATH = "/noice/preset"
    internal const val URI_NAME_PARAM = "name"
    internal const val URI_PLAYER_STATES_PARAM = "playerStates"
    private val jsonElementAdapter = Gson().getAdapter(JsonElement::class.java)

    /**
     * [from] exposes the primary constructor of [Preset] class. It automatically infers the
     * [PlayerState]s from provided [Collection] of [Player] instances.
     */
    fun from(name: String, players: Collection<Player>): Preset {
      val playerStates = arrayListOf<PlayerState>()
      for (player in players) {
        playerStates.add(PlayerState(player.soundKey, player.volume, player.timePeriod))
      }

      return Preset(UUID.randomUUID().toString(), name, playerStates.toTypedArray())
    }

    /**
     * Decodes a preset from an [Uri] encoded using [toUri].
     */
    fun from(uri: Uri): Preset {
      val name = uri.getQueryParameter(URI_NAME_PARAM) ?: ""
      val playerStatesJSON = uri.getQueryParameter(URI_PLAYER_STATES_PARAM)
        ?: throw IllegalArgumentException("'playerStates' query parameter is missing from the URI.")

      val playerStates = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()
        .fromJson(playerStatesJSON, Array<PlayerState>::class.java)

      return Preset(UUID.randomUUID().toString(), name, playerStates)
    }

    /** Parses a pasted recipe without opening its URL or starting playback. */
    fun fromSharedLink(text: String): Preset {
      require(text.length <= 16_384)
      val uri = Uri.parse(text.trim())
      require(
        (uri.scheme == URI_SCHEME && uri.encodedAuthority == URI_AUTHORITY && uri.encodedPath == URI_PATH) ||
          (uri.scheme == "noice" && uri.encodedAuthority == "preset" && uri.encodedPath.isNullOrEmpty())
      )
      val name = uri.getQueryParameters(URI_NAME_PARAM).also { require(it.size <= 1) }.firstOrNull().orEmpty()
      require(name.length <= 150)
      val payload = uri.getQueryParameters(URI_PLAYER_STATES_PARAM).singleOrNull()
        ?: throw IllegalArgumentException("Expected one playerStates query parameter")
      val parsed = try {
        // Gson's convenience parser accepts malformed JSON such as unquoted field names.
        JsonReader(StringReader(payload)).use { reader ->
          jsonElementAdapter.read(reader).also { require(reader.peek() == JsonToken.END_DOCUMENT) }
        }
      } catch (error: IOException) {
        throw IllegalArgumentException("Invalid preset JSON", error)
      }
      require(parsed.isJsonArray && parsed.asJsonArray.size() in 1..Sound.LIBRARY.size)
      val seen = mutableSetOf<String>()
      val states = parsed.asJsonArray.map { element ->
        require(element.isJsonObject)
        val entry = element.asJsonObject
        val sound = entry.get("soundKey") ?: throw IllegalArgumentException("Missing soundKey")
        require(sound.isJsonPrimitive && sound.asJsonPrimitive.isString)
        val key = sound.asString
        require(Sound.LIBRARY.containsKey(key) && seen.add(key))
        val volume = entry.requiredInteger("volume")
        val period = entry.requiredInteger("timePeriod")
        require(volume in 0..Player.MAX_VOLUME && period in Player.MIN_TIME_PERIOD..Player.MAX_TIME_PERIOD)
        PlayerState(key, volume, period)
      }
      return Preset(UUID.randomUUID().toString(), name, states.toTypedArray())
    }

    private fun JsonObject.requiredInteger(key: String): Int {
      val field: JsonElement = get(key) ?: throw IllegalArgumentException("Missing $key")
      require(field.isJsonPrimitive && field.asJsonPrimitive.isNumber)
      return field.asString.toIntOrNull() ?: throw IllegalArgumentException("Invalid $key")
    }
  }

  init {
    // for stable equality operations
    playerStates.sortBy { T -> T.soundKey }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }

    if (other !is Preset) {
      return false
    }

    // id and name need not be equal. playbackStates should be
    return playerStates.contentEquals(other.playerStates)
  }

  override fun hashCode(): Int {
    // auto-generated
    var result = name.hashCode()
    result = 31 * result + playerStates.contentHashCode()
    return result
  }

  /**
   * Encodes the preset to an [Uri] with [name] and [playerStates] as query its parameters.
   */
  fun toUri(): Uri {
    val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
    return Uri.Builder()
      .scheme(URI_SCHEME)
      .authority(URI_AUTHORITY)
      .path(URI_PATH)
      .appendQueryParameter(URI_NAME_PARAM, name)
      .appendQueryParameter(URI_PLAYER_STATES_PARAM, gson.toJson(playerStates))
      .build()
  }

  /**
   * [PlayerState] can hold the state properties [key, volume, timePeriod] of a [Player] instance.
   * Used for JSON encoding.
   */
  data class PlayerState(
    @Expose val soundKey: String, @Expose val volume: Int, @Expose val timePeriod: Int
  )
}

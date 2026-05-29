package com.dupbuster.scanengine.fixtures

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.json.JSONArray
import org.json.JSONObject

/** Loads M1-17 JSON fixture descriptors from `tests/fixtures/dupbuster/v1/`. */
object FixtureLoader {

  fun root(): Path = fixtureRoot()

  fun load(path: String): JSONObject {
    val text = String(Files.readAllBytes(root().resolve(path)), StandardCharsets.UTF_8)
    return JSONObject(text)
  }

  fun equivalenceFixturePaths(): List<String> {
    val fixtures = manifestFixtures()
    val paths = mutableListOf<String>()
    for (i in 0 until fixtures.length()) {
      val row = fixtures.getJSONObject(i)
      if (row.optString("kind") == "equivalence") {
        paths.add(row.getString("path"))
      }
    }
    return paths.sorted()
  }

  private fun manifestFixtures(): JSONArray {
    val manifest = JSONObject(readUtf8(root().resolve("manifest.json")))
    return manifest.getJSONArray("fixtures")
  }

  private fun readUtf8(path: Path): String =
      String(Files.readAllBytes(path), StandardCharsets.UTF_8)

  private fun fixtureRoot(): Path {
    var dir: Path? = Path.of(System.getProperty("user.dir"))
    while (dir != null) {
      val candidate = dir.resolve("tests/fixtures/dupbuster/v1/manifest.json")
      if (Files.exists(candidate)) {
        return dir.resolve("tests/fixtures/dupbuster/v1")
      }
      dir = dir.parent
    }
    error("Could not locate tests/fixtures/dupbuster/v1 from ${System.getProperty("user.dir")}")
  }
}

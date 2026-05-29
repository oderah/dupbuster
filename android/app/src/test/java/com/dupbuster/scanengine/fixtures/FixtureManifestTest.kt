package com.dupbuster.scanengine.fixtures

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validates M1-17 fixture matrix: manifest integrity and on-disk JSON descriptors. */
class FixtureManifestTest {

  @Test
  fun manifest_allListedFixturesExistWithMatchingIds() {
    val root = fixtureRoot()
    val manifestText = readUtf8(root.resolve("manifest.json"))
    assertTrue(manifestText.contains("\"schemaVersion\": 1"))

    val paths = extractQuotedValues(manifestText, "path")
    assertTrue("fixture matrix must list paths", paths.isNotEmpty())

    val ids = mutableSetOf<String>()
    for (path in paths) {
      val file = root.resolve(path)
      assertTrue("missing fixture file: $path", Files.exists(file))
      val fixtureText = readUtf8(file)
      val id = extractField(fixtureText, "id")
      assertTrue("duplicate manifest id: $id", ids.add(id))
      assertTrue("$path must have input", fixtureText.contains("\"input\""))
      assertTrue("$path must have expect", fixtureText.contains("\"expect\""))
      assertTrue("$path must have platform", fixtureText.contains("\"platform\""))
    }
  }

  @Test
  fun manifest_coversAllRequirementEquivAcceptanceCriteria() {
    val manifestText = readUtf8(fixtureRoot().resolve("manifest.json"))
    val listed = extractQuotedArrayValues(manifestText, "acceptanceCriteria").toSet()

    for (required in REQUIRED_EQUIV_AC) {
      assertTrue("manifest missing $required", listed.contains(required))
    }
  }

  private fun readUtf8(path: Path): String =
      String(Files.readAllBytes(path), StandardCharsets.UTF_8)

  private fun extractField(json: String, field: String): String {
    val pattern = "\"$field\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    return pattern.find(json)?.groupValues?.get(1)
        ?: error("field $field not found")
  }

  private fun extractQuotedValues(json: String, field: String): List<String> {
    val pattern = "\"$field\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    return pattern.findAll(json).map { it.groupValues[1] }.toList()
  }

  private fun extractQuotedArrayValues(json: String, field: String): List<String> {
    val pattern = "\"$field\"\\s*:\\s*\\[([^\\]]*)]".toRegex()
    return pattern
        .findAll(json)
        .flatMap { match ->
          "\"([^\"]+)\"".toRegex().findAll(match.groupValues[1]).map { it.groupValues[1] }
        }
        .toList()
  }

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

  companion object {
    private val REQUIRED_EQUIV_AC =
        listOf(
            "AC-equiv-text-01",
            "AC-equiv-text-02",
            "AC-equiv-text-03",
            "AC-equiv-text-04",
            "AC-equiv-doc-01",
            "AC-equiv-doc-02",
            "AC-equiv-img-01",
            "AC-equiv-img-02",
            "AC-equiv-av-01",
            "AC-equiv-video-xres-01",
            "AC-equiv-video-xres-02",
            "AC-equiv-video-xres-03",
            "AC-equiv-video-xres-04",
            "AC-equiv-video-xres-05",
            "AC-equiv-video-xres-05b",
            "AC-equiv-video-xres-06",
            "AC-equiv-video-xres-07",
            "AC-equiv-video-xres-08",
            "AC-equiv-video-xres-09",
            "AC-equiv-video-xres-10",
            "AC-equiv-empty-01",
            "AC-equiv-symlink-01",
        )
  }
}

package com.example.data.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CurriculumSubjectItem(
    val code: String,
    val name: String,
    val coefficient: Int = 4,
    val category: String = "Core",
    val systemCode: String = "ANGLOPHONE",
    val levelCode: String = "ORDINARY_LEVEL",
    val streamCode: String = "SCIENCE",
    val seriesCode: String = "GENERAL",
    val seriesName: String = "General Series"
) {
    val formattedLabel: String
        get() = if (code.isNotBlank()) "$name ($code)" else name
}

data class CurriculumSeriesItem(
    val code: String,
    val name: String,
    val description: String,
    val systemCode: String,
    val levelCode: String,
    val streamCode: String,
    val subjects: List<CurriculumSubjectItem>
)

object CurriculumCatalog {

    private var activeCustomJson: String? = null
    private val adminAddedSubjects = mutableListOf<AdminCustomSubject>()

    fun setAdminAddedSubjects(subjects: List<AdminCustomSubject>) {
        synchronized(adminAddedSubjects) {
            adminAddedSubjects.clear()
            adminAddedSubjects.addAll(subjects)
        }
    }

    fun getAdminAddedSubjectsSnapshot(): List<AdminCustomSubject> {
        return synchronized(adminAddedSubjects) {
            adminAddedSubjects.toList()
        }
    }

    fun setCustomCurriculumJson(json: String?): Boolean {
        if (json.isNullOrBlank()) {
            activeCustomJson = null
            return true
        }
        val parsed = parseFlexibleCurriculumSubjects(json)
        return if (parsed.isNotEmpty()) {
            activeCustomJson = json.trim()
            true
        } else {
            false
        }
    }

    fun getActiveJsonString(context: Context? = null): String {
        activeCustomJson?.let { if (it.isNotBlank()) return it }
        if (context != null) {
            try {
                context.assets.open("curriculum_subjects.json").bufferedReader().use { reader ->
                    val text = reader.readText()
                    if (text.isNotBlank()) return text
                }
            } catch (_: Exception) {
                // Fallback to embedded JSON
            }
        }
        return DEFAULT_CURRICULUM_JSON
    }

    fun getAllSeries(
        systemCode: String = "",
        levelCode: String = "",
        streamCode: String = "",
        customJson: String? = activeCustomJson
    ): List<CurriculumSeriesItem> {
        val raw = customJson?.takeIf { it.isNotBlank() } ?: DEFAULT_CURRICULUM_JSON
        val baseSeries = parseSeriesFromJson(raw).ifEmpty {
            parseSeriesFromJson(DEFAULT_CURRICULUM_JSON)
        }.toMutableList()

        val adminSnapshot = getAdminAddedSubjectsSnapshot()
        adminSnapshot.forEach { adminSubj ->
            val item = adminSubj.toCurriculumSubjectItem()
            val existingIdx = baseSeries.indexOfFirst {
                it.code.equals(item.seriesCode, ignoreCase = true) &&
                    it.systemCode.equals(item.systemCode, ignoreCase = true)
            }
            if (existingIdx >= 0) {
                val existing = baseSeries[existingIdx]
                if (existing.subjects.none { it.name.equals(item.name, ignoreCase = true) }) {
                    baseSeries[existingIdx] = existing.copy(subjects = listOf(item) + existing.subjects)
                }
            } else if (item.seriesCode.isNotBlank() && item.seriesCode != "ALL") {
                baseSeries.add(
                    0,
                    CurriculumSeriesItem(
                        code = item.seriesCode,
                        name = item.seriesName.ifBlank { "Series ${item.seriesCode}" },
                        description = adminSubj.description.ifBlank { "Custom Admin Series (${item.systemCode})" },
                        systemCode = item.systemCode,
                        levelCode = item.levelCode,
                        streamCode = item.streamCode,
                        subjects = listOf(item)
                    )
                )
            }
        }

        val normSys = normalizeSystemCode(systemCode)
        val normLevel = normalizeLevelCode(levelCode)
        val normStream = normalizeStreamCode(streamCode)

        val filtered = baseSeries.filter { series ->
            val sysMatch = normSys.isEmpty() || normSys == "BILINGUAL" ||
                series.systemCode == "BILINGUAL" || series.systemCode == normSys
            val lvlMatch = normLevel.isEmpty() || series.levelCode == normLevel
            val strMatch = normStream.isEmpty() || series.streamCode == normStream
            sysMatch && lvlMatch && strMatch
        }
        return filtered.ifEmpty { baseSeries }
    }

    fun getSubjectsFor(
        systemCode: String = "",
        levelCode: String = "",
        streamCode: String = "",
        seriesCode: String = "ALL",
        customJson: String? = activeCustomJson
    ): List<CurriculumSubjectItem> {
        val raw = customJson?.takeIf { it.isNotBlank() } ?: DEFAULT_CURRICULUM_JSON
        val jsonSubjects = parseFlexibleCurriculumSubjects(raw).ifEmpty {
            parseFlexibleCurriculumSubjects(DEFAULT_CURRICULUM_JSON)
        }
        val adminItems = getAdminAddedSubjectsSnapshot().map { it.toCurriculumSubjectItem() }
        val allSubjects = adminItems + jsonSubjects

        val normSys = normalizeSystemCode(systemCode)
        val normLevel = normalizeLevelCode(levelCode)
        val normStream = normalizeStreamCode(streamCode)
        val normSeries = seriesCode.trim()

        val filtered = allSubjects.filter { subj ->
            val sysMatch = normSys.isEmpty() || normSys == "BILINGUAL" ||
                subj.systemCode.equals("BILINGUAL", ignoreCase = true) ||
                subj.systemCode.equals(normSys, ignoreCase = true) ||
                subj.systemCode == "ALL"
            val lvlMatch = normLevel.isEmpty() ||
                subj.levelCode.equals(normLevel, ignoreCase = true) ||
                subj.levelCode == "ALL"
            val strMatch = normStream.isEmpty() ||
                subj.streamCode.equals(normStream, ignoreCase = true) ||
                subj.streamCode == "ALL"
            val serMatch = normSeries.isEmpty() || normSeries.equals("ALL", ignoreCase = true) ||
                subj.seriesCode.equals(normSeries, ignoreCase = true) ||
                subj.seriesName.contains(normSeries, ignoreCase = true)
            sysMatch && lvlMatch && strMatch && serMatch
        }

        val distinctByName = filtered.distinctBy { "${it.name.lowercase()}_${it.code.lowercase()}" }
        return if (distinctByName.isNotEmpty()) {
            distinctByName
        } else {
            allSubjects.distinctBy { "${it.name.lowercase()}_${it.code.lowercase()}" }
        }
    }

    fun getDefaultSubjectLabelsFor(
        systemCode: String,
        levelCode: String,
        streamCode: String,
        seriesCode: String = "ALL"
    ): List<String> {
        val matching = getSubjectsFor(
            systemCode = systemCode,
            levelCode = levelCode,
            streamCode = streamCode,
            seriesCode = seriesCode
        )
        val core = matching.filter {
            it.category.contains("Compulsory", ignoreCase = true) ||
                it.category.contains("Principal", ignoreCase = true) ||
                it.category.contains("Core", ignoreCase = true) ||
                it.category.contains("Spécialité", ignoreCase = true) ||
                it.category.contains("Obligatoire", ignoreCase = true) ||
                it.category.contains("Fondamental", ignoreCase = true)
        }
        return (if (core.isNotEmpty()) core.take(6) else matching.take(5)).map { it.formattedLabel }
    }

    private fun normalizeSystemCode(raw: String): String {
        val u = raw.trim().uppercase()
        return when {
            u.contains("FRANCO") -> EducationSystem.FRANCOPHONE.code
            u.contains("ANGLO") -> EducationSystem.ANGLOPHONE.code
            u.contains("BILING") -> EducationSystem.BILINGUAL.code
            else -> u
        }
    }

    private fun normalizeLevelCode(raw: String): String {
        val u = raw.trim().uppercase()
        return when {
            u.contains("ADV") || u.contains("2ND") || u.contains("SECOND") || u.contains("BACC") || u.contains("A_LEVEL") || u.contains("A-LEVEL") ->
                StudentLevel.ADVANCED_LEVEL.code
            u.contains("ORD") || u.contains("1ER") || u.contains("FIRST") || u.contains("BEPC") || u.contains("O_LEVEL") || u.contains("O-LEVEL") ->
                StudentLevel.ORDINARY_LEVEL.code
            else -> u
        }
    }

    private fun normalizeStreamCode(raw: String): String {
        val u = raw.trim().uppercase()
        return when {
            u.contains("ART") || u.contains("LIT") || u.contains("HUM") -> StudentStream.ART.code
            u.contains("SCI") || u.contains("TECH") || u.contains("STEM") -> StudentStream.SCIENCE.code
            else -> u
        }
    }

    private fun parseSeriesFromJson(jsonString: String): List<CurriculumSeriesItem> {
        val result = mutableListOf<CurriculumSeriesItem>()
        try {
            val root = JSONObject(jsonString.trim())
            val systemsArray = root.optJSONArray("systems") ?: return emptyList()
            for (i in 0 until systemsArray.length()) {
                val sysObj = systemsArray.optJSONObject(i) ?: continue
                val sysCode = normalizeSystemCode(sysObj.optString("code", "ANGLOPHONE"))
                val levelsArray = sysObj.optJSONArray("levels") ?: continue
                for (j in 0 until levelsArray.length()) {
                    val lvlObj = levelsArray.optJSONObject(j) ?: continue
                    val lvlCode = normalizeLevelCode(lvlObj.optString("code", "ORDINARY_LEVEL"))
                    val streamsArray = lvlObj.optJSONArray("streams") ?: continue
                    for (k in 0 until streamsArray.length()) {
                        val strObj = streamsArray.optJSONObject(k) ?: continue
                        val strCode = normalizeStreamCode(strObj.optString("code", "SCIENCE"))
                        val seriesArray = strObj.optJSONArray("series") ?: continue
                        for (m in 0 until seriesArray.length()) {
                            val serObj = seriesArray.optJSONObject(m) ?: continue
                            val serCode = serObj.optString("code", "SERIES_${m + 1}")
                            val serName = serObj.optString("name", serCode)
                            val serDesc = serObj.optString("description", "")
                            val subjectsArray = serObj.optJSONArray("subjects") ?: JSONArray()
                            val subjectsList = mutableListOf<CurriculumSubjectItem>()
                            for (n in 0 until subjectsArray.length()) {
                                val subEntry = subjectsArray.get(n)
                                when (subEntry) {
                                    is JSONObject -> {
                                        val name = subEntry.optString("name", subEntry.optString("title", "")).trim()
                                        if (name.isNotEmpty()) {
                                            subjectsList.add(
                                                CurriculumSubjectItem(
                                                    code = subEntry.optString("code", subEntry.optString("id", "")).trim(),
                                                    name = name,
                                                    coefficient = subEntry.optInt("coefficient", subEntry.optInt("coeff", 4)),
                                                    category = subEntry.optString("category", subEntry.optString("type", "Core")).trim(),
                                                    systemCode = sysCode,
                                                    levelCode = lvlCode,
                                                    streamCode = strCode,
                                                    seriesCode = serCode,
                                                    seriesName = serName
                                                )
                                            )
                                        }
                                    }
                                    is String -> {
                                        val name = subEntry.trim()
                                        if (name.isNotEmpty()) {
                                            subjectsList.add(
                                                CurriculumSubjectItem(
                                                    code = "",
                                                    name = name,
                                                    systemCode = sysCode,
                                                    levelCode = lvlCode,
                                                    streamCode = strCode,
                                                    seriesCode = serCode,
                                                    seriesName = serName
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            result.add(
                                CurriculumSeriesItem(
                                    code = serCode,
                                    name = serName,
                                    description = serDesc,
                                    systemCode = sysCode,
                                    levelCode = lvlCode,
                                    streamCode = strCode,
                                    subjects = subjectsList
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Handled by flexible parser fallback
        }
        return result
    }

    /**
     * Parses both the structured curriculum JSON and any custom JSON format provided by the user
     * (e.g., nested objects, {"subjects": [...]}, or a top-level JSON array of subjects).
     */
    fun parseFlexibleCurriculumSubjects(jsonString: String): List<CurriculumSubjectItem> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. Try canonical hierarchy first
        val fromSeries = parseSeriesFromJson(trimmed).flatMap { it.subjects }
        if (fromSeries.isNotEmpty()) return fromSeries

        // 2. Flexible recursive extraction for any custom JSON structure
        val extracted = mutableListOf<CurriculumSubjectItem>()
        try {
            if (trimmed.startsWith("[")) {
                extractFromJsonArray(
                    array = JSONArray(trimmed),
                    sysCode = "ALL",
                    lvlCode = "ALL",
                    strCode = "ALL",
                    serCode = "CUSTOM",
                    out = extracted
                )
            } else if (trimmed.startsWith("{")) {
                extractFromJsonObject(
                    obj = JSONObject(trimmed),
                    sysCode = "ALL",
                    lvlCode = "ALL",
                    strCode = "ALL",
                    serCode = "CUSTOM",
                    out = extracted
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return extracted
    }

    private fun extractFromJsonObject(
        obj: JSONObject,
        sysCode: String,
        lvlCode: String,
        strCode: String,
        serCode: String,
        out: MutableList<CurriculumSubjectItem>
    ) {
        // Check if this object itself is a subject definition
        val maybeSubjectName = obj.optString("name", obj.optString("subject", obj.optString("title", ""))).trim()
        val hasDirectChildrenArrays = obj.optJSONArray("subjects") != null ||
            obj.optJSONArray("series") != null ||
            obj.optJSONArray("streams") != null ||
            obj.optJSONArray("levels") != null ||
            obj.optJSONArray("systems") != null

        if (maybeSubjectName.isNotEmpty() && !hasDirectChildrenArrays) {
            out.add(
                CurriculumSubjectItem(
                    code = obj.optString("code", obj.optString("id", "")).trim(),
                    name = maybeSubjectName,
                    coefficient = obj.optInt("coefficient", obj.optInt("coeff", 4)),
                    category = obj.optString("category", obj.optString("type", "Custom")).trim(),
                    systemCode = sysCode,
                    levelCode = lvlCode,
                    streamCode = strCode,
                    seriesCode = serCode,
                    seriesName = serCode
                )
            )
            return
        }

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key) ?: continue
            val nextSys = when {
                key.contains("franco", ignoreCase = true) -> EducationSystem.FRANCOPHONE.code
                key.contains("anglo", ignoreCase = true) -> EducationSystem.ANGLOPHONE.code
                else -> sysCode
            }
            val nextLvl = when {
                key.contains("adv", ignoreCase = true) || key.contains("2nd", ignoreCase = true) || key.contains("bacc", ignoreCase = true) ->
                    StudentLevel.ADVANCED_LEVEL.code
                key.contains("ord", ignoreCase = true) || key.contains("1er", ignoreCase = true) || key.contains("bepc", ignoreCase = true) ->
                    StudentLevel.ORDINARY_LEVEL.code
                else -> lvlCode
            }
            val nextStr = when {
                key.contains("sci", ignoreCase = true) -> StudentStream.SCIENCE.code
                key.contains("art", ignoreCase = true) || key.contains("lit", ignoreCase = true) -> StudentStream.ART.code
                else -> strCode
            }
            when (value) {
                is JSONArray -> extractFromJsonArray(value, nextSys, nextLvl, nextStr, key, out)
                is JSONObject -> extractFromJsonObject(value, nextSys, nextLvl, nextStr, key, out)
            }
        }
    }

    private fun extractFromJsonArray(
        array: JSONArray,
        sysCode: String,
        lvlCode: String,
        strCode: String,
        serCode: String,
        out: MutableList<CurriculumSubjectItem>
    ) {
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is String -> {
                    val clean = item.trim()
                    if (clean.isNotEmpty()) {
                        out.add(
                            CurriculumSubjectItem(
                                code = "",
                                name = clean,
                                systemCode = sysCode,
                                levelCode = lvlCode,
                                streamCode = strCode,
                                seriesCode = serCode,
                                seriesName = serCode
                            )
                        )
                    }
                }
                is JSONObject -> extractFromJsonObject(item, sysCode, lvlCode, strCode, serCode, out)
                is JSONArray -> extractFromJsonArray(item, sysCode, lvlCode, strCode, serCode, out)
            }
        }
    }

    const val DEFAULT_CURRICULUM_JSON = """
{
  "curriculumName": "Cameroon Bilingual Secondary & High School E-Learning Curriculum",
  "version": "2026.1",
  "systems": [
    {
      "code": "ANGLOPHONE",
      "name": "Anglophone Educational System (GCE)",
      "levels": [
        {
          "code": "ORDINARY_LEVEL",
          "name": "Ordinary Level (GCE O-Level / Forms 1–5)",
          "streams": [
            {
              "code": "SCIENCE",
              "name": "Science & Technology Stream",
              "series": [
                {
                  "code": "OL_SCI_CORE",
                  "name": "O-Level General Science & STEM",
                  "description": "Core GCE Ordinary Level Science combination preparing for Advanced Level S1, S2, S3, and S4.",
                  "subjects": [
                    { "code": "0570", "name": "Mathematics", "coefficient": 5, "category": "Compulsory" },
                    { "code": "0575", "name": "Additional Mathematics", "coefficient": 4, "category": "Core Science" },
                    { "code": "0580", "name": "Physics", "coefficient": 4, "category": "Core Science" },
                    { "code": "0515", "name": "Chemistry", "coefficient": 4, "category": "Core Science" },
                    { "code": "0510", "name": "Biology", "coefficient": 4, "category": "Core Science" },
                    { "code": "0595", "name": "Computer Science", "coefficient": 3, "category": "Core Science" },
                    { "code": "0530", "name": "English Language", "coefficient": 5, "category": "Compulsory" },
                    { "code": "0545", "name": "French", "coefficient": 3, "category": "Compulsory" },
                    { "code": "0565", "name": "Human Biology", "coefficient": 3, "category": "Elective" },
                    { "code": "0550", "name": "Geography", "coefficient": 3, "category": "Elective" },
                    { "code": "0525", "name": "Economics", "coefficient": 3, "category": "Elective" },
                    { "code": "0562", "name": "Citizenship Education", "coefficient": 2, "category": "Compulsory" }
                  ]
                }
              ]
            },
            {
              "code": "ART",
              "name": "Arts, Humanities & Social Sciences Stream",
              "series": [
                {
                  "code": "OL_ART_CORE",
                  "name": "O-Level Arts, Languages & Social Sciences",
                  "description": "Core GCE Ordinary Level Arts combination preparing for Advanced Level A1, A2, A3, A4, and A5.",
                  "subjects": [
                    { "code": "0530", "name": "English Language", "coefficient": 5, "category": "Compulsory" },
                    { "code": "0535", "name": "Literature in English", "coefficient": 4, "category": "Core Arts" },
                    { "code": "0560", "name": "History", "coefficient": 4, "category": "Core Arts" },
                    { "code": "0550", "name": "Geography", "coefficient": 4, "category": "Core Arts" },
                    { "code": "0525", "name": "Economics", "coefficient": 4, "category": "Core Arts" },
                    { "code": "0545", "name": "French", "coefficient": 4, "category": "Compulsory" },
                    { "code": "0570", "name": "Mathematics", "coefficient": 4, "category": "Compulsory" },
                    { "code": "0585", "name": "Religious Studies", "coefficient": 3, "category": "Elective" },
                    { "code": "0572", "name": "Logic", "coefficient": 3, "category": "Elective" },
                    { "code": "0520", "name": "Commerce", "coefficient": 3, "category": "Elective" },
                    { "code": "0562", "name": "Citizenship Education", "coefficient": 2, "category": "Compulsory" },
                    { "code": "0595", "name": "Computer Science", "coefficient": 2, "category": "Elective" }
                  ]
                }
              ]
            }
          ]
        },
        {
          "code": "ADVANCED_LEVEL",
          "name": "Advanced Level (GCE A-Level / Lower & Upper Sixth)",
          "streams": [
            {
              "code": "SCIENCE",
              "name": "Science & Technology Stream",
              "series": [
                {
                  "code": "S1",
                  "name": "Series S1 (Pure Math, Physics & Chemistry)",
                  "description": "Engineering, Polytechnic, Architecture & Physical Sciences track.",
                  "subjects": [
                    { "code": "0770", "name": "Pure Mathematics With Mechanics", "coefficient": 5, "category": "Principal" },
                    { "code": "0780", "name": "Physics", "coefficient": 5, "category": "Principal" },
                    { "code": "0715", "name": "Chemistry", "coefficient": 5, "category": "Principal" },
                    { "code": "0765", "name": "Further Mathematics", "coefficient": 5, "category": "Specialty" },
                    { "code": "0795", "name": "Computer Science", "coefficient": 4, "category": "Elective" }
                  ]
                },
                {
                  "code": "S2",
                  "name": "Series S2 (Chemistry, Biology & Physics/Math)",
                  "description": "Medicine, Pharmacy, Biomedical & Health Sciences track.",
                  "subjects": [
                    { "code": "0710", "name": "Biology", "coefficient": 5, "category": "Principal" },
                    { "code": "0715", "name": "Chemistry", "coefficient": 5, "category": "Principal" },
                    { "code": "0780", "name": "Physics", "coefficient": 5, "category": "Principal" },
                    { "code": "0775", "name": "Pure Mathematics With Statistics", "coefficient": 5, "category": "Elective" },
                    { "code": "0740", "name": "Food Science and Nutrition", "coefficient": 3, "category": "Elective" }
                  ]
                },
                {
                  "code": "S3",
                  "name": "Series S3 (Biology, Chemistry & Geology/Geography)",
                  "description": "Agronomy, Earth Sciences, Environmental & Life Sciences track.",
                  "subjects": [
                    { "code": "0710", "name": "Biology", "coefficient": 5, "category": "Principal" },
                    { "code": "0715", "name": "Chemistry", "coefficient": 5, "category": "Principal" },
                    { "code": "0755", "name": "Geology", "coefficient": 5, "category": "Principal" },
                    { "code": "0750", "name": "Geography", "coefficient": 4, "category": "Elective" },
                    { "code": "0796", "name": "Information and Communication Technology (ICT)", "coefficient": 4, "category": "Elective" }
                  ]
                },
                {
                  "code": "S4",
                  "name": "Series S4 (Mathematics, Computer Science/ICT & Physics)",
                  "description": "Software Engineering, Artificial Intelligence, Telecommunications & Data Science track.",
                  "subjects": [
                    { "code": "0770", "name": "Pure Mathematics With Mechanics", "coefficient": 5, "category": "Principal" },
                    { "code": "0775", "name": "Pure Mathematics With Statistics", "coefficient": 5, "category": "Principal" },
                    { "code": "0795", "name": "Computer Science", "coefficient": 5, "category": "Principal" },
                    { "code": "0796", "name": "Information and Communication Technology (ICT)", "coefficient": 4, "category": "Specialty" },
                    { "code": "0780", "name": "Physics", "coefficient": 5, "category": "Principal" },
                    { "code": "0765", "name": "Further Mathematics", "coefficient": 5, "category": "Elective" }
                  ]
                }
              ]
            },
            {
              "code": "ART",
              "name": "Arts, Humanities & Social Sciences Stream",
              "series": [
                {
                  "code": "A1",
                  "name": "Series A1 (Literature, History & French)",
                  "description": "International Relations, Diplomacy, Law, Translation & Journalism track.",
                  "subjects": [
                    { "code": "0735", "name": "Literature in English", "coefficient": 5, "category": "Principal" },
                    { "code": "0760", "name": "History", "coefficient": 5, "category": "Principal" },
                    { "code": "0745", "name": "French", "coefficient": 5, "category": "Principal" },
                    { "code": "0772", "name": "Philosophy", "coefficient": 4, "category": "Elective" }
                  ]
                },
                {
                  "code": "A2",
                  "name": "Series A2 (History, Geography & Economics)",
                  "description": "Economics, Public Administration, Governance & Development Studies track.",
                  "subjects": [
                    { "code": "0760", "name": "History", "coefficient": 5, "category": "Principal" },
                    { "code": "0750", "name": "Geography", "coefficient": 5, "category": "Principal" },
                    { "code": "0725", "name": "Economics", "coefficient": 5, "category": "Principal" },
                    { "code": "0772", "name": "Philosophy", "coefficient": 4, "category": "Elective" }
                  ]
                },
                {
                  "code": "A3",
                  "name": "Series A3 (Literature, History & Religious Studies / Philosophy)",
                  "description": "Law, Magistracy, Humanities, Ethics & Education track.",
                  "subjects": [
                    { "code": "0735", "name": "Literature in English", "coefficient": 5, "category": "Principal" },
                    { "code": "0760", "name": "History", "coefficient": 5, "category": "Principal" },
                    { "code": "0785", "name": "Religious Studies", "coefficient": 5, "category": "Principal" },
                    { "code": "0772", "name": "Philosophy", "coefficient": 4, "category": "Principal" }
                  ]
                },
                {
                  "code": "A4",
                  "name": "Series A4 (Economics, Pure Mathematics & History/Geography/ICT)",
                  "description": "Corporate Finance, Banking, Business Analytics, Accounting & Management track.",
                  "subjects": [
                    { "code": "0725", "name": "Economics", "coefficient": 5, "category": "Principal" },
                    { "code": "0775", "name": "Pure Mathematics With Statistics", "coefficient": 5, "category": "Principal" },
                    { "code": "0750", "name": "Geography", "coefficient": 5, "category": "Principal" },
                    { "code": "0760", "name": "History", "coefficient": 4, "category": "Elective" },
                    { "code": "0796", "name": "Information and Communication Technology (ICT)", "coefficient": 4, "category": "Elective" }
                  ]
                },
                {
                  "code": "A5",
                  "name": "Series A5 (Philosophy, Literature & French / ICT)",
                  "description": "Mass Communication, Sociology, Political Science & Bilingual Studies track.",
                  "subjects": [
                    { "code": "0772", "name": "Philosophy", "coefficient": 5, "category": "Principal" },
                    { "code": "0735", "name": "Literature in English", "coefficient": 5, "category": "Principal" },
                    { "code": "0745", "name": "French", "coefficient": 5, "category": "Principal" },
                    { "code": "0796", "name": "Information and Communication Technology (ICT)", "coefficient": 4, "category": "Elective" }
                  ]
                }
              ]
            }
          ]
        }
      ]
    },
    {
      "code": "FRANCOPHONE",
      "name": "Francophone Educational System (OBC / ESG)",
      "levels": [
        {
          "code": "ORDINARY_LEVEL",
          "name": "1er Cycle Secondaire (6ème à 3ème / BEPC)",
          "streams": [
            {
              "code": "SCIENCE",
              "name": "Orientation Scientifique & Technologique (1er Cycle)",
              "series": [
                {
                  "code": "BEPC_SCI",
                  "name": "3ème / BEPC Dominante Scientifique",
                  "description": "Préparation au BEPC et à l'entrée en Seconde C / TI.",
                  "subjects": [
                    { "code": "MATH-1C", "name": "Mathématiques", "coefficient": 4, "category": "Obligatoire" },
                    { "code": "PCT-1C", "name": "Physique-Chimie-Technologie (PCT)", "coefficient": 3, "category": "Fondamental" },
                    { "code": "SVT-1C", "name": "Sciences de la Vie et de la Terre (SVTEEHB)", "coefficient": 3, "category": "Fondamental" },
                    { "code": "INFO-1C", "name": "Informatique", "coefficient": 2, "category": "Fondamental" },
                    { "code": "FR-1C", "name": "Français (Étude de Texte, Dictée, Rédaction)", "coefficient": 4, "category": "Obligatoire" },
                    { "code": "ANG-1C", "name": "Anglais", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "HG-1C", "name": "Histoire-Géographie", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "ECM-1C", "name": "Éducation à la Citoyenneté et à la Morale (ECM)", "coefficient": 2, "category": "Obligatoire" }
                  ]
                }
              ]
            },
            {
              "code": "ART",
              "name": "Orientation Littéraire & Langues (1er Cycle)",
              "series": [
                {
                  "code": "BEPC_LIT",
                  "name": "3ème / BEPC Allemand, Espagnol, Chinois, Arabe & Lettres",
                  "description": "Préparation au BEPC et à l'entrée en Seconde A4 / ABI.",
                  "subjects": [
                    { "code": "FR-1C", "name": "Français (Littérature, Dictée, Expression Écrite)", "coefficient": 5, "category": "Obligatoire" },
                    { "code": "ANG-1C", "name": "Anglais", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "LV2-ESP", "name": "Espagnol (Langue Vivante II)", "coefficient": 3, "category": "Spécialité" },
                    { "code": "LV2-ALL", "name": "Allemand (Langue Vivante II)", "coefficient": 3, "category": "Spécialité" },
                    { "code": "LV2-CHI", "name": "Chinois / Arabe / Italien (LV2)", "coefficient": 3, "category": "Optionnel" },
                    { "code": "HG-1C", "name": "Histoire-Géographie", "coefficient": 3, "category": "Fondamental" },
                    { "code": "ECM-1C", "name": "Éducation à la Citoyenneté et à la Morale (ECM)", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "MATH-1C", "name": "Mathématiques", "coefficient": 4, "category": "Obligatoire" },
                    { "code": "INFO-1C", "name": "Informatique", "coefficient": 2, "category": "Obligatoire" }
                  ]
                }
              ]
            }
          ]
        },
        {
          "code": "ADVANCED_LEVEL",
          "name": "2nd Cycle (Seconde, Première / Probatoire & Terminale / Baccalauréat)",
          "streams": [
            {
              "code": "SCIENCE",
              "name": "Séries Scientifiques & Technologiques (C, D, TI, E)",
              "series": [
                {
                  "code": "SERIE_C",
                  "name": "Série C (Mathématiques & Sciences Physiques)",
                  "description": "Polytechnique, Ingénierie, Architecture, Aéronautique & Sciences Exactes.",
                  "subjects": [
                    { "code": "MATH-C", "name": "Mathématiques", "coefficient": 7, "category": "Spécialité" },
                    { "code": "PHYS-C", "name": "Physique", "coefficient": 5, "category": "Spécialité" },
                    { "code": "CHIM-C", "name": "Chimie", "coefficient": 3, "category": "Spécialité" },
                    { "code": "SVT-C", "name": "Sciences de la Vie et de la Terre (SVTEEHB)", "coefficient": 2, "category": "Fondamental" },
                    { "code": "INFO-C", "name": "Informatique", "coefficient": 2, "category": "Fondamental" },
                    { "code": "PHILO-C", "name": "Philosophie", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "FR-C", "name": "Littérature / Français", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "ANG-C", "name": "Anglais", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "HG-C", "name": "Histoire-Géographie", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "ECM-C", "name": "Éducation à la Citoyenneté (ECM)", "coefficient": 2, "category": "Obligatoire" }
                  ]
                },
                {
                  "code": "SERIE_D",
                  "name": "Série D (SVTEEHB, Chimie, Physique & Mathématiques)",
                  "description": "Médecine (FMSB), Pharmacie, Biomédical, Agronomie (FASA) & Biotechnologies.",
                  "subjects": [
                    { "code": "SVT-D", "name": "Sciences de la Vie et de la Terre (SVTEEHB)", "coefficient": 5, "category": "Spécialité" },
                    { "code": "MATH-D", "name": "Mathématiques", "coefficient": 4, "category": "Spécialité" },
                    { "code": "PHYS-D", "name": "Physique", "coefficient": 3, "category": "Spécialité" },
                    { "code": "CHIM-D", "name": "Chimie", "coefficient": 3, "category": "Spécialité" },
                    { "code": "INFO-D", "name": "Informatique", "coefficient": 2, "category": "Fondamental" },
                    { "code": "PHILO-D", "name": "Philosophie", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "FR-D", "name": "Littérature / Français", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "ANG-D", "name": "Anglais", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "HG-D", "name": "Histoire-Géographie", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "ECM-D", "name": "Éducation à la Citoyenneté (ECM)", "coefficient": 2, "category": "Obligatoire" }
                  ]
                },
                {
                  "code": "SERIE_TI",
                  "name": "Série TI (Technologies de l'Information & Génie Logiciel)",
                  "description": "Génie Logiciel, Intelligence Artificielle, Réseaux, Cybersécurité & Télécoms.",
                  "subjects": [
                    { "code": "INFO-TI", "name": "Algorithmique, Programmation & Bases de Données", "coefficient": 6, "category": "Spécialité" },
                    { "code": "RES-TI", "name": "Architecture des Ordinateurs, Réseaux & Systèmes", "coefficient": 4, "category": "Spécialité" },
                    { "code": "MATH-TI", "name": "Mathématiques", "coefficient": 5, "category": "Spécialité" },
                    { "code": "PHYS-TI", "name": "Physique", "coefficient": 4, "category": "Spécialité" },
                    { "code": "CHIM-TI", "name": "Chimie", "coefficient": 2, "category": "Fondamental" },
                    { "code": "FR-TI", "name": "Littérature / Français", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "ANG-TI", "name": "Anglais", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "PHILO-TI", "name": "Philosophie", "coefficient": 2, "category": "Obligatoire" }
                  ]
                },
                {
                  "code": "SERIE_E",
                  "name": "Série E (Mathématiques & Construction Mécanique)",
                  "description": "Génie Mécanique, Génie Industriel, Électromécanique & Polytechnique.",
                  "subjects": [
                    { "code": "MATH-E", "name": "Mathématiques", "coefficient": 6, "category": "Spécialité" },
                    { "code": "PHYS-E", "name": "Sciences Physiques", "coefficient": 5, "category": "Spécialité" },
                    { "code": "MECA-E", "name": "Construction & Fabrication Mécanique", "coefficient": 5, "category": "Spécialité" },
                    { "code": "INFO-E", "name": "Informatique", "coefficient": 2, "category": "Fondamental" },
                    { "code": "FR-E", "name": "Français", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "ANG-E", "name": "Anglais", "coefficient": 3, "category": "Obligatoire" }
                  ]
                }
              ]
            },
            {
              "code": "ART",
              "name": "Séries Littéraires, Langues & Sciences Humaines (A4, A1, A2, A3, ABI)",
              "series": [
                {
                  "code": "SERIE_A4",
                  "name": "Série A4 (Allemand / Espagnol / Arabe / Chinois, Philosophie & Lettres)",
                  "description": "Droit, Diplomatie (IRIC), Administration (ENAM), Journalisme (ESSTIC) & Langues.",
                  "subjects": [
                    { "code": "LIT-A4", "name": "Littérature et Langue Française", "coefficient": 5, "category": "Spécialité" },
                    { "code": "PHILO-A4", "name": "Philosophie", "coefficient": 5, "category": "Spécialité" },
                    { "code": "LV2-A4", "name": "Langue Vivante II (Espagnol / Allemand / Arabe / Chinois)", "coefficient": 4, "category": "Spécialité" },
                    { "code": "ANG-A4", "name": "Anglais", "coefficient": 4, "category": "Spécialité" },
                    { "code": "HIST-A4", "name": "Histoire", "coefficient": 3, "category": "Fondamental" },
                    { "code": "GEO-A4", "name": "Géographie", "coefficient": 3, "category": "Fondamental" },
                    { "code": "ECM-A4", "name": "Éducation à la Citoyenneté (ECM)", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "MATH-A4", "name": "Mathématiques Appliquées", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "INFO-A4", "name": "Informatique", "coefficient": 2, "category": "Obligatoire" }
                  ]
                },
                {
                  "code": "SERIE_ABI",
                  "name": "Série ABI (Baccalauréat Bilingue & Lettres Modernes)",
                  "description": "Traduction & Interprétation (ASTI), Relations Internationales & Droit Bilingue.",
                  "subjects": [
                    { "code": "ANG-ABI", "name": "Anglais Intensif & Literature in English", "coefficient": 5, "category": "Spécialité" },
                    { "code": "LIT-ABI", "name": "Littérature et Langue Française", "coefficient": 5, "category": "Spécialité" },
                    { "code": "PHILO-ABI", "name": "Philosophie", "coefficient": 4, "category": "Spécialité" },
                    { "code": "HG-ABI", "name": "Histoire-Géographie", "coefficient": 4, "category": "Fondamental" },
                    { "code": "LV2-ABI", "name": "Langue Vivante II (Espagnol / Allemand)", "coefficient": 3, "category": "Fondamental" },
                    { "code": "MATH-ABI", "name": "Mathématiques", "coefficient": 2, "category": "Obligatoire" },
                    { "code": "INFO-ABI", "name": "Informatique", "coefficient": 2, "category": "Obligatoire" }
                  ]
                },
                {
                  "code": "SERIE_A1_A2_A3",
                  "name": "Séries A1 / A2 / A3 (Lettres Classiques: Latin, Grec & Humanités)",
                  "description": "Droit, Magistrature, Lettres Classiques, Philosophie & Sciences Politiques.",
                  "subjects": [
                    { "code": "LAT-A", "name": "Latin & Civilisation Classique", "coefficient": 4, "category": "Spécialité" },
                    { "code": "GREC-A", "name": "Grec Ancien / Langue Vivante II", "coefficient": 4, "category": "Spécialité" },
                    { "code": "LIT-A", "name": "Littérature Française", "coefficient": 5, "category": "Spécialité" },
                    { "code": "PHILO-A", "name": "Philosophie", "coefficient": 5, "category": "Spécialité" },
                    { "code": "ANG-A", "name": "Anglais", "coefficient": 3, "category": "Obligatoire" },
                    { "code": "HG-A", "name": "Histoire-Géographie", "coefficient": 4, "category": "Fondamental" },
                    { "code": "MATH-A3", "name": "Mathématiques (Renforcé A3)", "coefficient": 4, "category": "Spécialité" }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
"""
}

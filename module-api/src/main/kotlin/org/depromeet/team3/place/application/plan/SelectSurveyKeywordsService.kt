package org.depromeet.team3.place.application.plan

import org.depromeet.team3.place.application.plan.CreateSurveyKeywordService.KeywordCandidate
import org.depromeet.team3.place.application.model.PlaceSurveySummary
import org.springframework.stereotype.Service
import java.util.LinkedHashMap

/**
 *  역 이름과 카테고리를 조합하여 “검색 키워드 문구” 생성
 *  응답 비율을 기반으로 가중치(weight) 부여
 *  최대 5개 키워드 반환
 */
@Service
class SelectSurveyKeywordsService {

    private val maxKeywordCount = 5
    private val strongLeafSupportThreshold = 0.6
    private val branchSupportThreshold = 0.5
    private val minimalKeywordWeight = 0.1

    fun selectKeywords(aggregate: PlaceSurveySummary): List<KeywordCandidate> {
        if (aggregate.totalRespondents == 0) {
            return emptyList()
        }

        val keywordMap = LinkedHashMap<String, KeywordCandidate>()
        val total = aggregate.totalRespondents.toDouble()

        fun addCandidate(rawKeyword: String, weight: Double) {
            val normalized = CreateSurveyKeywordService.normalizeKeyword(rawKeyword)
            if (normalized.isEmpty() || keywordMap.containsKey(normalized)) return
            keywordMap[normalized] = KeywordCandidate(normalized, weight.coerceIn(0.0, 1.0))
        }

        aggregate.leafVotes.entries
            .filter { it.value == aggregate.totalRespondents }
            .sortedByDescending { aggregate.leafCategories[it.key]?.sortOrder ?: Int.MAX_VALUE }
            .forEach { entry ->
                val leaf = aggregate.leafCategories[entry.key] ?: return@forEach
                addCandidate(buildLeafKeyword(aggregate.stationName, leaf.name), 1.0)
            }

        aggregate.leafVotes.entries
            .filter { it.value < aggregate.totalRespondents }
            .sortedByDescending { it.value }
            .forEach { entry ->
                val leaf = aggregate.leafCategories[entry.key] ?: return@forEach
                val ratio = entry.value.toDouble() / total
                if (ratio >= strongLeafSupportThreshold) {
                    addCandidate(buildLeafKeyword(aggregate.stationName, leaf.name), ratio)
                }
            }

        aggregate.branchVotes.entries
            .sortedByDescending { it.value }
            .forEach { entry ->
                val branch = aggregate.branchCategories[entry.key] ?: return@forEach
                val ratio = entry.value.toDouble() / total
                if (ratio >= branchSupportThreshold) {
                    addCandidate(buildBranchKeyword(aggregate.stationName, branch.name), ratio)
                }
            }

        if (keywordMap.size < maxKeywordCount) {
            aggregate.leafVotes.entries
                .sortedByDescending { it.value }
                .forEach { entry ->
                    val leaf = aggregate.leafCategories[entry.key] ?: return@forEach
                    val ratio = entry.value.toDouble() / total
                    addCandidate(buildLeafKeyword(aggregate.stationName, leaf.name), ratio)
                    if (keywordMap.size >= maxKeywordCount) return@forEach
                }
        }

        if (keywordMap.isEmpty() || keywordMap.size < maxKeywordCount) {
            addCandidate(buildGeneralKeyword(aggregate.stationName), minimalKeywordWeight)
        }

        return keywordMap.values.take(maxKeywordCount)
    }

    private fun buildLeafKeyword(stationName: String, leafName: String): String =
        "$stationName $leafName 맛집"

    private fun buildBranchKeyword(stationName: String, branchName: String): String =
        "$stationName $branchName 맛집"

    private fun buildGeneralKeyword(stationName: String): String =
        "$stationName 맛집"
}


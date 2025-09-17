package org.depromeet.team3.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.auth.model.KakaoResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class KakaoOAuthClient(
    private val objectMapper: ObjectMapper,
    private val kakaoProperties: KakaoProperties
) {
    private val log = LoggerFactory.getLogger(KakaoOAuthClient::class.java)

    private val allowedRedirectUris = setOf(
        "http://localhost:3000/auth/callback",
        "http://localhost:8080/auth/callback",
        "https://api.momuzzi.site/auth/callback",
        "https://www.momuzzi.site/auth/callback"
    )

    /**
     * 인가 코드를 이용해 카카오 서버로부터 OAuth 토큰 반환 받음.
     * 추후 OAuth Token 을 이용해, 카카오 서버로부터 사용자 정보 반환 => DB 저장 및 자체 인증/인가 로직
     */
    fun requestToken(accessCode: String, redirectUri: String): KakaoResponse.OAuthToken {
        val trimmedRedirectUri = redirectUri.trim()

        if (!allowedRedirectUris.contains(trimmedRedirectUri)) {
            log.error("허용되지 않은 redirect_uri: {}", trimmedRedirectUri)
            throw AuthException(ErrorCode.KAKAO_INVALID_GRANT)
        }


        val decodedAccessCode = accessCode

        // 요청 헤더 및 파라미터 구성
        val restTemplate = RestTemplate()
        val headers = HttpHeaders().apply {
            add("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
        }

        val params: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", kakaoProperties.clientId)
            add("redirect_uri", trimmedRedirectUri)
            add("code", decodedAccessCode)
        }


        val kakaoTokenRequest = HttpEntity(params, headers)

        return try {
            val response = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String::class.java
            )

            if (response.statusCode != HttpStatus.OK) {
                throw AuthException(ErrorCode.KAKAO_AUTH_FAILED)
            }

            objectMapper.readValue(response.body, KakaoResponse.OAuthToken::class.java)

        } catch (e: HttpClientErrorException.Unauthorized) {
            log.error("카카오 인증 실패 (401): {}", e.responseBodyAsString)
            throw AuthException(ErrorCode.KAKAO_INVALID_GRANT)
        } catch (e: HttpClientErrorException.BadRequest) {
            log.error("카카오 API Bad Request (400): {}", e.responseBodyAsString)
            throw AuthException(ErrorCode.KAKAO_INVALID_GRANT)
        } catch (e: HttpClientErrorException) {
            log.error("카카오 API HTTP 에러 - 상태코드: {}", e.statusCode)
            throw AuthException(ErrorCode.KAKAO_API_ERROR)
        } catch (e: Exception) {
            when (e.javaClass.simpleName) {
                "JsonProcessingException" -> {
                    log.error("카카오 응답 JSON 파싱 오류: {}", e.message)
                    throw AuthException(ErrorCode.KAKAO_JSON_PARSE_ERROR)
                }
                else -> {
                    log.error("카카오 API 호출 중 오류 발생: {}", e.message)
                    throw AuthException(ErrorCode.KAKAO_API_ERROR)
                }
            }
        }
    }

    /**
     * access token 을 사용해 카카오 사용자 정보 요청
     */
    fun requestProfile(oAuthToken: KakaoResponse.OAuthToken?): KakaoResponse.KakaoProfile {
        if (oAuthToken?.access_token == null) {
            throw AuthException(ErrorCode.KAKAO_AUTH_FAILED)
        }

        val restTemplate = RestTemplate()
        val headers = HttpHeaders().apply {
            add("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
            add("Authorization", "Bearer ${oAuthToken.access_token}")
        }
        val requestEntity = HttpEntity<Void>(headers)

        return try {
            val response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                requestEntity,
                String::class.java
            )

            objectMapper.readValue(response.body, KakaoResponse.KakaoProfile::class.java)
        } catch (e: Exception) {
            when (e.javaClass.simpleName) {
                "JsonProcessingException" -> {
                    log.error("카카오 프로필 파싱 오류: {}", e.message)
                    throw AuthException(ErrorCode.KAKAO_JSON_PARSE_ERROR)
                }
                else -> {
                    log.error("카카오 프로필 요청 중 오류 발생: {}", e.message)
                    throw AuthException(ErrorCode.KAKAO_API_ERROR)
                }
            }
        }
    }
}

package org.depromeet.team3.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.auth.exception.AuthException
import org.depromeet.team3.auth.model.KakaoResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Component
class KakaoOAuthClient(
    private val objectMapper: ObjectMapper,
    private val kakaoProperties: KakaoProperties
) {
    private val log = LoggerFactory.getLogger(KakaoOAuthClient::class.java)

    private val allowedRedirectUris = setOf(
        "http://localhost:8080/api/auth/kakao-login"
    )

    /**
     * 인가 코드를 이용해 카카오 서버로부터 OAuth 토큰 반환 받음.
     * 추후 OAuth Token 을 이용해, 카카오 서버로부터 사용자 정보 반환 => DB 저장 및 자체 인증/인가 로직
     */

    fun requestToken(accessCode: String, redirectUri: String): KakaoResponse.OAuthToken {
        val trimmedRedirectUri = redirectUri.trim()

        log.info("[🔍DEBUG🔍] 토큰 요청 시작")
        log.info("- 받은 인가코드: {}", accessCode)
        log.info("- 받은 redirect_uri: {}", redirectUri)
        log.info("- 트림된 redirect_uri: {}", trimmedRedirectUri)

        if (!allowedRedirectUris.contains(trimmedRedirectUri)) {
            log.error("[🚨ERROR🚨] 허용되지 않은 redirect_uri 요청: {}", trimmedRedirectUri)
            log.error("허용된 URI 목록: {}", allowedRedirectUris)
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
            kakaoProperties.clientSecret?.takeIf { it.isNotBlank() }?.let {
                add("client_secret", it)
                log.info("- client_secret 포함됨")
            }
        }

        log.info("[📤REQUEST📤] 카카오 토큰 요청 파라미터")
        params.forEach { (key, values) ->
            if (key == "code") {
                log.info("- {}: {}...{} (길이: {})", key, values[0].substring(0, 10), 
                    values[0].substring(values[0].length - 10), values[0].length)
            } else {
                log.info("- {}: {}", key, values[0])
            }
        }

        val kakaoTokenRequest = HttpEntity(params, headers)

        return try {
            val response = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String::class.java
            )

            log.info("[📥RESPONSE📥] 카카오 API 응답 상태: {}", response.statusCode)
            log.info("응답 본문: {}", response.body)

            if (response.statusCode != HttpStatus.OK) {
                throw AuthException(ErrorCode.KAKAO_AUTH_FAILED)
            }

            objectMapper.readValue(response.body, KakaoResponse.OAuthToken::class.java)

        } catch (e: HttpClientErrorException.Unauthorized) {
            log.error("[🚨ERROR🚨] 유효하지 않은 카카오 인증 코드 (401 Unauthorized)")
            log.error("카카오 에러 응답: {}", e.responseBodyAsString)
            throw AuthException(ErrorCode.KAKAO_INVALID_GRANT)
        } catch (e: HttpClientErrorException.BadRequest) {
            log.error("[🚨ERROR🚨] 카카오 API Bad Request (400)")
            log.error("카카오 에러 응답: {}", e.responseBodyAsString)
            throw AuthException(ErrorCode.KAKAO_INVALID_GRANT)
        } catch (e: HttpClientErrorException) {
            log.error("[🚨ERROR🚨] 카카오 API HTTP 에러 - 상태코드: {}, 응답: {}", e.statusCode, e.responseBodyAsString)
            throw AuthException(ErrorCode.KAKAO_API_ERROR)
        } catch (e: Exception) {
            when (e.javaClass.simpleName) {
                "JsonProcessingException" -> {
                    log.error("[🚨ERROR🚨] 카카오 응답 JSON 파싱 오류: {}", e.message)
                    throw AuthException(ErrorCode.KAKAO_JSON_PARSE_ERROR)
                }
                else -> {
                    log.error("[🚨ERROR🚨] 카카오 API 호출 중 오류 발생: {}", e.message, e)
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
                    log.error("[🚨ERROR🚨] 카카오 프로필 파싱 오류: {}", e.message)
                    throw AuthException(ErrorCode.KAKAO_JSON_PARSE_ERROR)
                }
                else -> {
                    log.error("[🚨ERROR🚨] 카카오 프로필 요청 중 오류 발생: {}", e.message)
                    throw AuthException(ErrorCode.KAKAO_API_ERROR)
                }
            }
        }
    }

}

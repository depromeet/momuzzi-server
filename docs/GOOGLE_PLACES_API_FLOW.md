# Google Places API 호출 흐름 및 재시도 백오프 로직

## 📋 개요

이 문서는 Google Places API 호출 시 사용되는 지수 백오프(Exponential Backoff) 재시도 로직과 전체 호출 흐름을 설명합니다.

---

## 🔄 전체 호출 흐름

```
[Controller] → [SearchPlacesService] → [PlaceQuery] → [GooglePlacesClient] → [Google Places API]
                                                              ↓
                                                    [재시도 로직]
                                                              ↓
                                                    [지수 백오프]
```

### 1. 상세 호출 흐름

#### 텍스트 검색 시나리오

```
1. SearchPlacesService.textSearch()
   ├─ meetingId가 있으면 Station 좌표 조회
   └─ PlaceQuery.textSearch() 호출

2. PlaceQuery.textSearch()
   └─ GooglePlacesClient.textSearch() 호출

3. GooglePlacesClient.textSearch()
   ├─ Dispatchers.IO로 컨텍스트 전환
   ├─ retryWithExponentialBackoff() 래핑
   ├─ withTimeout(10초) 설정
   ├─ Google Places API POST /v1/places:searchText 호출
   └─ 재시도 로직 적용

4. PlaceDetails 배치 조회 시나리오
   ├─ PlaceQuery.getPlaceDetailsBatch()
   ├─ DB 캐시 확인
   ├─ 업데이트 필요한 장소: 병렬 API 호출 (async)
   ├─ 캐시 없는 장소: 병렬 API 호출 (async)
   └─ 각 API 호출마다 재시도 로직 적용
```

---

## ⚙️ 재시도 백오프 설정

### 기본 설정값

```kotlin
// API 호출 타임아웃
apiTimeoutMillis = 10_000L  // 10초

// 재시도 설정
maxRetries = 3              // 최대 3번 시도 (초기 1번 + 재시도 2번)
initialDelayMillis = 100L   // 초기 지연: 100ms
maxDelayMillis = 2000L      // 최대 지연: 2초
jitterMaxMillis = 100L      // 지터 최대값: 0~100ms
```

### 지수 백오프 + 지터 계산

```
시도 1: 즉시 실행 (초기 시도)
시도 2: (100ms + 0~100ms 지터) 대기 후 재시도
시도 3: (200ms + 0~100ms 지터) 대기 후 재시도 (100ms × 2)
시도 4: (400ms + 0~100ms 지터) 대기 후 재시도 (200ms × 2)
시도 5: (800ms + 0~100ms 지터) 대기 후 재시도 (400ms × 2)
시도 6: (1600ms + 0~100ms 지터) 대기 후 재시도 (800ms × 2)
시도 7: (2000ms + 0~100ms 지터) 대기 후 재시도 (maxDelayMillis로 제한)
```

**최대 3번 시도 (초기 1번 + 재시도 2번)**
- 시도 1: 즉시 실행
- 시도 2: 100~200ms 대기 후 재시도 (100ms + 0~100ms 지터)
- 시도 3: 200~300ms 대기 후 재시도 (200ms + 0~100ms 지터)

**지터(Jitter) 효과**
- 동시에 여러 클라이언트가 재시도할 때 서로 다른 시간에 재시도하여 **thundering herd 문제 방지**
- 재시도 타이밍을 랜덤하게 분산하여 서버 부하 감소

---

## 🔍 재시도 로직 분기

### 재시도 가능한 오류

| HTTP 상태 코드 | 의미 | 재시도 여부 |
|---------------|------|------------|
| 429 | Too Many Requests | ✅ 재시도 |
| 500 | Internal Server Error | ✅ 재시도 |
| 501 | Not Implemented | ✅ 재시도 |
| 502 | Bad Gateway | ✅ 재시도 |
| 503 | Service Unavailable | ✅ 재시도 |
| 504 | Gateway Timeout | ✅ 재시도 |
| - | RestClientException (네트워크 오류) | ✅ 재시도 |
| - | 일반 Exception | ✅ 재시도 |

### 재시도하지 않는 오류 (즉시 실패)

| HTTP 상태 코드 | 의미 | 처리 |
|---------------|------|------|
| 401 | Unauthorized | PlaceSearchException(PLACE_API_KEY_INVALID) |
| 404 | Not Found | PlaceSearchException(PLACE_DETAILS_NOT_FOUND) |
| - | TimeoutCancellationException | PlaceSearchException(PLACE_API_ERROR) |
| - | PlaceSearchException | 그대로 전파 |

---

## 📊 재시도 로직 흐름도

```
[API 호출 시작]
    ↓
[withTimeout(10초) 설정]
    ↓
[HTTP 요청 실행]
    ↓
    ├─ ✅ 성공 → [결과 반환]
    │
    ├─ ❌ HttpClientErrorException
    │   ├─ 401/404 → [즉시 PlaceSearchException으로 변환하여 실패]
    │   ├─ 429/500-504 → [재시도 로직으로 이동]
    │   └─ 기타 → [즉시 실패]
    │
    ├─ ❌ RestClientException (네트워크 오류)
    │   └─ [재시도 로직으로 이동]
    │
    ├─ ❌ TimeoutCancellationException
    │   └─ [즉시 PlaceSearchException으로 변환하여 실패]
    │
    └─ ❌ PlaceSearchException
        └─ [그대로 전파]

[재시도 로직]
    ↓
[attempt < maxRetries?]
    ├─ Yes → [delay(delayMillis) 대기]
    │        [delayMillis = min(delayMillis × 2, maxDelayMillis)]
    │        [다시 API 호출 시도]
    │
    └─ No → [모든 재시도 실패]
            [PlaceSearchException으로 변환하여 실패]
```

---

## 💻 코드 예시

### 재시도 로직 핵심 코드

```kotlin
private suspend fun <T> retryWithExponentialBackoff(
    operation: String,
    operationDetail: String = "",
    block: suspend () -> T
): T {
    var lastException: Exception? = null
    var delayMillis = initialDelayMillis  // 100ms

    for (attempt in 0 until maxRetries) {  // 최대 3번 시도
        try {
            return block()  // API 호출 성공 시 즉시 반환
        } catch (e: HttpClientErrorException) {
            val statusCode = e.statusCode.value()
            
            // 재시도하지 않을 오류
            if (statusCode in listOf(401, 404)) {
                throw e
            }
            
            // 재시도 가능한 오류
            if (statusCode == 429 || statusCode in 500..504) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    logger.warn("재시도 (${attempt + 1}/$maxRetries) - ${delayMillis}ms 후 재시도")
                    delay(delayMillis)
                    delayMillis = minOf(delayMillis * 2, maxDelayMillis)  // 지수 백오프
                }
            }
        } catch (e: RestClientException) {
            // 네트워크 오류도 재시도
            lastException = e
            if (attempt < maxRetries - 1) {
                delay(delayMillis)
                delayMillis = minOf(delayMillis * 2, maxDelayMillis)
            }
        }
    }
    
    // 모든 재시도 실패 시 PlaceSearchException으로 변환
    throw PlaceSearchException(...)
}
```

### 실제 API 호출 예시

```kotlin
suspend fun getPlaceDetails(placeId: String): PlaceDetailsResponse = 
    withContext(Dispatchers.IO) {
        retryWithExponentialBackoff(
            operation = "상세 정보 조회",
            operationDetail = "placeId=$placeId"
        ) {
            try {
                withTimeout(apiTimeoutMillis) {  // 10초 타임아웃
                    val response = googlePlacesRestClient.get()
                        .uri("/v1/places/{placeId}?languageCode=ko", placeId)
                        .header("X-Goog-Api-Key", googlePlacesApiProperties.apiKey)
                        .header("X-Goog-FieldMask", fieldMask)
                        .retrieve()
                        .body(PlaceDetailsResponse::class.java)
                    
                    response ?: throw PlaceSearchException(...)
                    response
                }
            } catch (e: TimeoutCancellationException) {
                // 타임아웃은 재시도하지 않음
                throw PlaceSearchException(...)
            }
        }
    }
```

---

## 🎯 부분 실패 처리 (supervisorScope)

### 배치 조회 시 부분 실패 처리

```kotlin
// PlaceQuery.getPlaceDetailsBatch()
suspend fun getPlaceDetailsBatch(placeIds: List<String>): Map<String, PlaceDetailsResponse> = 
    coroutineScope {
        // 병렬로 여러 장소 조회
        val apiResults = placeIds.map { placeId ->
            async(Dispatchers.IO) {
                try {
                    googlePlacesClient.getPlaceDetails(placeId)  // 각각 재시도 로직 적용
                } catch (e: Exception) {
                    // 개별 실패는 로깅만 하고 null 반환
                    logger.warn("Place 조회 실패: placeId=$placeId")
                    null
                }
            }
        }.awaitAll().filterNotNull()  // 실패한 것은 제외
        
        // 성공한 것만 반환
        apiResults.toMap()
    }
```

### SearchPlacesService에서의 부분 실패 처리

```kotlin
suspend fun textSearch(request: PlacesSearchRequest): PlacesSearchResponse = 
    supervisorScope {  // 일부 실패해도 계속 진행
        // 여러 장소의 상세 정보를 병렬로 조회
        val allPlaceDetails = placeDetailsProcessor.fetchPlaceDetailsInParallel(places)
        
        // 일부 실패해도 성공한 것만 반환
        // (10개 요청 중 1개 실패해도 9개는 반환)
    }
```

---

## 📈 실제 시나리오 예시

### 시나리오 1: 정상 호출

```
1. API 호출 → 200 OK
2. 결과 반환 (재시도 없음)
총 시간: ~500ms
```

### 시나리오 2: 일시적 오류 후 성공

```
1. API 호출 → 500 Internal Server Error
2. 100ms 대기
3. 재시도 → 200 OK
총 시간: ~600ms (100ms 대기 + API 호출)
```

### 시나리오 3: 여러 번 재시도 후 성공

```
1. API 호출 → 502 Bad Gateway
2. 100ms 대기
3. 재시도 → 503 Service Unavailable
4. 200ms 대기
5. 재시도 → 200 OK
총 시간: ~900ms (100ms + 200ms 대기 + API 호출)
```

### 시나리오 4: 최대 재시도 후 실패

```
1. API 호출 → 500 Internal Server Error
2. 100ms 대기
3. 재시도 → 500 Internal Server Error
4. 200ms 대기
5. 재시도 → 500 Internal Server Error
6. PlaceSearchException(PLACE_API_ERROR) 발생
총 시간: ~900ms (100ms + 200ms 대기 + API 호출)
```

### 시나리오 5: 타임아웃

```
1. API 호출 → 10초 경과
2. TimeoutCancellationException 발생
3. 즉시 PlaceSearchException(PLACE_API_ERROR) 발생
4. 재시도 없음
총 시간: 10초
```

### 시나리오 6: 인증 오류 (401)

```
1. API 호출 → 401 Unauthorized
2. 즉시 PlaceSearchException(PLACE_API_KEY_INVALID) 발생
3. 재시도 없음
총 시간: ~500ms
```

---

## 🔧 주요 개선 사항

### 1. 지수 백오프 재시도
- 일시적 오류(429, 500-504, 네트워크 오류)에 대해 자동 재시도
- 지수적으로 증가하는 대기 시간으로 서버 부하 감소

### 2. 타임아웃 설정
- 모든 API 호출에 10초 타임아웃 적용
- 무한 대기 방지

### 3. 부분 실패 처리
- `supervisorScope` 사용으로 일부 실패해도 나머지 결과 반환
- 배치 조회 시 개별 실패는 로깅만 하고 계속 진행

### 4. 일관된 예외 처리
- 모든 최종 실패는 `PlaceSearchException`으로 통일
- 에러 코드와 상세 정보 포함

### 5. 병렬 처리
- 배치 조회 시 `async`로 병렬 처리
- 각 API 호출마다 독립적으로 재시도 로직 적용

---

## 📝 로깅

### 재시도 시 로그

```
WARN: 텍스트 검색 재시도 (1/3) - 상태코드: 500, query=강남역 맛집, 100ms 후 재시도
WARN: 텍스트 검색 재시도 (2/3) - 상태코드: 500, query=강남역 맛집, 200ms 후 재시도
ERROR: 텍스트 검색 최종 실패 (3회 재시도 후), query=강남역 맛집
```

### 부분 실패 로그

```
WARN: Place 상세 정보 조회 실패: placeId=place_1, errorCode=P002, message=...
WARN: Place 상세 정보 배치 조회 부분 실패: 성공=9, 실패=1, 전체=10
```

---

## ⚠️ 주의사항

1. **재시도 횟수**: 최대 3번 시도 (초기 1번 + 재시도 2번)
2. **타임아웃**: 각 시도마다 10초 타임아웃 적용
3. **최악의 경우**: 3번 × 10초 = 최대 30초 + 대기 시간(300ms) = 약 30.3초
4. **부분 실패**: 일부 장소 조회 실패해도 나머지는 반환
5. **인증 오류**: 401/404는 즉시 실패 (재시도 없음)

---

## 📚 참고

- 재시도 로직: `GooglePlacesClient.retryWithExponentialBackoff()`
- API 호출: `GooglePlacesClient.textSearch()`, `getPlaceDetails()`, `searchNearby()`
- 배치 처리: `PlaceQuery.getPlaceDetailsBatch()`
- 부분 실패 처리: `SearchPlacesService.textSearch()` (supervisorScope)

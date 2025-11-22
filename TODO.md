# TEFAS API Middleware - To-Do Listesi

## Öncelik 1: API Yapısı Düzenlemeleri (Öncelikli)

### A. Controller Temizliği
- [ ] NavController'ı kaldır (duplicate, HistoryController kullanılacak)
- [ ] ComparisonController'ı kaldır (artık gerekli değil)

### B. HistoryParser Implementasyonu
- [ ] `HistoryParser.toPriceRows(String rawJson)` metodu oluştur
- [ ] TEFAS JSON formatını `List<PriceRowDto>`'ya çevir
- [ ] Error handling (HTML response, parse hataları)
- [ ] Date parsing (TEFAS formatından LocalDate'e)

### C. Service Layer Genişletme
- [ ] `TefasService` interface'ine `getFundNav(String code, LocalDate start, LocalDate end, Pageable pageable)` ekle
- [ ] `TefasServiceImpl`'de `getFundNav` implement et
- [ ] HistoryClient'tan raw JSON al, HistoryParser ile parse et
- [ ] Pagination logic ekle (Spring Data Pageable)

### D. HistoryController Güncelleme
- [ ] Raw JSON yerine `PagedResponse<PriceRowDto>` döndürsün
- [ ] Spring Data `Pageable` parametresi ekle (page, size)
- [ ] Service layer'dan veri alsın (raw JSON değil)
- [ ] `@Valid` ve `@NotNull` annotation'ları ekle
- [ ] Date format validation (`@DateTimeFormat`)

### E. Error Handling Standartlaştırma
- [ ] `NotFoundException` custom exception oluştur
- [ ] FundController'daki 404 response'u GlobalExceptionHandler'a taşı
- [ ] GlobalExceptionHandler'a `@ExceptionHandler(NotFoundException.class)` ekle
- [ ] Tüm error response'ları standart format: `{error, message, timestamp, traceId}`

### F. Swagger Dokümantasyonu
- [ ] HistoryController'a Swagger annotation'ları ekle (`@Operation`, `@ApiResponse`)
- [ ] FundController Swagger annotation'larını güncelle

---

## Öncelik 2: Production-Ready Özellikler

### G. Caching (Redis)
- [ ] `pom.xml`: Spring Data Redis ve Lettuce dependency ekle
- [ ] Redis cache configuration class oluştur (`CacheConfig.java`)
- [ ] TTL, key strategy yapılandırması
- [ ] `TefasServiceImpl`'e `@Cacheable` annotation'ları ekle
- [ ] Cache invalidation stratejisi belirle

### H. Retry Mekanizması
- [ ] `pom.xml`: Spring Retry dependency ekle
- [ ] Retry configuration class oluştur (`RetryConfig.java`)
- [ ] Exponential backoff stratejisi
- [ ] `FundsClient` ve diğer client'lara `@Retryable` ekle
- [ ] WAF hatalarında retry (max 3 deneme)
- [ ] Retryable exception'ları tanımla

### I. Rate Limiting
- [ ] `pom.xml`: Bucket4j dependency ekle
- [ ] Rate limiting configuration ve interceptor oluştur
- [ ] Per-user rate limiting (API key bazlı, opsiyonel)
- [ ] Global rate limiting fallback
- [ ] Rate limit headers (X-RateLimit-*)
- [ ] GlobalExceptionHandler'a rate limit exception handling ekle

### J. Configuration Management
- [ ] `application.properties`: Redis, rate limit, retry config ekle
- [ ] Environment variables desteği
- [ ] `application-docker.properties` profile oluştur

### K. Monitoring & Logging
- [ ] Structured logging yapılandırması (`logback-spring.xml`)
- [ ] Custom health indicators ekle (TEFAS connectivity, Redis)
- [ ] Actuator endpoints genişletme

### L. Error Handling İyileştirmeleri
- [ ] Retry sonrası hata handling
- [ ] Rate limit exceeded özel exception
- [ ] Cache miss handling

---

## Öncelik 3: Docker & Deployment (Son Adım)

### M. Docker Yapılandırması
- [ ] Dockerfile oluştur (multi-stage build, Playwright browser binary dahil)
- [ ] docker-compose.yml oluştur (Redis, Spring Boot app, network)
- [ ] .dockerignore dosyası oluştur

### N. Dokümantasyon
- [ ] README.md: Docker, caching, rate limiting, API örnekleri ekle
- [ ] Quick start guide
- [ ] API endpoint'leri dokümantasyonu
- [ ] Rate limiting açıklaması
- [ ] Caching stratejisi açıklaması
- [ ] API örnekleri (curl, Postman collection)

---

## Final Endpoint Yapısı

```
GET /v1/funds/{code}                    # Tek fon detayı (FundDto) ✓ ZATEN VAR
GET /v1/funds/{code}/nav?start=YYYY-MM-DD&end=YYYY-MM-DD&page=0&size=20  # NAV geçmişi (PagedResponse<PriceRowDto>)
```

---

## Notlar

- Tüm response'lar DTO formatında olacak (tutarlılık için)
- Error response'lar standart format: `{error, message, timestamp, traceId}`
- Pagination Spring Data Pageable kullanacak
- Nav endpoint'inde start ve end tarihleri zorunlu


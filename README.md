# TEFAS Fund API

Türkiye'deki yatırım fonları hakkında veri sağlayan RESTful API middleware. TEFAS (Takasbank Elektronik Fon Alım Satım Platformu) web sitesinden Playwright kullanarak veri çeker ve düzenlenmiş, kullanıma hazır JSON formatında sunar.

## 🚀 Özellikler

- **Fon Bilgileri**: Fon koduna göre detaylı fon bilgileri (getiri oranları, fon türü, vb.)
- **NAV Geçmişi**: Fonların tarihsel NAV (Net Aktif Değer) verileri
- **RESTful API**: Standart HTTP metodları ile kolay entegrasyon
- **Field Filtering**: İhtiyacınız olan alanları seçerek response boyutunu optimize edin
- **Swagger UI**: Interaktif API dokümantasyonu (`/docs`)
- **Error Handling**: Standartlaştırılmış hata yanıtları
- **Unit Tests**: Kapsamlı test kapsamı

## 📋 Gereksinimler

- Java 17 veya üzeri
- Maven 3.6+ (veya Maven Wrapper kullanın)
- Playwright browser binary'leri (otomatik indirilir)

## 🛠️ Kurulum

### Source Code ile

1. Repository'yi klonlayın:
```bash
git clone https://github.com/KULLANICI_ADI/tefas-fund-api.git
cd tefas-fund-api
```

2. Maven ile build edin:
```bash
./mvnw clean install
```

3. Uygulamayı çalıştırın:
```bash
./mvnw spring-boot:run
```

Uygulama `http://localhost:8080` adresinde çalışacaktır.

### Docker ile (Yakında)

```bash
docker run -p 8080:8080 tefas-fund-api
```

## 📚 API Endpoints

### Base URL
```
http://localhost:8080/v1
```

### 1. Fon Detayı

Belirli bir fonun detaylı bilgilerini getirir.

**Endpoint:** `GET /v1/funds/{code}`

**Parametreler:**
- `code` (path, required): Fon kodu (örn: `AAK`, `AOY`)
- `fields` (query, optional): Döndürülecek alanlar (virgülle ayrılmış)

**Örnek İstek:**
```bash
curl "http://localhost:8080/v1/funds/AAK"
```

**Örnek Response:**
```json
{
  "fundCode": "AAK",
  "fundName": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
  "umbrellaType": "Hisse Senedi Şemsiye Fonu",
  "getiri1A": 2.6331,
  "getiri3A": 15.3461,
  "getiri6A": 39.8057,
  "getiri1Y": 66.6525,
  "getiriYB": 65.8769,
  "getiri3Y": 52.8775,
  "getiri5Y": 159.3955
}
```

**Sadece Belirli Alanları İsteme:**
```bash
curl "http://localhost:8080/v1/funds/AAK?fields=fundCode,fundName,getiri1A,getiri3A"
```

### 2. NAV Geçmişi

Fonun belirli bir tarih aralığındaki NAV geçmişini getirir.

**Endpoint:** `GET /v1/funds/{code}/nav`

**Parametreler:**
- `code` (path, required): Fon kodu
- `start` (query, required): Başlangıç tarihi (YYYY-MM-DD formatında)
- `end` (query, required): Bitiş tarihi (YYYY-MM-DD formatında)

**Örnek İstek:**
```bash
curl "http://localhost:8080/v1/funds/AAK/nav?start=2024-01-01&end=2024-01-31"
```

**Örnek Response:**
```json
{
  "data": [
    {
      "date": "2024-01-02",
      "fundCode": "AAK",
      "fundName": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
      "price": 30.373708,
      "outstandingShares": 1096100,
      "totalValue": 33292621.25,
      "holderCount": 755
    }
  ]
}
```

## 📖 Swagger UI

Interaktif API dokümantasyonu için Swagger UI'ı kullanabilirsiniz:

```
http://localhost:8080/docs
```

## 🔧 Yapılandırma

`src/main/resources/application.properties` dosyasında yapılandırma ayarları:

```properties
# Server port
server.port=8080

# Application name
spring.application.name=tefas-proxy

# Swagger UI path
springdoc.swagger-ui.path=/docs

# Actuator endpoints
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

## 🧪 Test

Unit testleri çalıştırmak için:

```bash
./mvnw test
```

Belirli bir test sınıfını çalıştırmak için:

```bash
./mvnw test -Dtest=HistoryParserTest
```

## 📁 Proje Yapısı

```
tefas-fund-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/tefasfundapi/tefasFundAPI/
│   │   │       ├── client/          # TEFAS API client'ları (Playwright)
│   │   │       ├── controller/      # REST controllers
│   │   │       ├── dto/             # Data Transfer Objects
│   │   │       ├── parser/          # JSON parser'lar
│   │   │       ├── service/         # Business logic
│   │   │       └── filter/          # Field filtering
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/                    # Unit tests
├── pom.xml
└── README.md
```

## 🐛 Hata Yönetimi

API standart hata formatı kullanır:

```json
{
  "error": "NOT_FOUND",
  "message": "Fund not found: INVALID_CODE",
  "timestamp": "2025-01-22T21:00:00Z",
  "traceId": "uuid-here"
}
```

**Hata Kodları:**
- `NOT_FOUND`: İstenen kaynak bulunamadı
- `BAD_REQUEST`: Geçersiz istek parametreleri
- `INTERNAL_ERROR`: Sunucu hatası

## 🚧 Geliştirme Durumu

Bu proje aktif geliştirme aşamasındadır. Planlanan özellikler:

- [ ] Redis caching
- [ ] Rate limiting
- [ ] Retry mekanizması
- [ ] Docker support
- [ ] Pagination (NAV endpoint için)
- [ ] Authentication/Authorization

Detaylı geliştirme planı için `TODO.md` dosyasına bakın.

## 🤝 Katkıda Bulunma

Katkılarınızı bekliyoruz! Lütfen:

1. Fork edin
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit edin (`git commit -m 'Add some amazing feature'`)
4. Push edin (`git push origin feature/amazing-feature`)
5. Pull Request açın

## 📝 Lisans

Bu proje açık kaynaklıdır. Lisans bilgisi için `LICENSE` dosyasına bakın.

## ⚠️ Uyarı

Bu API, TEFAS web sitesinden veri çekmek için web scraping kullanır. TEFAS'ın kullanım şartlarına ve rate limiting politikalarına dikkat edin. Aşırı istek göndermekten kaçının.

## 📞 İletişim

Sorularınız veya önerileriniz için issue açabilirsiniz.

## 🙏 Teşekkürler

- [TEFAS](https://www.tefas.gov.tr/) - Veri kaynağı
- [Playwright](https://playwright.dev/) - Web automation
- [Spring Boot](https://spring.io/projects/spring-boot) - Framework

package com.tefasfundapi.tefasFundAPI.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tefasFundApiOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local development server");

        Contact contact = new Contact();
        contact.setName("TEFAS Fund API");
        contact.setUrl("https://github.com/KULLANICI_ADI/tefas-fund-api");

        License license = new License();
        license.setName("Apache 2.0");
        license.setUrl("https://www.apache.org/licenses/LICENSE-2.0.html");

        Info info = new Info()
                .title("TEFAS Fund Data API")
                .version("0.0.1-SNAPSHOT")
                .description("""
                        Türkiye'deki yatırım fonları hakkında veri sağlayan RESTful API Proxy.
                        TEFAS (Takasbank Elektronik Fon Alım Satım Platformu) web sitesinden
                        Playwright kullanarak veri çeker ve düzenlenmiş, kullanıma hazır JSON formatında sunar.
                        
                        ## Özellikler
                        - **Fon Bilgileri**: Fon koduna göre detaylı fon bilgileri (getiri oranları, fon türü, vb.)
                        - **NAV Geçmişi**: Fonların tarihsel NAV (Net Aktif Değer) verileri
                        - **Fon Performansı**: Tarih aralığına göre fon getirileri
                        - **Field Filtering**: İhtiyacınız olan alanları seçerek response boyutunu optimize edin
                        """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}

package mx.tsj.connect.general.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI presupuestoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Presupuesto API")
                        .version("v1")
                        .description("API del sistema presupuestal")
                        .contact(new Contact()
                                .name("Connect TSJ")))
                .addServersItem(new Server()
                        .url("http://localhost:3012")
                        .description("Local"));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/api/man", "/api/man/index.html");
    }
}

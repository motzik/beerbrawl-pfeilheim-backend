import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig: WebMvcConfigurer {

    @Override
    fun addCorsMapping(registry: CorsRegistry){
        registry.addMapping("/api/**") // Passe das Mapping an deine API an
            .allowedOrigins("*") // Erlaube alle Origin
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}

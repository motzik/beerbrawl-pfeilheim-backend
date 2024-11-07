import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class WebConfig {

    @Bean
    fun corsFilter(): CorsFilter {
        val corsConfiguration = CorsConfiguration()
        corsConfiguration.allowCredentials = true
        corsConfiguration.addAllowedOrigin("https://beerbrawl-pfeilheim-frontend.vercel.app") // specify frontend origin
        corsConfiguration.addAllowedHeader("*") // allow all headers
        corsConfiguration.addAllowedMethod("*") // allow all HTTP methods (POST, GET, etc.)

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfiguration)
        return CorsFilter(source)
    }
}

/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class LogConfiguration {

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> logFilter() {
        var reg = new FilterRegistrationBean<OncePerRequestFilter>(new LogFilter());
        reg.addUrlPatterns("/*");
        reg.setName("logFilter");
        reg.setOrder(Ordered.LOWEST_PRECEDENCE);
        return reg;
    }
}

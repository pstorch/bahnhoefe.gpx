package org.railwaystations.rsapi;

import org.railwaystations.rsapi.writer.PhotographersTxtWriter;
import org.railwaystations.rsapi.writer.StationsGpxWriter;
import org.railwaystations.rsapi.writer.StationsTxtWriter;
import org.railwaystations.rsapi.writer.StatisticTxtWriter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.List;

@EnableWebMvc
@Configuration
@ComponentScan({ "org.railwaystations.rsapi" })
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
        converters.add(new PhotographersTxtWriter());
        converters.add(new StationsGpxWriter());
        converters.add(new StationsTxtWriter());
        converters.add(new StatisticTxtWriter());
        converters.add(new MappingJackson2HttpMessageConverter());
    }

    @Override
    public void addCorsMappings(final CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedHeaders("X-Requested-With","Content-Type","Accept","Origin","Authorization","Comment","Country","Station-Id","NameOrEmail","New-Password")
                .allowedMethods("OPTIONS","GET","PUT","POST","DELETE","HEAD")
                .allowedOriginPatterns("*");
    }

}
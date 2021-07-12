package org.railwaystations.rsapi;

import org.railwaystations.rsapi.writer.PhotographersTxtWriter;
import org.railwaystations.rsapi.writer.StationsGpxWriter;
import org.railwaystations.rsapi.writer.StationsTxtWriter;
import org.railwaystations.rsapi.writer.StatisticTxtWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
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
        converters.add(byteArrayHttpMessageConverter());
    }

    @Override
    public void addCorsMappings(final CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedHeaders("X-Requested-With","Content-Type","Accept","Origin","Authorization","Comment","Country","Station-Id","NameOrEmail","New-Password")
                .allowedMethods("OPTIONS","GET","PUT","POST","DELETE","HEAD")
                .allowedOriginPatterns("*");
    }

    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        final ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        arrayHttpMessageConverter.setSupportedMediaTypes(getSupportedMediaTypes());
        return arrayHttpMessageConverter;
    }

    private List<MediaType> getSupportedMediaTypes() {
        final List<MediaType> list = new ArrayList<>();
        list.add(MediaType.IMAGE_JPEG);
        list.add(MediaType.IMAGE_PNG);
        list.add(MediaType.APPLICATION_OCTET_STREAM);
        return list;
    }

}
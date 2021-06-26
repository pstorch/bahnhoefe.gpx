package org.railwaystations.rsapi;

import org.railwaystations.rsapi.writer.PhotographersTxtWriter;
import org.railwaystations.rsapi.writer.StationsGpxWriter;
import org.railwaystations.rsapi.writer.StationsTxtWriter;
import org.railwaystations.rsapi.writer.StatisticTxtWriter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
    }

}
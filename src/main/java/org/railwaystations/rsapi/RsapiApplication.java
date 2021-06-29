package org.railwaystations.rsapi;

import org.railwaystations.rsapi.mail.Mailer;
import org.railwaystations.rsapi.mail.SmtpMailer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableTransactionManagement
@EnableWebSecurity
@EnableGlobalMethodSecurity(
		prePostEnabled = true,
		securedEnabled = true,
		jsr250Enabled = true)
public class RsapiApplication {

	public static void main(final String[] args) {
		SpringApplication.run(RsapiApplication.class, args);
	}

	@Bean
	@ConfigurationProperties(prefix = "mailer")
	public Mailer createMailer() {
		return new SmtpMailer();
	}

}

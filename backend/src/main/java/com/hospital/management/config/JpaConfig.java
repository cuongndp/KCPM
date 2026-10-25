package com.hospital.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.hospital.management.util.BusinessTime;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "businessDateTimeProvider")
public class JpaConfig {

	@Bean
	public DateTimeProvider businessDateTimeProvider() {
		return () -> Optional.of(BusinessTime.now());
	}
}
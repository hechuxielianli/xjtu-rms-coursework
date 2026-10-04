package com.example.rms.shared.infrastructure;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class FoundationConfiguration { @Bean Clock utcClock() { return Clock.systemUTC(); } }

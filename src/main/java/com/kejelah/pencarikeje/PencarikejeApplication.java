package com.kejelah.pencarikeje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PencarikejeApplication {

	public static void main(String[] args) {
		SpringApplication.run(PencarikejeApplication.class, args);
	}

}

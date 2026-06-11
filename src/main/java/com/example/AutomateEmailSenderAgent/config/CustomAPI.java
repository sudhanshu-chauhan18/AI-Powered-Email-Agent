package com.example.AutomateEmailSenderAgent.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomAPI {

    @Bean
    public OpenAPI CustomOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Powered Email Agent Rest API-")
                        .version("v1.O")
                        .description("Production Grade API")
                        .contact(new Contact()
                                .name("Sudhanshu Chauhan")
                                .email("sudhanshuchauhan6789@gmail.com")
//                                .url("https://sudhanshu.com")
                        )
                );
    }
}

//for Docs go on this ->  http://localhost:8080/swagger-ui/index.html

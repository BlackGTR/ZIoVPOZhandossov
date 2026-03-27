package com.example.antivirus.security;

import com.example.antivirus.license.LicenseType;
import com.example.antivirus.license.LicenseTypeRepository;
import com.example.antivirus.license.Product;
import com.example.antivirus.license.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemo(ProductRepository products, LicenseTypeRepository types) {
        return args -> {
            products.findByName("ANTIVIRUS_PRODUCT")
                    .orElseGet(() -> {
                        Product p = new Product();
                        p.setName("ANTIVIRUS_PRODUCT");
                        p.setBlocked(false);
                        return products.save(p);
                    });

            types.findByName("STD_30")
                    .orElseGet(() -> {
                        LicenseType t = new LicenseType();
                        t.setName("STD_30");
                        t.setDefaultDurationInDays(5);
                        t.setDescription("Стандартная лицензия на 5 дней");
                        return types.save(t);
                    });
        };
    }
}

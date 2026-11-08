package com.example.pharmacyservice.config;

import com.example.pharmacyservice.entity.Medicine;
import com.example.pharmacyservice.repository.MedicineRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MedicineDataInitializer {

    @Bean
    public CommandLineRunner initMedicineCatalog(MedicineRepository medicineRepository) {
        return args -> {
            if (!medicineRepository.existsById(1L)) {
                Medicine sample1 = new Medicine(
                        1L,
                        "Panadol Extra",
                        "Thuốc giảm đau hạ sốt",
                        50000.0,
                        100
                );
                medicineRepository.save(sample1);
                System.out.println(">>> [INIT CATALOG] Đã khởi tạo mẫu thuốc id=1 (Panadol Extra, Giá: 50.000)");
            }

            if (!medicineRepository.existsById(2L)) {
                Medicine sample2 = new Medicine(
                        2L,
                        "Aspirin 500mg",
                        "Thuốc chống viêm giảm đau",
                        25000.0,
                        200
                );
                medicineRepository.save(sample2);
                System.out.println(">>> [INIT CATALOG] Đã khởi tạo mẫu thuốc id=2 (Aspirin 500mg, Giá: 25.000)");
            }
        };
    }
}


package com.example.pharmacyservice;

import com.example.pharmacyservice.entity.Medicine;
import com.example.pharmacyservice.repository.MedicineRepository;
import com.example.pharmacyservice.service.MedicineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/pharmacy_db",
        "spring.datasource.username=postgres",
        "spring.datasource.password=17111998",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.cache.type=simple"
})
public class MedicineCacheTest {

    @Autowired
    private MedicineService medicineService;

    @MockitoBean
    private MedicineRepository medicineRepository;

    @Test
    public void testCacheHitOnSecondCall() {
        Medicine sample = new Medicine(1L, "Panadol Extra", "Thuốc giảm đau", 50000.0, 100);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(sample));

        // Lần 1: Gọi hàm lấy chi tiết thuốc -> Cache miss -> gọi database
        Medicine firstCall = medicineService.getMedicineById(1L);
        assertNotNull(firstCall);
        assertEquals("Panadol Extra", firstCall.getMedicineName());

        // Lần 2: Gọi lại cùng id -> Cache hit -> KHÔNG gọi database
        Medicine secondCall = medicineService.getMedicineById(1L);
        assertNotNull(secondCall);
        assertEquals("Panadol Extra", secondCall.getMedicineName());

        // Kiểm tra chắc chắn repository chỉ được gọi 1 lần duy nhất!
        verify(medicineRepository, times(1)).findById(1L);
        System.out.println("==================================================================");
        System.out.println(">>> [CACHE TEST THÀNH CÔNG]");
        System.out.println(">>> Lần 1: Gọi Database tìm kiếm thuốc id=1");
        System.out.println(">>> Lần 2: Lấy trực tiếp từ Cache (Database không bị gọi lại)");
        System.out.println("==================================================================");
    }
}

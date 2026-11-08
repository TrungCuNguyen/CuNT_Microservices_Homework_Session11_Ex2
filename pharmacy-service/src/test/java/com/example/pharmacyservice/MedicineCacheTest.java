package com.example.pharmacyservice;

import com.example.pharmacyservice.entity.Medicine;
import com.example.pharmacyservice.repository.MedicineRepository;
import com.example.pharmacyservice.service.MedicineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
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

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private MedicineRepository medicineRepository;

    @BeforeEach
    public void setup() {
        if (cacheManager != null && cacheManager.getCache("medicines") != null) {
            cacheManager.getCache("medicines").clear();
        }
        reset(medicineRepository);
    }

    @Test
    public void testCacheHitOnSecondCall() {
        Medicine sample = new Medicine(1L, "Panadol Extra", "Thuốc giảm đau", 50000.0, 100);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(sample));

        // Lần 1: Gọi hàm lấy chi tiết thuốc -> Cache miss -> gọi database
        Medicine firstCall = medicineService.getMedicineById(1L);
        assertNotNull(firstCall);
        assertEquals("Panadol Extra", firstCall.getMedicineName());
        verify(medicineRepository, times(1)).findById(1L);

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

    @Test
    public void testCacheEvictOnUpdateMedicine() {
        Medicine sample = new Medicine(1L, "Panadol Extra", "Thuốc giảm đau", 50000.0, 100);
        Medicine updatedSample = new Medicine(1L, "Panadol Extra", "Thuốc giảm đau", 65000.0, 100);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(sample));
        when(medicineRepository.save(any(Medicine.class))).thenReturn(updatedSample);

        // Bước 1: Gọi API lấy thuốc ID=1 -> DB được gọi lần 1
        Medicine call1 = medicineService.getMedicineById(1L);
        assertEquals(50000.0, call1.getPrice());
        verify(medicineRepository, times(1)).findById(1L);

        // Bước 2: Gọi lại lần 2 -> Lấy từ Cache, DB KHÔNG bị gọi thêm (vẫn là 1)
        Medicine call2 = medicineService.getMedicineById(1L);
        assertEquals(50000.0, call2.getPrice());
        verify(medicineRepository, times(1)).findById(1L);

        // Bước 3: Update giá thuốc ID=1 -> Kích hoạt @CacheEvict(value = "medicines", key = "#medicine.id")
        Medicine updatePayload = new Medicine();
        updatePayload.setId(1L);
        updatePayload.setPrice(65000.0);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(updatedSample));
        medicineService.updateMedicine(updatePayload);
        // findById được gọi 2 lần (1 lần ở bước 1, 1 lần trong updateMedicine)
        verify(medicineRepository, times(2)).findById(1L);

        // Bước 4: Gọi lại API lấy thuốc ID=1 -> Cache 'medicines::1' đã bị Evict, DB phải được gọi lại!
        Medicine call4 = medicineService.getMedicineById(1L);
        assertEquals(65000.0, call4.getPrice());
        verify(medicineRepository, times(3)).findById(1L); // Tổng 3 lần: lần 1 (bước 1) + lần 2 (update) + lần 3 (bước 4)

        System.out.println("==================================================================");
        System.out.println(">>> [CACHE EVICT TEST THÀNH CÔNG]");
        System.out.println(">>> Bước 1: Lấy thuốc ID=1 -> Truy vấn DB lần 1");
        System.out.println(">>> Bước 2: Lấy thuốc ID=1 -> Hit Cache (DB không bị gọi lại)");
        System.out.println(">>> Bước 3: Update giá thuốc -> Cache 'medicines::1' bị Evict khỏi Redis");
        System.out.println(">>> Bước 4: Lấy lại thuốc ID=1 -> Cache Miss, truy vấn lại DB lấy giá mới 65.000");
        System.out.println("==================================================================");
    }
}

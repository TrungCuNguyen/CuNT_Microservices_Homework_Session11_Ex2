package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.entity.Medicine;
import com.example.pharmacyservice.service.MedicineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/pharmacy/medicines", "/api/medicines"})
public class MedicineController {

    private static final Logger log = LoggerFactory.getLogger(MedicineController.class);

    @Autowired
    private MedicineService medicineService;

    /**
     * API lấy thông tin chi tiết thuốc:
     * GET http://localhost:8080/api/pharmacy/medicines/{id}
     * Lần 1: Có log SQL, thời gian phản hồi thường > 20ms
     * Lần 2: Không có log SQL, lấy từ Redis Cache, thời gian phản hồi < 10ms
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMedicine(@PathVariable Long id) {
        long start = System.currentTimeMillis();

        Medicine medicine = medicineService.getMedicineById(id);

        long responseTime = System.currentTimeMillis() - start;
        log.info(">>> [API RESPONSE] Hoàn tất truy vấn thuốc id={} trong {} ms", id, responseTime);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("medicine", medicine);
        response.put("responseTimeMs", responseTime);
        response.put("note", responseTime <= 10
                ? "Dữ liệu được lấy siêu tốc từ Redis Cache (< 10ms, không gọi Database)"
                : "Dữ liệu được truy vấn từ Database (lần đầu chưa có cache)");

        return ResponseEntity.ok(response);
    }

    /**
     * API cập nhật thông tin / giá thuốc:
     * PUT http://localhost:8080/api/pharmacy/medicines/{id}
     * Kích hoạt @CacheEvict(value = "medicines", key = "#medicine.id")
     * Xóa sạch cache cũ trong Redis để lần truy vấn sau lấy giá mới nhất từ Database.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMedicine(
            @PathVariable Long id,
            @RequestBody(required = false) Medicine updateData,
            @RequestParam(name = "price", required = false) Double queryPrice) {

        if (updateData == null) {
            updateData = new Medicine();
        }
        updateData.setId(id);
        if (queryPrice != null) {
            updateData.setPrice(queryPrice);
        }

        Medicine updated = medicineService.updateMedicine(updateData);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("action", "CACHE_EVICTED");
        response.put("message", "Cập nhật thuốc thành công! Key 'medicines::" + id + "' đã bị xóa khỏi Redis Cache.");
        response.put("medicine", updated);

        return ResponseEntity.ok(response);
    }

    /**
     * API chuyên biệt cập nhật giá thuốc nhanh qua Query Param:
     * PUT http://localhost:8080/api/pharmacy/medicines/{id}/price?price=65000
     */
    @PutMapping("/{id}/price")
    public ResponseEntity<Map<String, Object>> updatePrice(
            @PathVariable Long id,
            @RequestParam Double price) {

        Medicine med = new Medicine();
        med.setId(id);
        med.setPrice(price);

        Medicine updated = medicineService.updateMedicine(med);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("action", "CACHE_EVICTED");
        response.put("message", "Cập nhật giá thuốc thành công! Cache 'medicines::" + id + "' đã được xóa.");
        response.put("medicine", updated);

        return ResponseEntity.ok(response);
    }
}


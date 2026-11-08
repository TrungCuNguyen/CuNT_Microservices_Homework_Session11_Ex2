package com.example.pharmacyservice.service;

import com.example.pharmacyservice.entity.Medicine;
import com.example.pharmacyservice.repository.MedicineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MedicineService {

    private static final Logger log = LoggerFactory.getLogger(MedicineService.class);

    @Autowired
    private MedicineRepository medicineRepository;

    /**
     * Lấy thông tin chi tiết thuốc có áp dụng Cache Redis.
     * @Cacheable(value = "medicines", key = "#id"):
     * - Lần 1: Chưa có trong Redis cache -> hàm getMedicineById sẽ được thực thi và gọi SQL xuống DB.
     *   Kết quả trả về sẽ tự động được lưu vào Redis với key "medicines::#id".
     * - Lần 2+: Đã có trong Redis -> trả về kết quả ngay lập tức mà KHÔNG thực thi hàm này (không có SQL).
     */
    @Cacheable(value = "medicines", key = "#id")
    public Medicine getMedicineById(Long id) {
        log.info(">>> [DATABASE QUERY] Truy vấn trực tiếp từ PostgreSQL Database cho thuốc có ID = {}", id);
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));
    }
}


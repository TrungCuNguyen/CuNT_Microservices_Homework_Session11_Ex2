package com.example.pharmacyservice.service;

import com.example.pharmacyservice.entity.Medicine;
import com.example.pharmacyservice.repository.MedicineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
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

    /**
     * Cập nhật thông tin thuốc (ví dụ cập nhật giá thuốc).
     * @CacheEvict(value = "medicines", key = "#medicine.id"):
     * - Khi một loại thuốc được sửa giá, Cache cũ (key "medicines::#medicine.id") phải bị xóa ngay lập tức
     *   khỏi Redis để lần truy vấn sau hệ thống sẽ lấy giá mới nhất từ Database.
     */
    @CacheEvict(value = "medicines", key = "#medicine.id")
    public Medicine updateMedicine(Medicine medicine) {
        log.info(">>> [DATABASE UPDATE] Cập nhật thông tin thuốc ID = {} vào Database và xóa Cache cũ trong Redis", medicine.getId());

        Medicine existing = medicineRepository.findById(medicine.getId())
                .orElseGet(() -> medicine);

        if (medicine.getPrice() != null) {
            existing.setPrice(medicine.getPrice());
        }
        if (medicine.getMedicineName() != null) {
            existing.setMedicineName(medicine.getMedicineName());
        }
        if (medicine.getDescription() != null) {
            existing.setDescription(medicine.getDescription());
        }
        if (medicine.getQuantity() != null) {
            existing.setQuantity(medicine.getQuantity());
        }

        return medicineRepository.save(existing);
    }
}


package com.carrental.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.domain.vehicle.Vehicle;
import com.carrental.domain.vehicle.VehicleRepository;
import com.carrental.infrastructure.persistence.dataobject.VehicleDO;
import com.carrental.infrastructure.persistence.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class VehicleRepositoryImpl implements VehicleRepository {

    private final VehicleMapper vehicleMapper;

    @Override
    public Optional<Vehicle> findById(Long id) {
        VehicleDO vehicleDO = vehicleMapper.selectById(id);
        return Optional.ofNullable(vehicleDO).map(this::toDomain);
    }

    @Override
    public List<Vehicle> findActiveVehicles(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<VehicleDO> vehicleDOs = vehicleMapper.selectActiveVehicles(offset, pageSize);
        return vehicleDOs.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countActiveVehicles() {
        return vehicleMapper.countActiveVehicles();
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        VehicleDO vehicleDO = toDO(vehicle);
        if (vehicle.getId() == null) {
            vehicleMapper.insert(vehicleDO);
            vehicle.setId(vehicleDO.getId());
        } else {
            vehicleMapper.updateById(vehicleDO);
        }
        return vehicle;
    }

    @Override
    public void softDelete(Long id) {
        VehicleDO vehicleDO = vehicleMapper.selectById(id);
        if (vehicleDO != null) {
            vehicleDO.setDeletedAt(LocalDateTime.now());
            vehicleMapper.updateById(vehicleDO);
        }
    }

    private Vehicle toDomain(VehicleDO vehicleDO) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleDO.getId());
        vehicle.setName(vehicleDO.getName());
        vehicle.setBrand(vehicleDO.getBrand());
        vehicle.setSeats(vehicleDO.getSeats());
        vehicle.setTransmission(vehicleDO.getTransmission());
        vehicle.setDescription(vehicleDO.getDescription());
        vehicle.setImages(vehicleDO.getImages());
        vehicle.setTags(vehicleDO.getTags());
        vehicle.setWeekdayPrice(vehicleDO.getWeekdayPrice());
        vehicle.setWeekendPrice(vehicleDO.getWeekendPrice());
        vehicle.setHolidayPrice(vehicleDO.getHolidayPrice());
        vehicle.setStatus(vehicleDO.getStatus());
        vehicle.setDeletedAt(vehicleDO.getDeletedAt());
        vehicle.setCreatedAt(vehicleDO.getCreatedAt());
        vehicle.setUpdatedAt(vehicleDO.getUpdatedAt());
        return vehicle;
    }

    private VehicleDO toDO(Vehicle vehicle) {
        VehicleDO vehicleDO = new VehicleDO();
        vehicleDO.setId(vehicle.getId());
        vehicleDO.setName(vehicle.getName());
        vehicleDO.setBrand(vehicle.getBrand());
        vehicleDO.setSeats(vehicle.getSeats());
        vehicleDO.setTransmission(vehicle.getTransmission());
        vehicleDO.setDescription(vehicle.getDescription());
        vehicleDO.setImages(vehicle.getImages());
        vehicleDO.setTags(vehicle.getTags());
        vehicleDO.setWeekdayPrice(vehicle.getWeekdayPrice());
        vehicleDO.setWeekendPrice(vehicle.getWeekendPrice());
        vehicleDO.setHolidayPrice(vehicle.getHolidayPrice());
        vehicleDO.setStatus(vehicle.getStatus());
        vehicleDO.setDeletedAt(vehicle.getDeletedAt());
        return vehicleDO;
    }
}

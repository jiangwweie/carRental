package com.carrental.domain.vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {

    Optional<Vehicle> findById(Long id);

    List<Vehicle> findActiveVehicles(int page, int pageSize);

    long countActiveVehicles();

    Vehicle save(Vehicle vehicle);

    List<Vehicle> findAllVehicles(int page, int pageSize);

    long countAllVehicles();

    void softDelete(Long id);
}

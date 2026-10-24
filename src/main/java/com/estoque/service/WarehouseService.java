package com.estoque.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.LocationUsage;
import com.estoque.domain.Warehouse;
import com.estoque.repository.WarehouseRepository;

@Service
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouses;
    private final LocationService locationService;

    public WarehouseService(WarehouseRepository warehouses, LocationService locationService) {
        this.warehouses = warehouses;
        this.locationService = locationService;
    }

    /** Cria o armazém com seu local de visão (CODE) e o local de estoque (CODE/Estoque). */
    public Warehouse create(String name, String code) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Informe o nome do armazém.");
        }
        if (code == null || !code.trim().matches("[A-Za-z0-9]{1,10}")) {
            throw new BusinessException("O código deve ter de 1 a 10 letras ou números.");
        }
        String normalized = code.trim().toUpperCase();
        if (warehouses.existsByCodeIgnoreCase(normalized)) {
            throw new BusinessException("Já existe um armazém com o código " + normalized + ".");
        }
        var view = locationService.create(normalized, null, LocationUsage.VIEW);
        var stock = locationService.create("Estoque", view.getId(), LocationUsage.INTERNAL);

        var warehouse = new Warehouse();
        warehouse.setName(name.trim());
        warehouse.setCode(normalized);
        warehouse.setViewLocation(view);
        warehouse.setStockLocation(stock);
        return warehouses.save(warehouse);
    }

    public void rename(Long id, String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Informe o nome do armazém.");
        }
        warehouses.findById(id).orElseThrow(() -> BusinessException.notFound("Armazém")).setName(name.trim());
    }
}

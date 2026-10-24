package com.estoque.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.Location;
import com.estoque.domain.LocationUsage;
import com.estoque.domain.Warehouse;
import com.estoque.repository.LocationRepository;
import com.estoque.repository.WarehouseRepository;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locations;
    private final WarehouseRepository warehouses;

    public LocationService(LocationRepository locations, WarehouseRepository warehouses) {
        this.locations = locations;
        this.warehouses = warehouses;
    }

    @Transactional(readOnly = true)
    public Location get(Long id) {
        return locations.findById(id).orElseThrow(() -> BusinessException.notFound("Local"));
    }

    @Transactional(readOnly = true)
    public List<Location> internal() {
        return locations.findByUsageAndActiveTrueOrderByFullName(LocationUsage.INTERNAL);
    }

    @Transactional(readOnly = true)
    public List<Location> active() {
        return locations.findByActiveTrueOrderByFullName();
    }

    /** Local de estoque do primeiro armazém, usado como padrão nas operações. */
    @Transactional(readOnly = true)
    public Location defaultStock() {
        return warehouses.findAllByOrderById().stream().findFirst()
                .map(Warehouse::getStockLocation)
                .orElseThrow(() -> new BusinessException("Cadastre ao menos um armazém."));
    }

    @Transactional(readOnly = true)
    public Location virtual(LocationUsage usage) {
        return locations.findFirstByUsageOrderById(usage)
                .orElseThrow(() -> new BusinessException("Local do tipo \"" + usage.getLabel() + "\" não configurado."));
    }

    public Location create(String name, Long parentId, LocationUsage usage) {
        var location = new Location();
        location.setName(validName(name));
        location.setParent(parentId == null ? null : get(parentId));
        location.setUsage(usage);
        location.setFullName(path(location.getParent(), location.getName()));
        ensureUnique(location.getFullName());
        return locations.save(location);
    }

    /** Atualiza o local e recalcula o caminho completo de todos os descendentes. */
    public Location update(Long id, String name, Long parentId, LocationUsage usage, boolean active) {
        var location = get(id);
        var parent = parentId == null ? null : get(parentId);
        if (parent != null && parent.isChildOf(location)) {
            throw new BusinessException("Um local não pode ficar dentro de si mesmo ou de um sublocal.");
        }
        String oldPath = location.getFullName();
        String newPath = path(parent, validName(name));
        if (!newPath.equals(oldPath)) {
            ensureUnique(newPath);
            for (Location child : locations.findSubtree(oldPath)) {
                child.setFullName(newPath + child.getFullName().substring(oldPath.length()));
            }
        }
        location.setName(validName(name));
        location.setParent(parent);
        location.setUsage(usage);
        location.setActive(active);
        return location;
    }

    private void ensureUnique(String fullName) {
        if (locations.existsByFullName(fullName)) {
            throw new BusinessException("Já existe um local chamado \"" + fullName + "\".");
        }
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Informe o nome do local.");
        }
        if (name.contains("/")) {
            throw new BusinessException("O nome do local não pode conter \"/\".");
        }
        return name.trim();
    }

    private static String path(Location parent, String name) {
        return parent == null ? name : parent.getFullName() + "/" + name;
    }
}

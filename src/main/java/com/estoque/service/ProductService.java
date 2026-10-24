package com.estoque.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.Product;
import com.estoque.repository.CategoryRepository;
import com.estoque.repository.PartnerRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.service.form.ProductForm;

@Service
@Transactional
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final PartnerRepository partners;

    public ProductService(ProductRepository products, CategoryRepository categories, PartnerRepository partners) {
        this.products = products;
        this.categories = categories;
        this.partners = partners;
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id).orElseThrow(() -> BusinessException.notFound("Produto"));
    }

    public Product save(ProductForm form) {
        String sku = form.getSku() == null || form.getSku().isBlank() ? null : form.getSku().trim();
        if (sku != null && (form.getId() == null
                ? products.existsBySkuIgnoreCase(sku)
                : products.existsBySkuIgnoreCaseAndIdNot(sku, form.getId()))) {
            throw new BusinessException("Já existe um produto com o código " + sku + ".");
        }
        var product = form.getId() == null ? new Product() : get(form.getId());
        product.setSku(sku);
        product.setName(form.getName().trim());
        product.setCategory(form.getCategoryId() == null ? null
                : categories.findById(form.getCategoryId()).orElseThrow(() -> BusinessException.notFound("Categoria")));
        product.setUom(form.getUom().trim());
        product.setCost(form.getCost());
        product.setSalePrice(form.getSalePrice());
        product.setRoute(form.getRoute());
        product.setSupplier(form.getSupplierId() == null ? null
                : partners.findById(form.getSupplierId()).orElseThrow(() -> BusinessException.notFound("Fornecedor")));
        product.setLeadTimeDays(form.getLeadTimeDays());
        product.setBarcode(form.getBarcode() == null || form.getBarcode().isBlank() ? null : form.getBarcode().trim());
        product.setDescription(form.getDescription());
        product.setActive(form.isActive());
        return products.save(product);
    }

    /** Produtos não são excluídos (há histórico de movimentos); são arquivados. */
    public void setActive(Long id, boolean active) {
        get(id).setActive(active);
    }
}

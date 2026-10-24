package com.estoque.web;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.estoque.domain.Category;
import com.estoque.domain.LocationUsage;
import com.estoque.domain.Role;
import com.estoque.repository.CategoryRepository;
import com.estoque.repository.LocationRepository;
import com.estoque.repository.WarehouseRepository;
import com.estoque.service.BusinessException;
import com.estoque.service.LocationService;
import com.estoque.service.UserService;
import com.estoque.service.WarehouseService;
import com.estoque.service.form.UserForm;

/** Configuração (somente administradores): armazéns, locais, categorias e usuários. */
@Controller
@RequestMapping("/config")
public class ConfigController {

    private final WarehouseRepository warehouses;
    private final WarehouseService warehouseService;
    private final LocationRepository locations;
    private final LocationService locationService;
    private final CategoryRepository categories;
    private final UserService userService;

    public ConfigController(WarehouseRepository warehouses, WarehouseService warehouseService, LocationRepository locations,
                            LocationService locationService, CategoryRepository categories, UserService userService) {
        this.warehouses = warehouses;
        this.warehouseService = warehouseService;
        this.locations = locations;
        this.locationService = locationService;
        this.categories = categories;
        this.userService = userService;
    }

    // ---- Armazéns ----

    @GetMapping("/warehouses")
    public String warehouses(Model model) {
        model.addAttribute("warehouses", warehouses.findAllByOrderById());
        return "config/warehouses";
    }

    @PostMapping("/warehouses")
    public String createWarehouse(@RequestParam String name, @RequestParam String code, RedirectAttributes redirect) {
        warehouseService.create(name, code);
        redirect.addFlashAttribute("success", "Armazém criado com seus locais.");
        return "redirect:/config/warehouses";
    }

    @PostMapping("/warehouses/{id}")
    public String renameWarehouse(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirect) {
        warehouseService.rename(id, name);
        redirect.addFlashAttribute("success", "Armazém atualizado.");
        return "redirect:/config/warehouses";
    }

    // ---- Locais ----

    @GetMapping("/locations")
    public String locations(Model model) {
        model.addAttribute("locations", locations.findAllByOrderByFullName());
        model.addAttribute("usages", LocationUsage.values());
        return "config/locations";
    }

    @GetMapping("/locations/{id}")
    public String editLocation(@PathVariable Long id, Model model) {
        model.addAttribute("location", locationService.get(id));
        model.addAttribute("locations", locations.findAllByOrderByFullName());
        model.addAttribute("usages", LocationUsage.values());
        return "config/location-form";
    }

    @PostMapping("/locations")
    public String createLocation(@RequestParam String name, @RequestParam(required = false) Long parentId,
                                 @RequestParam LocationUsage usage, RedirectAttributes redirect) {
        locationService.create(name, parentId, usage);
        redirect.addFlashAttribute("success", "Local criado.");
        return "redirect:/config/locations";
    }

    @PostMapping("/locations/{id}")
    public String updateLocation(@PathVariable Long id, @RequestParam String name,
                                 @RequestParam(required = false) Long parentId, @RequestParam LocationUsage usage,
                                 @RequestParam(defaultValue = "false") boolean active, RedirectAttributes redirect) {
        locationService.update(id, name, parentId, usage, active);
        redirect.addFlashAttribute("success", "Local atualizado.");
        return "redirect:/config/locations";
    }

    // ---- Categorias ----

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categories.findAllByOrderByName());
        return "config/categories";
    }

    @PostMapping("/categories")
    public String saveCategory(@RequestParam(required = false) Long id, @RequestParam String name,
                               RedirectAttributes redirect) {
        if (name.isBlank()) {
            throw new BusinessException("Informe o nome da categoria.");
        }
        var existing = categories.findByNameIgnoreCase(name.trim());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new BusinessException("Já existe a categoria " + name.trim() + ".");
        }
        var category = id == null ? new Category() : categories.findById(id).orElseThrow(() -> BusinessException.notFound("Categoria"));
        category.setName(name.trim());
        categories.save(category);
        redirect.addFlashAttribute("success", "Categoria salva.");
        return "redirect:/config/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirect) {
        categories.deleteById(id);
        categories.flush();
        redirect.addFlashAttribute("success", "Categoria excluída.");
        return "redirect:/config/categories";
    }

    // ---- Usuários ----

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.all());
        return "config/users";
    }

    @GetMapping("/users/new")
    public String newUser(Model model) {
        return userForm(new UserForm(), model);
    }

    @GetMapping("/users/{id}")
    public String editUser(@PathVariable Long id, Model model) {
        return userForm(UserForm.from(userService.get(id)), model);
    }

    @PostMapping("/users/save")
    public String saveUser(@Validated @ModelAttribute("form") UserForm form, BindingResult result, Principal principal,
                           Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                userService.save(form, principal.getName());
                redirect.addFlashAttribute("success", "Usuário salvo.");
                return "redirect:/config/users";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return userForm(form, model);
    }

    private String userForm(UserForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("roles", Role.values());
        return "config/user-form";
    }
}

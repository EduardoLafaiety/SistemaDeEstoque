package com.estoque.service.form;

import com.estoque.domain.AppUser;
import com.estoque.domain.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserForm {

    private Long id;

    @NotBlank(message = "Informe o login")
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Use apenas letras, números, ponto, hífen ou sublinhado")
    private String username;

    @NotBlank(message = "Informe o nome")
    @Size(max = 120)
    private String fullName;

    @NotNull
    private Role role = Role.USER;

    private boolean active = true;

    /** Obrigatória na criação; na edição, em branco mantém a senha atual. */
    @Size(max = 72)
    private String password;

    public static UserForm from(AppUser u) {
        var f = new UserForm();
        f.id = u.getId();
        f.username = u.getUsername();
        f.fullName = u.getFullName();
        f.role = u.getRole();
        f.active = u.isActive();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

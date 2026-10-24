package com.estoque.service;

import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.AppUser;
import com.estoque.domain.Role;
import com.estoque.repository.AppUserRepository;
import com.estoque.service.form.UserForm;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final int MIN_PASSWORD = 6;

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        var user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AppUser> all() {
        return users.findAllByOrderByUsername();
    }

    @Transactional(readOnly = true)
    public AppUser get(Long id) {
        return users.findById(id).orElseThrow(() -> BusinessException.notFound("Usuário"));
    }

    @Transactional(readOnly = true)
    public String fullName(String username) {
        return users.findByUsernameIgnoreCase(username).map(AppUser::getFullName).orElse(username);
    }

    public AppUser save(UserForm form, String currentUsername) {
        String username = form.getUsername().trim();
        boolean creating = form.getId() == null;
        var user = creating ? new AppUser() : get(form.getId());

        if (!username.equalsIgnoreCase(user.getUsername() == null ? "" : user.getUsername())
                && users.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("O login " + username + " já está em uso.");
        }
        boolean self = !creating && user.getUsername().equalsIgnoreCase(currentUsername);
        if (self && (!form.isActive() || form.getRole() != Role.ADMIN) && user.getRole() == Role.ADMIN) {
            throw new BusinessException("Você não pode desativar nem remover o perfil de administrador da sua própria conta.");
        }
        String password = form.getPassword() == null ? "" : form.getPassword();
        if (creating && password.isBlank()) {
            throw new BusinessException("Informe a senha do novo usuário.");
        }
        if (!password.isBlank()) {
            if (password.length() < MIN_PASSWORD) {
                throw new BusinessException("A senha deve ter ao menos " + MIN_PASSWORD + " caracteres.");
            }
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setUsername(username);
        user.setFullName(form.getFullName().trim());
        user.setRole(form.getRole());
        user.setActive(form.isActive());
        return users.save(user);
    }

    /** Cria o administrador inicial se ainda não houver usuários. */
    public boolean createInitialAdmin(String username, String password) {
        if (users.count() > 0) {
            return false;
        }
        var admin = new AppUser();
        admin.setUsername(username);
        admin.setFullName("Administrador");
        admin.setRole(Role.ADMIN);
        admin.setPassword(passwordEncoder.encode(password));
        users.save(admin);
        return true;
    }
}

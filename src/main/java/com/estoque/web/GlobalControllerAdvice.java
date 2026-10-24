package com.estoque.web;

import java.security.Principal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.estoque.service.BusinessException;
import com.estoque.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Atributos comuns a todas as telas e tratamento de erros de negócio em ações
 * (o usuário volta à tela anterior com a mensagem de erro).
 */
@ControllerAdvice(basePackages = "com.estoque.web")
public class GlobalControllerAdvice {

    private final UserService userService;

    public GlobalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(Principal principal) {
        return principal == null ? null : userService.fullName(principal.getName());
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(BusinessException.class)
    public String businessError(BusinessException e, HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", e.getMessage());
        return "redirect:" + back(request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integrityError(HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "Não foi possível concluir: o registro está em uso ou viola uma restrição.");
        return "redirect:" + back(request);
    }

    /** Volta para a página de origem, aceitando apenas caminhos locais. */
    static String back(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            String host = request.getScheme() + "://" + request.getServerName();
            if (referer.startsWith(host)) {
                int pathStart = referer.indexOf('/', host.length());
                if (pathStart >= 0 && !referer.startsWith("//", pathStart) && !referer.startsWith("/\\", pathStart)) {
                    return referer.substring(pathStart);
                }
            }
        }
        return "/";
    }
}

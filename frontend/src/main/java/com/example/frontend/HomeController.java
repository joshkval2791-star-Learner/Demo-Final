package com.example.frontend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private ProtectedRestService protectedRestService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/secured")
    public String secured(Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            model.addAttribute("username", "Unknown");
            model.addAttribute("email", "No email");
            model.addAttribute("accessToken", "Not available");
        } else {
            // Get username from claims
            String username = principal.getClaim("preferred_username");
            if (username == null) {
                username = principal.getClaim("sub");
            }
            
            // Get email from claims
            String email = principal.getClaim("email");
            if (email == null) {
                email = "No email provided";
            }
            
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("accessToken", principal.getIdToken().getTokenValue());
        }
        return "secured";
    }

    @GetMapping("/call-service")
    public String callService(Model model) {
        try {
            String data = protectedRestService.callCalendarService().block();
            model.addAttribute("serviceData", data);
            return "service-result";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("errorDetails", e.getClass().getName());
            return "service-result";
        }
    }
}
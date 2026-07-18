package com.mysqlmcp.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    @GetMapping("/")
    public String root(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated (has a valid JWT token)
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            log.debug("Authenticated user accessing root - redirecting to dashboard");
            return "redirect:/dashboard";
        }
        
        log.debug("Unauthenticated user accessing root - redirecting to login");
        return "redirect:/login";
    }

    /**
     * MCP clients or external tools may send a POST to the root URL as a health
     * check or initialization request. Return a 200 with a simple status instead
     * of crashing with a 405 Method Not Allowed.
     */
    @PostMapping("/")
    @ResponseBody
    public ResponseEntity<String> rootPost() {
        return ResponseEntity.ok("OK");
    }
}

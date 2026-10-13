
package com.complaint.complaint_management.security;

import com.complaint.complaint_management.service.AdminTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminApiFilter extends OncePerRequestFilter {

    private final AdminTokenService adminTokenService;

    public AdminApiFilter(AdminTokenService adminTokenService) {
        this.adminTokenService = adminTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        boolean viewAllComplaints =
                "GET".equals(method) && "/api/complaints".equals(path);

        boolean updateComplaintStatus =
                "PUT".equals(method)
                && path.matches("/api/complaints/\\d+/status");

        return !viewAllComplaints && !updateComplaintStatus;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            sendUnauthorized(response);
            return;
        }

        String token = authorization.substring(7);

        if (!adminTokenService.isValid(token)) {
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"message\":\"Admin login required or session expired.\"}"
        );
    }
}

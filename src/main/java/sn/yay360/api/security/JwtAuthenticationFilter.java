package sn.yay360.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final java.util.Set<String> CHEMINS_AUTORISES_MDP_TEMPORAIRE =
            java.util.Set.of("/auth/change-password", "/auth/logout", "/utilisateurs/profil");

    private final JwtUtil jwtUtil;
    private final UtilisateurDetailsService utilisateurDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String email = jwtUtil.extraireEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UtilisateurPrincipal userDetails =
                        (UtilisateurPrincipal) utilisateurDetailsService.loadUserByUsername(email);

                // Jeton refusé si le compte a été désactivé ou si le mot de passe a changé depuis son émission
                if (jwtUtil.estValide(token, userDetails.getUsername())
                        && userDetails.isEnabled()
                        && versionValide(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Mot de passe temporaire (compte créé par un admin) : seul son changement est permis
                    if (Boolean.TRUE.equals(userDetails.getUtilisateur().getMotDePasseTemporaire())
                            && !CHEMINS_AUTORISES_MDP_TEMPORAIRE.contains(request.getRequestURI())) {
                        SecurityContextHolder.clearContext();
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"status\":403,\"erreur\":\"Mot de passe temporaire\","
                                + "\"message\":\"Changez votre mot de passe temporaire avant d'utiliser la plateforme.\","
                                + "\"chemin\":\"" + request.getRequestURI().replace("\"", "") + "\"}");
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            // Token invalide ou expiré : la requête continue non authentifiée,
            // Spring Security renverra 401 si la ressource est protégée.
            log.debug("[YAY360] Jeton JWT refusé : {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean versionValide(String token, UtilisateurPrincipal principal) {
        Integer version = principal.getUtilisateur().getVersionJeton();
        return jwtUtil.extraireVersion(token) == (version == null ? 0 : version);
    }
}

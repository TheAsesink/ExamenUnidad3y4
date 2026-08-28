package ec.edu.uteq.appweb.biblioteca.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String cabecera = peticion.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            String token = cabecera.substring(7);

            if (jwtService.esValido(token)) {
                String username = jwtService.extraerUsername(token);
                String rol = jwtService.extraerRol(token);

                var autoridades = java.util.List.of(new SimpleGrantedAuthority("ROLE_" + rol));
                var autenticacion = new UsernamePasswordAuthenticationToken(username, null, autoridades);
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        }

        cadena.doFilter(peticion, respuesta);
    }
}

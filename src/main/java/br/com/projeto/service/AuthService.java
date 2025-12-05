package br.com.projeto.service;

import java.util.Optional;

import br.com.projeto.dao.UserDAO;
import br.com.projeto.models.User;
import br.com.projeto.utils.JwtUtil;
import br.com.projeto.utils.PasswordUtil;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthService {

    @Inject
    UserDAO userDAO;

    @Inject
    JwtUtil jwtUtil;

    public boolean hasAdmin(){
        return userDAO.find("role", "admin").firstResultOptional().isPresent();
    }

    @Transactional
    public AuthResult createAdmin(String username, String password){
        if(hasAdmin()) return null; // Ou lançar exceção
        return createUserWithRole(username, password, "admin");
    }

    @Transactional
    public AuthResult register(String username, String password){
        return createUserWithRole(username, password, "funcionario");
    }

    @Transactional
    public AuthResult login(String username, String password){
        Optional<User> uo = userDAO.findByUsername(username);
        if(uo.isEmpty()) return null;
        User u = uo.get();
        String hash = PasswordUtil.sha256(password);
        if(!u.passwordHash.equals(hash)) return null;
        String token = jwtUtil.createToken(u.id, u.username, u.role, 3600 * 12);
        return new AuthResult(token, toPublicUser(u));
    }

    @Transactional
    public boolean promoteToManager(String userId){
        try{
            java.util.UUID id = java.util.UUID.fromString(userId);
            Optional<User> uo = userDAO.findByIdOptional(id);
            if(uo.isEmpty()) return false;
            User u = uo.get();
            u.role = "gestor";
            userDAO.getEntityManager().merge(u);
            return true;
        } catch(IllegalArgumentException e){
            return false;
        }
    }

    public java.util.List<PublicUser> getAllUsers(){
        return userDAO.listAll().stream()
            .map(this::toPublicUser)
            .collect(java.util.stream.Collectors.toList());
    }

    private AuthResult createUserWithRole(String username, String password, String role){
        if(userDAO.findByUsername(username).isPresent()) return null;
        String hash = PasswordUtil.sha256(password);
        User u = User.of(username, hash, role);
        userDAO.persist(u);
        String token = jwtUtil.createToken(u.id, u.username, u.role, 3600 * 12);
        return new AuthResult(token, toPublicUser(u));
    }

    private PublicUser toPublicUser(User u){
        PublicUser pu = new PublicUser();
        pu.id = u.id.toString();
        pu.username = u.username;
        pu.role = u.role;
        return pu;
    }

    @RegisterForReflection
    public static class AuthResult{
        public String token;
        public PublicUser user;
        public AuthResult(String token, PublicUser user){ this.token = token; this.user = user; }
    }

    @RegisterForReflection
    public static class PublicUser{
        public String id;
        public String username;
        public String role;
    }
}

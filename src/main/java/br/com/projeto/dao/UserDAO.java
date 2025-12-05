package br.com.projeto.dao;

import java.util.Optional;
import java.util.UUID;

import br.com.projeto.models.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserDAO implements PanacheRepository<User> {
    public Optional<User> findByUsername(String username){
        return find("username", username).firstResultOptional();
    }
    public Optional<User> findByIdOptional(UUID id){
        return find("id", id).firstResultOptional();
    }
}

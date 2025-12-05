package br.com.projeto.models;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public String role; // admin | gestor | funcionario

    public static User of(String username, String passwordHash, String role){
        User u = new User();
        u.id = UUID.randomUUID();
        u.username = username;
        u.passwordHash = passwordHash;
        u.role = role;
        return u;
    }
}

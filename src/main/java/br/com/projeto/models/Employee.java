package br.com.projeto.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "employees")
public class Employee extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId; // referencia users.id

    @Column(columnDefinition = "TEXT")
    public String data; // JSON como string para simplicidade

    @Column(name = "manager_id")
    public UUID managerId; // opcional: userId do gestor

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<EmployeeDocument> documents = new ArrayList<>();

    public static Employee create(UUID userId, String data){
        Employee e = new Employee();
        e.id = UUID.randomUUID();
        e.userId = userId;
        e.data = data;
        return e;
    }
}

package br.com.projeto.dao;

import java.util.Optional;
import java.util.UUID;

import br.com.projeto.models.Employee;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmployeeDAO implements PanacheRepository<Employee> {
    public Optional<Employee> findByIdOptional(UUID id){
        return find("id", id).firstResultOptional();
    }
    public Optional<Employee> findByUserId(UUID userId){
        return find("userId", userId).firstResultOptional();
    }
}

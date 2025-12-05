package br.com.projeto.dao;

import java.util.List;
import java.util.UUID;

import br.com.projeto.models.MyEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyEntityDAO {
    
    public MyEntity create(MyEntity entity) {
        entity.persist();
        return entity;
    }

    public MyEntity findById(UUID uuid) {
        return MyEntity.findById(uuid);
    }

    public List<MyEntity> getAll() {
        return MyEntity.listAll();
    }
}

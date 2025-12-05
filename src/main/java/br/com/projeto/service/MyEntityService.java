package br.com.projeto.service;

import java.util.List;
import java.util.UUID;

import br.com.projeto.dao.MyEntityDAO;
import br.com.projeto.models.MyEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyEntityService {
    
    @Inject
    MyEntityDAO myEntityDAO;

    public MyEntityService(MyEntityDAO myEntityDAO) {
        this.myEntityDAO = myEntityDAO;
    }

    public MyEntity createEntity(MyEntity entity) {
        return myEntityDAO.create(entity);
    }

    public MyEntity getEntity(UUID uuid) {
        return myEntityDAO.findById(uuid);
    }

    public List<MyEntity> getAll() {
        return myEntityDAO.getAll();
    }
}

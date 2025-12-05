package br.com.projeto.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.projeto.dao.EmployeeDAO;
import br.com.projeto.models.Employee;
import br.com.projeto.models.EmployeeDocument;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EmployeeService {

    @Inject
    EmployeeDAO employeeDAO;
    @Inject
    br.com.projeto.dao.UserDAO userDAO;

    @Transactional
    public Employee createEmployeeForUser(String userId, String data){
        if(userId == null || userId.trim().isEmpty()){
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        UUID uid;
        try {
            uid = UUID.fromString(userId.trim());
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format for userId: " + userId, e);
        }
        Employee e = Employee.create(uid, data);
        employeeDAO.persist(e);
        return e;
    }

    @Transactional
    public Employee createEmployeeForUserWithManager(String userId, String data, String managerId){
        if(userId == null || userId.trim().isEmpty()){
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        UUID uid;
        UUID mid = null;
        try {
            uid = UUID.fromString(userId.trim());
            if(managerId != null && !managerId.trim().isEmpty()){
                mid = UUID.fromString(managerId.trim());
            }
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format", e);
        }
        Employee e = Employee.create(uid, data);
        e.managerId = mid;
        employeeDAO.persist(e);
        return e;
    }

    @Transactional
    public EmployeeDocument addDocument(String employeeId, String documentName, String fileName, String contentType, byte[] fileData){
        Optional<Employee> eo = getById(employeeId);
        if(eo.isEmpty()) throw new IllegalArgumentException("Employee not found");
        
        Employee e = eo.get();
        EmployeeDocument doc = EmployeeDocument.of(e, documentName, fileName, contentType, fileData);
        e.documents.add(doc);
        employeeDAO.getEntityManager().merge(e);
        return doc;
    }

    @Transactional
    public boolean updateDocument(String employeeId, String documentName, String fileName, String contentType, byte[] fileData){
        Optional<Employee> eo = getById(employeeId);
        if(eo.isEmpty()) return false;
        
        Employee e = eo.get();
        EmployeeDocument doc = e.documents.stream()
            .filter(d -> documentName.equals(d.name))
            .findFirst()
            .orElse(null);
        
        if(doc == null) return false;
        
        doc.fileName = fileName;
        doc.contentType = contentType;
        doc.fileData = fileData;
        doc.fileSize = (long) fileData.length;
        doc.approved = null; // Resetar aprovação quando documento é atualizado
        
        employeeDAO.getEntityManager().merge(e);
        return true;
    }

    public Optional<EmployeeDocument> getDocument(String employeeId, String documentName){
        Optional<Employee> eo = getById(employeeId);
        if(eo.isEmpty()) return Optional.empty();
        
        return eo.get().documents.stream()
            .filter(d -> documentName.equals(d.name))
            .findFirst();
    }

    public List<Employee> getAllEmployees(){
        return employeeDAO.listAll();
    }

    public List<Employee> getEmployeesByManager(String managerId){
        if(managerId == null || managerId.trim().isEmpty()){
            return List.of();
        }
        UUID mid;
        try {
            mid = UUID.fromString(managerId.trim());
        } catch(IllegalArgumentException e) {
            return List.of();
        }
        return employeeDAO.find("managerId", mid).list();
    }

    public Optional<Employee> getById(String employeeId){
        try{
            return employeeDAO.findByIdOptional(UUID.fromString(employeeId));
        } catch(IllegalArgumentException e){
            return Optional.empty();
        }
    }

    public Optional<Employee> getByUserId(String userId){
        if(userId == null || userId.trim().isEmpty()){
            return Optional.empty();
        }
        UUID uid;
        try {
            uid = UUID.fromString(userId.trim());
        } catch(IllegalArgumentException e) {
            return Optional.empty();
        }
        return employeeDAO.find("userId", uid).firstResultOptional();
    }

    @Transactional
    public boolean approveDocument(String employeeId, String name, boolean approved){
        Optional<Employee> eo = getById(employeeId);
        if(eo.isEmpty()) return false;
        Employee e = eo.get();
        EmployeeDocument doc = e.documents.stream().filter(d->name.equals(d.name)).findFirst().orElse(null);
        if(doc == null) return false;
        doc.approved = approved;
        employeeDAO.getEntityManager().merge(e);
        return true;
    }

    @Transactional
    public boolean assignManager(String employeeId, String managerId){
        Optional<Employee> eo = getById(employeeId);
        if(eo.isEmpty()) return false;
        Employee e = eo.get();

        // Remover vínculo de gerente
        if(managerId == null || managerId.trim().isEmpty()){
            e.managerId = null;
            employeeDAO.getEntityManager().merge(e);
            return true;
        }

        UUID mid;
        try {
            mid = UUID.fromString(managerId.trim());
        } catch(IllegalArgumentException ex){
            return false;
        }

        // Prevenir atribuição do funcionário como gerente de si mesmo
        if (mid.equals(e.userId)) {
            return false;
        }

        // Verificar se usuário alvo existe e tem role gestor ou admin
        var userOpt = userDAO.findByIdOptional(mid);
        if(userOpt.isEmpty()) return false;
        var targetUser = userOpt.get();
        if(!"gestor".equals(targetUser.role) && !"admin".equals(targetUser.role)) return false;

        e.managerId = mid;
        employeeDAO.getEntityManager().merge(e);
        return true;
    }
}

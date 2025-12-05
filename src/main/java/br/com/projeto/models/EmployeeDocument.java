package br.com.projeto.models;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "employee_documents")
public class EmployeeDocument extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    public Employee employee;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String fileName; // nome original do arquivo

    @Column(nullable = false)
    public String contentType; // MIME type (application/pdf, image/jpeg, etc)

    @Column(nullable = false)
    public byte[] fileData; // conteúdo do arquivo

    @Column(nullable = false)
    public Long fileSize; // tamanho em bytes

    public Boolean approved; // null = pending

    public static EmployeeDocument of(Employee employee, String name, String fileName, String contentType, byte[] fileData){
        EmployeeDocument d = new EmployeeDocument();
        d.id = UUID.randomUUID();
        d.employee = employee;
        d.name = name;
        d.fileName = fileName;
        d.contentType = contentType;
        d.fileData = fileData;
        d.fileSize = (long) fileData.length;
        d.approved = null;
        return d;
    }
}

package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base abstract class demonstrating Object-Oriented Inheritance and Abstraction.
 */
public abstract class BaseEntity {
    protected int id;
    protected String createdAt;

    public BaseEntity() {
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public BaseEntity(int id) {
        this();
        this.id = id;
    }

    public BaseEntity(int id, String createdAt) {
        this.id = id;
        this.createdAt = (createdAt != null && !createdAt.isEmpty()) 
            ? createdAt 
            : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

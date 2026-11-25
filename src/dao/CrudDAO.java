package dao;

import java.sql.SQLException;
import java.util.List;

// the four CRUD operations every DAO offers
public interface CrudDAO<T> {

    boolean create(T entity) throws SQLException;   // Create
    T findById(int id) throws SQLException;         // Read one
    List<T> findAll() throws SQLException;          // Read all
    boolean update(T entity) throws SQLException;   // Update
    boolean delete(int id) throws SQLException;     // Delete
}

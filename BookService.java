package service;

import dao.BookDao;
import exceptions.BookNotFoundException;
import exceptions.LibraryException;
import model.Book;
import util.DateUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * Service managing books with runtime HashMap caching.
 */
public class BookService {
    private final BookDao bookDao;
    // Runtime HashMap for fast memory lookups
    private final Map<Integer, Book> bookMap;

    public BookService(BookDao bookDao) {
        this.bookDao = bookDao;
        this.bookMap = new HashMap<>();
        syncCache();
    }

    public synchronized void syncCache() {
        bookMap.clear();
        try {
            List<Book> books = bookDao.findAll();
            for (Book b : books) {
                bookMap.put(b.getId(), b);
            }
        } catch (SQLException e) {
            System.err.println("Failed to cache books: " + e.getMessage());
        }
    }

    public synchronized List<Book> getAllBooks() throws SQLException {
        List<Book> books = bookDao.findAll();
        // Update cache
        bookMap.clear();
        for (Book b : books) {
            bookMap.put(b.getId(), b);
        }
        return books;
    }

    public synchronized Book getBookById(int id) throws LibraryException {
        if (bookMap.containsKey(id)) {
            return bookMap.get(id);
        }
        try {
            Book book = bookDao.findById(id);
            if (book == null) {
                throw new BookNotFoundException(id);
            }
            bookMap.put(id, book);
            return book;
        } catch (SQLException e) {
            throw new LibraryException("Database error: " + e.getMessage(), 500);
        }
    }

    public List<Book> searchBooks(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return getAllBooks();
        }
        return bookDao.search(query.trim());
    }

    public synchronized Book addBook(Book book) throws LibraryException {
        validateBook(book);
        try {
            // Check for duplicate ISBN
            Book existing = bookDao.findByIsbn(book.getIsbn());
            if (existing != null) {
                throw new LibraryException("A book with ISBN '" + book.getIsbn() + "' already exists.", 409);
            }

            book.setCreatedAt(DateUtils.now());
            if (book.getAvailableCopies() == 0 && book.getTotalCopies() > 0) {
                book.setAvailableCopies(book.getTotalCopies());
            }

            int id = bookDao.insert(book);
            book.setId(id);
            bookMap.put(id, book);
            return book;
        } catch (SQLException e) {
            throw new LibraryException("Database error adding book: " + e.getMessage(), 500);
        }
    }

    public synchronized Book updateBook(Book book) throws LibraryException {
        validateBook(book);
        try {
            Book existing = bookDao.findById(book.getId());
            if (existing == null) {
                throw new BookNotFoundException(book.getId());
            }

            // Check if ISBN taken by another book
            Book isbnCheck = bookDao.findByIsbn(book.getIsbn());
            if (isbnCheck != null && isbnCheck.getId() != book.getId()) {
                throw new LibraryException("Another book already uses ISBN: " + book.getIsbn(), 409);
            }

            int issuedCount = existing.getTotalCopies() - existing.getAvailableCopies();
            if (book.getTotalCopies() < issuedCount) {
                throw new LibraryException("Total copies cannot be less than currently issued copies (" + issuedCount + ").", 400);
            }

            // Adjust available copies based on new total
            int diff = book.getTotalCopies() - existing.getTotalCopies();
            int newAvailable = existing.getAvailableCopies() + diff;
            book.setAvailableCopies(Math.max(0, newAvailable));

            boolean success = bookDao.update(book);
            if (!success) {
                throw new LibraryException("Failed to update book.", 500);
            }

            bookMap.put(book.getId(), book);
            return book;
        } catch (SQLException e) {
            throw new LibraryException("Database error updating book: " + e.getMessage(), 500);
        }
    }

    public synchronized boolean deleteBook(int id) throws LibraryException {
        try {
            Book book = bookDao.findById(id);
            if (book == null) {
                throw new BookNotFoundException(id);
            }

            if (bookDao.hasActiveIssuesOrReservations(id)) {
                throw new LibraryException("Cannot delete book '" + book.getTitle() + "' because it has active issues or reservations.", 400);
            }

            boolean deleted = bookDao.delete(id);
            if (deleted) {
                bookMap.remove(id);
            }
            return deleted;
        } catch (SQLException e) {
            throw new LibraryException("Database error deleting book: " + e.getMessage(), 500);
        }
    }

    private void validateBook(Book book) throws LibraryException {
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new LibraryException("Book title cannot be empty.", 400);
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new LibraryException("Book author cannot be empty.", 400);
        }
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new LibraryException("ISBN cannot be empty.", 400);
        }
        if (book.getTotalCopies() < 1) {
            throw new LibraryException("Total copies must be at least 1.", 400);
        }
    }
}

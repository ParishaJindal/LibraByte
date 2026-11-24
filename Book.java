package model;

/**
 * Book entity representing a title in the library catalog.
 * Encapsulates book attributes and copy tracking.
 */
public class Book extends BaseEntity {
    private String isbn;
    private String title;
    private String author;
    private String category;
    private String publisher;
    private int totalCopies;
    private int availableCopies;

    public Book() {
        super();
    }

    public Book(int id, String isbn, String title, String author, String category, String publisher, int totalCopies, int availableCopies) {
        super(id);
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publisher = publisher;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public int getIssuedCopies() {
        return Math.max(0, totalCopies - availableCopies);
    }

    @Override
    public String toString() {
        return "Book[id=" + id + ", isbn='" + isbn + "', title='" + title + "', author='" + author + 
               "', available=" + availableCopies + "/" + totalCopies + "]";
    }
}

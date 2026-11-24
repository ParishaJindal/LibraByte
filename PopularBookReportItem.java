package model;

/**
 * Report DTO for Frequently Borrowed / Popular Books.
 */
public class PopularBookReportItem {
    private int bookId;
    private String isbn;
    private String title;
    private String author;
    private String category;
    private int totalCopies;
    private int availableCopies;
    private int borrowCount;
    private int popularityRank;

    public PopularBookReportItem(int bookId, String isbn, String title, String author, String category,
                                 int totalCopies, int availableCopies, int borrowCount, int popularityRank) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.borrowCount = borrowCount;
        this.popularityRank = popularityRank;
    }

    public int getBookId() { return bookId; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public int getBorrowCount() { return borrowCount; }
    public int getPopularityRank() { return popularityRank; }

    public void setPopularityRank(int rank) { this.popularityRank = rank; }
}

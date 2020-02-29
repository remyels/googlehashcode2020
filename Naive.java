import java.util.*;
import java.util.Map.Entry;
import java.io.*;

public class Naive {
    static Scanner file;
    static PrintWriter pw;

    static int numBooks;
    static int numLibraries;
    static int totalNumDays;

    static int[] bookScores;

    static Map<Integer, Boolean> bookIdToScanned;

    static List<Library> libraries;

    static String[] files = { "b_read_on.txt", "c_incunabula.txt", "d_tough_choices.txt", "e_so_many_books.txt",
            "f_libraries_of_the_world.txt" };

    public static void main(String[] args) throws Exception {
        for (String filename : files) {
            libraries = new ArrayList<>();
            bookIdToScanned = new HashMap<>();
            file = new Scanner(new File(filename));
            pw = new PrintWriter(new BufferedWriter(new FileWriter("output_" + filename + ".txt")), true);
            numBooks = file.nextInt();
            numLibraries = file.nextInt();
            totalNumDays = file.nextInt();
            bookScores = new int[numBooks];
            for (int i = 0; i < numBooks; i++) {
                bookScores[i] = file.nextInt();
            }
            for (int i = 0; i < numLibraries; i++) {
                int libraryNumBooks = file.nextInt();
                int librarySignUpTime = file.nextInt();
                int libraryShippingPerDay = file.nextInt();
                List<Book> books = new ArrayList<>();
                for (int j = 0; j < libraryNumBooks; j++) {
                    int bookId = file.nextInt();
                    books.add(new Book(bookId, bookScores[bookId]));
                }
                libraries.add(new Library(i, books, librarySignUpTime, libraryShippingPerDay));
            }
            solve();
        }
    }

    public static void solve() {
        // Libraries out of order
        int curTime = 0;
        List<Integer> librariesScannedId = new ArrayList<>();
        LinkedHashMap<Integer, List<Integer>> libraryToBookIdsScanned = new LinkedHashMap<>();
        Collections.sort(libraries);
        for (int i = 0; i < numLibraries; i++) {
            curTime += libraries.get(i).getTime();
            // If the sign up time gets us above the total number of days dont consider the
            // library
            if (curTime >= totalNumDays)
                break;
            librariesScannedId.add(libraries.get(i).id);
            int perDayMax = libraries.get(i).getNumberOfBooksScannedPerDay();
            List<Book> booksInLibrary = libraries.get(i).books;
            Collections.sort(booksInLibrary, (b1, b2) -> b2.score - b1.score);
            int curTimeCopy = curTime;
            int offset = 0;
            List<Integer> pickedBooks = new ArrayList<>();
            outer: while (curTimeCopy < totalNumDays) {
                int picked = 0;
                while (picked < perDayMax) {
                    if (bookIdToScanned.containsKey(booksInLibrary.get(offset + picked).id)
                            && bookIdToScanned.get(booksInLibrary.get(offset + picked).id)) {
                        picked++;
                    } else {
                        bookIdToScanned.put(booksInLibrary.get(offset + picked).id, true);
                        pickedBooks.add(booksInLibrary.get(offset + picked).id);
                        picked += 1;
                    }
                    if (offset + picked >= booksInLibrary.size()) {
                        break outer;
                    }
                }
                curTimeCopy += 1;
                offset += picked;
                if (offset + picked >= booksInLibrary.size()) {
                    break;
                }
            }
            libraryToBookIdsScanned.put(libraries.get(i).id, pickedBooks);
        }
        libraryToBookIdsScanned.entrySet()
                .removeIf(
                        entry -> (entry.getValue().size() == 0));
        pw.println(libraryToBookIdsScanned.entrySet().size());
        for (Entry<Integer, List<Integer>> e : libraryToBookIdsScanned.entrySet()) {
            pw.println(e.getKey() + " " + e.getValue().size());
            for (Integer bookId : e.getValue()) {
                pw.print(bookId + " ");
            }
            pw.println();
        }
    }
}

class Library implements Comparable{
    int id;
    List<Book> books;
    int time;
    int numberOfBooksScannedPerDay;

    public Library(int id, List<Book> books, int time, int numberOfBooksScannerPerDay) {
        this.id = id;
        this.books = books;
        this.time = time;
        this.numberOfBooksScannedPerDay = numberOfBooksScannerPerDay;
    }

    public int getTime() {
        return time;
    }

    public int getNumberOfBooksScannedPerDay() {
        return numberOfBooksScannedPerDay;
    }

    public int getTotalBookScore() {
        int total = 0;
        for (Book b : books) {
            total += b.score;
        }
        return total;
    }

    @Override
    public int compareTo(Object o) {
        Library lib2 = (Library) o;
        Float f1 = (float) (1.0*lib2.getTotalBookScore() / (lib2.getTime() * 1.0));
        Float f2 = (float) (1.0*this.getTotalBookScore() / (this.getTime() * 1.0));
        return f1.compareTo(f2);
    }
}

class Book {
    int id;
    int score;

    public Book(int id, int score) {
        this.id = id;
        this.score = score;
    }
}
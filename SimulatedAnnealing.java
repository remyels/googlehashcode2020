import java.util.*;
import java.util.Map.Entry;
import java.io.*;

public class SimulatedAnnealing {
	static Scanner file;
	static PrintWriter pw;

	static int numBooks;
	static int numLibraries;
	static int totalNumDays;

	static int[] bookScores;

	static Map<Integer, Boolean> bookIdToScanned;

	static List<Library> libraries;

	static List<Integer> overallLibrariesScannedId = new ArrayList<>();
	static LinkedHashMap<Integer, List<Integer>> overallLibraryToBookIdsScanned = new LinkedHashMap<>();

	static List<Integer> librariesScannedId;
	static LinkedHashMap<Integer, List<Integer>> libraryToBookIdsScanned;
	
	static int cur_max = 0;
	static int overall_max = 0;

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
			
			List<Integer> ok1 = new ArrayList<>();
			LinkedHashMap<Integer, List<Integer>> ok2 = new LinkedHashMap<>();
			
			cur_max = 0;
			for (int k = 0; k < 10; k++) {
				System.out.println(k);
				solve();
				if (cur_max > overall_max) {
					overall_max = cur_max;
					ok1 = overallLibrariesScannedId;
					ok2 = overallLibraryToBookIdsScanned;
				}
			}
			
			pw.println(ok2.entrySet().size());
			for (Entry<Integer, List<Integer>> e : ok2.entrySet()) {
				pw.println(e.getKey() + " " + e.getValue().size());
				for (Integer bookId : e.getValue()) {
					pw.print(bookId + " ");
				}
				pw.println();
			}
		}
	}

	public static void solve() {
		double tmax = 5000;
		double tmin = 100;
		double step = (tmax - tmin) / 1000;
		int[] config = new int[numLibraries];
		for (int i = 0; i < numLibraries; i++) {
			config[i] = libraries.get(i).id;
		}
		for (int i = 0; i < config.length; i++) {
			int j = (int) Math.floor(Math.random() * (numLibraries - i)) + i;
			int temp = config[j];
			config[j] = config[i];
			config[i] = temp;
		}

		for (double i = tmax; i >= tmin; i -= step) {
			int curEnergy = getEnergy(config);
			int[] neighbor = getNeighbor(config);
			int neighborEnergy = getEnergy(neighbor);
			cur_max = Math.max(overall_max, neighborEnergy);
			int deltaEnergy = neighborEnergy - curEnergy;
			if (deltaEnergy > 0) {
				overallLibrariesScannedId = librariesScannedId;
				overallLibraryToBookIdsScanned = libraryToBookIdsScanned;
			} else if (Math.pow(Math.E, 1.0 * deltaEnergy / i) > Math.random()) {
				overallLibrariesScannedId = librariesScannedId;
				overallLibraryToBookIdsScanned = libraryToBookIdsScanned;
			}
		}
	}

	public static int[] getNeighbor(int[] order) {
		int[] neighbor = order.clone();
		int randind = (int) Math.floor(Math.random() * numLibraries);
		int temp = neighbor[randind];
		neighbor[randind] = neighbor[(randind + 1) % numLibraries];
		neighbor[(randind + 1) % numLibraries] = temp;
		return neighbor;
	}

	public static int getEnergy(int[] order) {

		int curTime = 0;
		librariesScannedId = new ArrayList<>();
		libraryToBookIdsScanned = new LinkedHashMap<>();

		for (int i = 0; i < numLibraries; i++) {
			int currentLibrary = order[i];
			curTime += libraries.get(currentLibrary).getTime();
			// If the sign up time gets us above the total number of days dont consider the
			// library
			if (curTime >= totalNumDays)
				break;
			librariesScannedId.add(libraries.get(currentLibrary).id);
			int perDayMax = libraries.get(currentLibrary).getNumberOfBooksScannedPerDay();
			List<Book> booksInLibrary = libraries.get(currentLibrary).books;
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
			libraryToBookIdsScanned.put(currentLibrary, pickedBooks);
		}

		long score = 0;

		Set<Integer> picked = new HashSet<>();
		for (Integer lib : libraryToBookIdsScanned.keySet()) {
			for (Integer book : libraryToBookIdsScanned.get(lib)) {
				picked.add(book);
			}
		}

		for (Integer book : picked) {
			score += bookScores[book];
		}

		return (int) score;
	}
}

class Library implements Comparable {
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
		Float f1 = (float) (1.0 * lib2.getTotalBookScore()
				/ (lib2.getNumberOfBooksScannedPerDay() * 1.0 + lib2.getTime() * 10000));
		Float f2 = (float) (1.0 * this.getTotalBookScore()
				/ (this.getNumberOfBooksScannedPerDay() * 1.0 + this.getTime() * 10000));
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
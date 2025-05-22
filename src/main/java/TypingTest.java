import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class TypingTest {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Random  rnd     = new Random();
    private static final ExecutorService pool = Executors.newSingleThreadExecutor();

    private static boolean askAndCheck(String word, long timeoutMs) {
        System.out.println(word);
        Future<String> f = pool.submit(() -> {
            System.out.print("Enter answer: ");
            return scanner.nextLine();
        });

        try {
            String typed = f.get(timeoutMs, TimeUnit.MILLISECONDS);
            System.out.println("You typed: " + typed);
            return word.equals(typed);
        } catch (TimeoutException e) {
            System.out.println("Time's up!");
            f.cancel(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;   // treat as incorrect
    }

    public static void typingTest(List<String> words) throws InterruptedException {
        int correct = 0, incorrect = 0;
        long total = 0;

        for (int i = 0; i < 20; i++) {
            String w = words.get(rnd.nextInt(words.size()));
            long t0 = System.currentTimeMillis();
            if (askAndCheck(w, Math.max(5000, w.length() * 500))) correct++;
            else incorrect++;
            total += System.currentTimeMillis() - t0;
            Thread.sleep(2000);
        }

        System.out.printf("""
                Test complete!
                Total correct   : %d
                Total incorrect : %d
                Total time      : %.2f s
                Avg per word    : %.0f ms%n""",
                correct, incorrect, total / 1000.0, total / 20.0);
    }

    public static List<String> readWords(String path) {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String s;
            while ((s = br.readLine()) != null) list.add(s.trim());
        } catch (IOException e) { e.printStackTrace(); }
        return list;
    }

    public static void main(String[] args) throws InterruptedException {
        List<String> words = readWords("D:\\Documents\\IdeaProjects\\Fifth-Assignment-Multithreading-Basics\\src\\main\\resources\\Words.txt");
        if (words.isEmpty()) { System.out.println("No words found."); return; }
        typingTest(words);
        pool.shutdownNow();
    }
}

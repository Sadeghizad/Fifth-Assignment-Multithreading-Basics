import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ReportGenerator {

    
    static class Product {
        private final int id;
        private final String name;
        private final double price;

        public Product(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
        public int getId()   { return id;   }
        public String getName() { return name; }
        public double getPrice() { return price; }
    }

    
    private static final Product[] PRODUCT_CATALOG = new Product[10];

    
    private static final Map<Integer, Double> YEARLY_SALES = new ConcurrentHashMap<>();

    
    static class TaskRunnable implements Runnable {
        private final Path filePath;
        private double totalCost;
        private int    totalAmount;
        private int    totalDiscountSum;
        private int    totalLines;
        private Product mostExpensiveProduct;
        private double  highestCostAfterDiscount;
        private final DecimalFormat df = new DecimalFormat("#0.00");

        public TaskRunnable(Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try {
                for (String line : Files.readAllLines(filePath)) {
                    
                    if (line.isBlank()) continue;
                    String[] parts = line.split(",");
                    int productId      = Integer.parseInt(parts[0].trim());
                    int amount         = Integer.parseInt(parts[1].trim());
                    int discountAmount = Integer.parseInt(parts[2].trim()); 

                    Product p = PRODUCT_CATALOG[productId - 1];
                    if (p == null) {
                        System.err.println("Unknown product id " + productId + " in " + filePath);
                        continue;
                    }

                    double discountedPricePerUnit = p.getPrice() - discountAmount;
                    double lineCost = discountedPricePerUnit * amount;

                    
                    totalLines++;
                    totalAmount      += amount;
                    totalDiscountSum += discountAmount;
                    totalCost        += lineCost;

                    if (lineCost > highestCostAfterDiscount) {
                        highestCostAfterDiscount = lineCost;
                        mostExpensiveProduct     = p;
                    }
                }

                
                int year = extractYearFromFilename(filePath.getFileName().toString());
                YEARLY_SALES.merge(year, totalCost, Double::sum);

            } catch (IOException ex) {
                System.err.println("Failed to read " + filePath + ": " + ex.getMessage());
            }
        }

        
        public void makeReport() {
            System.out.println("------ Report for file: " + filePath.getFileName() + " ------");

            System.out.printf("Total cost: %s%n", df.format(totalCost));
            System.out.println ("Total items bought: " + totalAmount);

            double avgDiscount = totalLines == 0 ? 0
                    : (double) totalDiscountSum / totalLines;
            System.out.printf("Average discount: %s%n", df.format(avgDiscount));

            if (mostExpensiveProduct != null) {
                System.out.printf("Most expensive purchase after discount: %s (%.2f)%n",
                        mostExpensiveProduct.getName(), highestCostAfterDiscount);
            } else {
                System.out.println("Most expensive purchase after discount: —");
            }
            System.out.println();
        }

        
        private int extractYearFromFilename(String name) {
            String yearPart = name.substring(0, name.indexOf('_'));
            return Integer.parseInt(yearPart);
        }
    }

    
    private static void loadProducts(Path productsFile) throws IOException {
        for (String line : Files.readAllLines(productsFile)) {
            if (line.isBlank()) continue;
            String[] parts = line.split(",");
            int    id    = Integer.parseInt(parts[0].trim());
            String name  = parts[1].trim();
            double price = Double.parseDouble(parts[2].trim());

            PRODUCT_CATALOG[id - 1] = new Product(id, name, price); 
        }
        
        if (Arrays.stream(PRODUCT_CATALOG).anyMatch(p -> p == null)) {
            System.err.println("⚠ Some catalogue slots are still null – check Products.txt");
        }
    }

    
    public static void main(String[] args) throws Exception {
        
        loadProducts(Path.of("D:\\Documents\\IdeaProjects\\Fifth-Assignment-Multithreading-Basics\\src\\main\\resources\\Products.txt"));

        
        Path[] orderFiles = {
                Path.of("D:\\Documents\\IdeaProjects\\Fifth-Assignment-Multithreading-Basics\\src\\main\\resources\\2021_order_details.txt"),
                Path.of("D:\\Documents\\IdeaProjects\\Fifth-Assignment-Multithreading-Basics\\src\\main\\resources\\2022_order_details.txt"),
                Path.of("D:\\Documents\\IdeaProjects\\Fifth-Assignment-Multithreading-Basics\\src\\main\\resources\\2023_order_details.txt"),
                Path.of("D:\\Documents\\IdeaProjects\\Fifth-Assignment-Multithreading-Basics\\src\\main\\resources\\2024_order_details.txt")
        };

        
        TaskRunnable[] tasks  = new TaskRunnable[orderFiles.length];
        Thread[]       threads = new Thread[orderFiles.length];

        for (int i = 0; i < orderFiles.length; i++) {
            tasks[i]   = new TaskRunnable(orderFiles[i]);
            threads[i] = new Thread(tasks[i], "Worker-" + orderFiles[i].getFileName());
            threads[i].start();
        }


        for (Thread t : threads) {
            try {
                t.join();              
            } catch (InterruptedException e) {
                
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while waiting for " + t.getName());
            }
        }

        
        System.out.println("\n========= INDIVIDUAL REPORTS =========\n");
        for (TaskRunnable t : tasks) t.makeReport();

        
        
        SalesChartApp.launchChart(YEARLY_SALES);
    }

    
    
    
    public static class SalesChartApp extends javafx.application.Application {
        private static Map<Integer, Double> data;

        public static void launchChart(Map<Integer, Double> map) {
            data = map;
            javafx.application.Application.launch(SalesChartApp.class);
        }

        @Override
        public void start(javafx.stage.Stage stage) {
            try {
                javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
                javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
                xAxis.setLabel("Year");
                yAxis.setLabel("Total Sales (after discount)");

                // Calculate bounds from data
                int minX = data.keySet().stream().min(Integer::compareTo).orElse(0);
                int maxX = data.keySet().stream().max(Integer::compareTo).orElse(1);
                double minY = data.values().stream().min(Double::compareTo).orElse(0.0);
                double maxY = data.values().stream().max(Double::compareTo).orElse(1.0);

                // Apply padding
                int paddingX = 1;
                double paddingY = (maxY - minY) * 0.1;

                xAxis.setAutoRanging(false);
                xAxis.setLowerBound(minX - paddingX);
                xAxis.setUpperBound(maxX + paddingX);
                xAxis.setTickUnit(1);

                yAxis.setAutoRanging(false);
                yAxis.setLowerBound(minY - paddingY);
                yAxis.setUpperBound(maxY + paddingY);
                yAxis.setTickUnit(Math.max(1, (maxY - minY) / 10));

                javafx.scene.chart.LineChart<Number, Number> chart =
                        new javafx.scene.chart.LineChart<>(xAxis, yAxis);
                chart.setTitle("Sales Over Time");

                javafx.scene.chart.XYChart.Series<Number, Number> series = new javafx.scene.chart.XYChart.Series<>();
                series.setName("Sales");

                data.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> series.getData().add(
                                new javafx.scene.chart.XYChart.Data<>(e.getKey(), e.getValue())));

                chart.getData().add(series);

                stage.setScene(new javafx.scene.Scene(chart, 800, 600));
                stage.setTitle("Sales Over Time");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}

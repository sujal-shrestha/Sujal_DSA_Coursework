import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteOptimizationApp extends JFrame {

    private JTextArea deliveryListArea;
    private JComboBox<String> algorithmSelection;
    private JTextField vehicleCapacityField;
    private JTextField vehicleDistanceField;
    private JButton optimizeButton;
    private JTextArea resultArea;
    private RouteVisualizationPanel routeVisualizationPanel;

    public RouteOptimizationApp() {
        setTitle("Route Optimization for Delivery Service");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(6, 2));

        inputPanel.add(new JLabel("Delivery List (address, priority):"));
        deliveryListArea = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(deliveryListArea);
        inputPanel.add(scrollPane);

        inputPanel.add(new JLabel("Select Algorithm:"));
        algorithmSelection = new JComboBox<>(new String[]{"Dijkstra", "A*", "Greedy"});
        inputPanel.add(algorithmSelection);

        inputPanel.add(new JLabel("Vehicle Capacity:"));
        vehicleCapacityField = new JTextField();
        inputPanel.add(vehicleCapacityField);

        inputPanel.add(new JLabel("Vehicle Driving Distance:"));
        vehicleDistanceField = new JTextField();
        inputPanel.add(vehicleDistanceField);

        optimizeButton = new JButton("Optimize Route");
        optimizeButton.addActionListener(new OptimizeButtonListener());
        inputPanel.add(optimizeButton);

        add(inputPanel, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        routeVisualizationPanel = new RouteVisualizationPanel();
        add(routeVisualizationPanel, BorderLayout.SOUTH);
    }

    private class OptimizeButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String selectedAlgorithm = (String) algorithmSelection.getSelectedItem();
            String deliveryList = deliveryListArea.getText();
            int vehicleCapacity = Integer.parseInt(vehicleCapacityField.getText());
            int drivingDistance = Integer.parseInt(vehicleDistanceField.getText());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                String optimizedRoute = performRouteOptimization(selectedAlgorithm, deliveryList, vehicleCapacity, drivingDistance);
                SwingUtilities.invokeLater(() -> {
                    resultArea.setText(optimizedRoute);
                    routeVisualizationPanel.setRoute(optimizedRoute);
                    routeVisualizationPanel.repaint();
                });
            });
            executor.shutdown();
        }
    }

    private String performRouteOptimization(String algorithm, String deliveryList, int vehicleCapacity, int drivingDistance) {
        String[] deliveries = deliveryList.split("\n");
        List<DeliveryPoint> deliveryPoints = new ArrayList<>();
        for (String delivery : deliveries) {
            String[] parts = delivery.split(",");
            deliveryPoints.add(new DeliveryPoint(parts[0].trim(), Integer.parseInt(parts[1].trim())));
        }

        List<DeliveryPoint> optimizedRoute = new ArrayList<>();

        switch (algorithm) {
            case "Dijkstra":
                optimizedRoute = dijkstraAlgorithm(deliveryPoints, vehicleCapacity, drivingDistance);
                break;
            case "A*":
                optimizedRoute = aStarAlgorithm(deliveryPoints, vehicleCapacity, drivingDistance);
                break;
            case "Greedy":
                optimizedRoute = greedyAlgorithm(deliveryPoints, vehicleCapacity, drivingDistance);
                break;
            default:
                return "Algorithm not recognized!";
        }

        StringBuilder result = new StringBuilder();
        result.append(algorithm).append(" optimized route:\n");
        for (DeliveryPoint point : optimizedRoute) {
            result.append(point.address).append("\n");
        }
        result.append("Vehicle Capacity: ").append(vehicleCapacity).append("\n");
        result.append("Driving Distance: ").append(drivingDistance);

        return result.toString();
    }

    private List<DeliveryPoint> dijkstraAlgorithm(List<DeliveryPoint> deliveryPoints, int vehicleCapacity, int drivingDistance) {
        deliveryPoints.sort(Comparator.comparingInt(dp -> dp.priority));
        return handleVehicleConstraints(deliveryPoints, vehicleCapacity, drivingDistance);
    }

    private List<DeliveryPoint> aStarAlgorithm(List<DeliveryPoint> deliveryPoints, int vehicleCapacity, int drivingDistance) {
        deliveryPoints.sort(Comparator.comparingInt(dp -> dp.priority));
        Collections.reverse(deliveryPoints);
        return handleVehicleConstraints(deliveryPoints, vehicleCapacity, drivingDistance);
    }

    private List<DeliveryPoint> greedyAlgorithm(List<DeliveryPoint> deliveryPoints, int vehicleCapacity, int drivingDistance) {
        Collections.shuffle(deliveryPoints);
        return handleVehicleConstraints(deliveryPoints, vehicleCapacity, drivingDistance);
    }

    private List<DeliveryPoint> handleVehicleConstraints(List<DeliveryPoint> deliveryPoints, int vehicleCapacity, int drivingDistance) {
        List<DeliveryPoint> constrainedRoute = new ArrayList<>();
        int totalDistance = 0;
        for (DeliveryPoint point : deliveryPoints) {
            if (constrainedRoute.size() < vehicleCapacity && totalDistance < drivingDistance) {
                constrainedRoute.add(point);
                totalDistance += 10;
            } else {
                break;
            }
        }
        return constrainedRoute;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RouteOptimizationApp app = new RouteOptimizationApp();
            app.setVisible(true);
        });
    }

    static class DeliveryPoint {
        String address;
        int priority;

        public DeliveryPoint(String address, int priority) {
            this.address = address;
            this.priority = priority;
        }
    }

    class RouteVisualizationPanel extends JPanel {
        private List<Point> points;
        private String route;

        public RouteVisualizationPanel() {
            this.points = new ArrayList<>();
        }

        public void setRoute(String route) {
            this.route = route;
            points.clear();
            String[] locations = route.split("\n");
            Random random = new Random();
            for (String loc : locations) {
                points.add(new Point(50 + random.nextInt(800), 50 + random.nextInt(400)));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.BLACK);
            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                g.fillOval(p.x, p.y, 10, 10);
                g.drawString("Point " + (i + 1), p.x + 15, p.y + 5);
                if (i > 0) {
                    Point prev = points.get(i - 1);
                    g.drawLine(prev.x + 5, prev.y + 5, p.x + 5, p.y + 5);
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(800, 400);
        }
    }
}

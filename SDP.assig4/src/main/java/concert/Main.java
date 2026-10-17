package concert;
import concert.access.AccessControlGate;
import concert.access.AccessPassRepository;
import concert.access.RefundService;
import concert.facade.BoxOfficeAdminFacade;
import concert.facade.InvalidRequestException;
import concert.facade.Order;
import concert.facade.OrderRepository;
import concert.facade.PurchaseFailedException;
import concert.facade.TicketBookingFacade;
import concert.seating.DynamicPricingEngine;
import concert.seating.ReservationRepository;
import concert.seating.SeatingChart;
import concert.ticketing.FraudChecker;
import concert.ticketing.TicketPrinter;
import concert.ticketing.TicketRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Path dataDir = Paths.get(args.length > 0 ? args[0] : "data");
        SeatingChart seating = new SeatingChart(new ReservationRepository(dataDir), new DynamicPricingEngine());
        TicketPrinter printer = new TicketPrinter(new TicketRepository(dataDir), new FraudChecker());
        AccessControlGate gate = new AccessControlGate(new AccessPassRepository(dataDir), new RefundService());
        OrderRepository orders = new OrderRepository(dataDir);

        TicketBookingFacade booking = new TicketBookingFacade(seating, printer, gate, orders);
        BoxOfficeAdminFacade admin = new BoxOfficeAdminFacade(seating, printer, gate, orders);

        System.out.println("--- Concert Ticketing --- (data folder: " + dataDir.toAbsolutePath() + ")");
        int fixed = admin.recoverUnfinishedOrders();
        System.out.println("Unfinished orders were found and compensated: " + fixed);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        String concert = ask(scanner, "Concert " + SeatingChart.CONCERTS);
                        String seat = ask(scanner, "Seat (for example A1)");
                        String buyer = ask(scanner, "Buyer name");
                        Order order = booking.buyTicket(concert, seat, buyer);
                        System.out.println("SUCCESS: " + order.getId() + ", price " + order.getPrice());
                        break;
                    case "2":
                        String concertForSeats = ask(scanner, "Concert " + SeatingChart.CONCERTS);
                        System.out.println("Free seats: " + booking.showFreeSeats(concertForSeats));
                        break;
                    case "3":
                        for (Order o : admin.listOrders()) {
                            System.out.println(o.getId() + " " + o.getStatus() + " buyer=" + o.getBuyer()
                                    + " seat=" + o.getSeatId() + " price=" + o.getPrice());
                        }
                        break;
                    case "4":
                        System.out.println(admin.inspect(ask(scanner, "Order id (for example ORD-1)")));
                        break;
                    case "5":
                        admin.blockBuyer(ask(scanner, "Buyer name to block"));
                        System.out.println("Buyer blocked. Their next purchase fails at step B.");
                        break;
                    case "6":
                        admin.closeGate(ask(scanner, "Concert " + SeatingChart.CONCERTS));
                        System.out.println("Gate closed. The next purchase for it fails at step C.");
                        break;
                    case "7":
                        admin.openGate(ask(scanner, "Concert " + SeatingChart.CONCERTS));
                        System.out.println("Gate opened.");
                        break;
                    case "8":
                        int refund = admin.refundOrder(ask(scanner, "Order id to refund"));
                        System.out.println("Order refunded, money back: " + refund);
                        break;
                    case "9":
                        admin.compensateOrder(ask(scanner, "Unfinished order id to compensate"));
                        System.out.println("Order compensated.");
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Unknown choice");
                }
            } catch (PurchaseFailedException e) {
                System.out.println("PURCHASE FAILED (" + e.getOrderId() + "): " + e.getMessage());
            } catch (InvalidRequestException e) {
                System.out.println("INVALID REQUEST: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
        System.out.println("Bye!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1 - Buy a ticket");
        System.out.println("2 - Show free seats");
        System.out.println("3 - List all orders");
        System.out.println("4 - Inspect an order (state of A, B and C on disk)");
        System.out.println("5 - Block a buyer");
        System.out.println("6 - Close the gate of a concert (makes step C fail)");
        System.out.println("7 - Open the gate of a concert");
        System.out.println("8 - Refund a completed order");
        System.out.println("9 - Compensate an unfinished order manually");
        System.out.println("0 - Exit");
        System.out.print("> ");
    }

    private static String ask(Scanner scanner, String question) {
        System.out.print(question + ": ");
        return scanner.nextLine().trim();
    }
}

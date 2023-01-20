import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;
import java.util.Map;

public class RoomBookingBot {
    public static void main(String[] args) throws TelegramApiException {
        // Initialize Telegram Bot API
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        // Create the bot
        BookingBot bot = new BookingBot();

        // Register the bot
        try {
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

class BookingBot extends TelegramLongPollingBot {
    private Map<Long, Booking> bookings = new HashMap<>();
    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private final String[] timeSlots = {"8:00-10:00", "10:00-12:00", "12:00-14:00", "14:00-16:00", "16:00-18:00"};
    private final String[] rooms = {"Room 1", "Room 2", "Room 3", "Room 4"};
    private final boolean[][][] booked = new boolean[days.length][timeSlots.length][rooms.length];
    String pred;

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if(messageText.equals("/start")){
                sendMessage(chatId,"Hello! Please use /book to book room");
            }
            else if (messageText.equals("/book")) {
                bookings.put(chatId, new Booking());
                sendMessage(chatId, "Please select day:\n" + getDaysMenu());
                pred = "book";
            } else if (isValidDay(messageText) && pred.equals("book")) {
                bookings.get(chatId).setDay(Integer.parseInt(messageText) - 1);
                sendMessage(chatId, "Please select time slot:\n" + getTimeSlotsMenu());
                pred = "day";
            } else if (isValidTimeSlot(messageText) && pred.equals("day")) {
                bookings.get(chatId).setTimeSlot(Integer.parseInt(messageText) - 1);
                sendMessage(chatId, "Please select room:\n" + getRoomsMenu());
                pred = "time";
            } else if (isValidRoom(messageText) && pred.equals("time")) {
                pred="";
                bookings.get(chatId).setRoom(Integer.parseInt(messageText) - 1);
                if (isRoomAvailable(bookings.get(chatId))) {
                    bookRoom(bookings.get(chatId));
                    sendMessage(chatId, rooms[bookings.get(chatId).getRoom()] + " has been successfully booked for " + days[bookings.get(chatId).getDay()] + " at " + timeSlots[bookings.get(chatId).getTimeSlot()] + ".");
                } else {
                    sendMessage(chatId, rooms[bookings.get(chatId).getRoom()] + " is not available for " + days[bookings.get(chatId).getDay()] + " at " + timeSlots[bookings.get(chatId).getTimeSlot()] + ". Please select different time or room.");
                }
            } else if (messageText.equals("/check")) {
                checkBooking(chatId);
            } else if (messageText.equals("/cancel")) {
                cancelBooking(chatId);
            } else {
                sendMessage(chatId, "Invalid input. Please use /book, /check or /cancel.");
                bookings.remove(chatId);
            }
        }
    }

    public String getBotUsername() {
        return "SDU_BOT";
    }

    public String getBotToken() {
        return "5757997047:AAH280Wq84jJDi8HgZBOMf7dr6oBew0ZOJI";
    }

    private void sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getDaysMenu() {
        StringBuilder menu = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            menu.append((i + 1) + ". " + days[i] + "\n");
        }
        return menu.toString();
    }

    private String getTimeSlotsMenu() {
        StringBuilder menu = new StringBuilder();
        for (int i = 0; i < timeSlots.length; i++) {
            menu.append((i + 1) + ". " + timeSlots[i] + "\n");
        }
        return menu.toString();
    }

    private String getRoomsMenu() {
        StringBuilder menu = new StringBuilder();
        for (int i = 0; i < rooms.length; i++) {
            menu.append((i + 1) + ". " + rooms[i] + "\n");
        }
        return menu.toString();
    }

    private boolean isValidDay(String input) {
        try {
            int day = Integer.parseInt(input);
            return day > 0 && day <= days.length;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidTimeSlot(String input) {
        try {
            int timeSlot = Integer.parseInt(input);
            return timeSlot > 0 && timeSlot <= timeSlots.length;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidRoom(String input) {
        try {
            int room = Integer.parseInt(input);
            return room > 0 && room <= rooms.length;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isRoomAvailable(Booking booking) {
        return !booked[booking.getDay()][booking.getTimeSlot()][booking.getRoom()];
    }

    private void bookRoom(Booking booking) {
        booked[booking.getDay()][booking.getTimeSlot()][booking.getRoom()] = true;
    }

    private void checkBooking(long chatId) {
        if (bookings.containsKey(chatId)) {
            int room = bookings.get(chatId).getRoom();
            int day = bookings.get(chatId).getDay();
            int timeSlot = bookings.get(chatId).getTimeSlot();
            sendMessage(chatId, "You have booked " + rooms[room] + " for " + days[day] + " at " + timeSlots[timeSlot]);
        } else {
            sendMessage(chatId, "You have not booked any room.");
        }
    }

    private void cancelBooking(long chatId) {
        if (bookings.containsKey(chatId)) {
            int room = bookings.get(chatId).getRoom();
            int day = bookings.get(chatId).getDay();
            int timeSlot = bookings.get(chatId).getTimeSlot();
            booked[day][timeSlot][room] = false;
            bookings.remove(chatId);
            sendMessage(chatId, "Your booking for " + rooms[room] + " on " + days[day] + " at " + timeSlots[timeSlot] + " has been cancelled.");
        } else {
            sendMessage(chatId, "You have not booked any room.");
        }
    }
}

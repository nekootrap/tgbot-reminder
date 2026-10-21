package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final Pattern REMINDER_PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository repository; 

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            // проверка пустоты сообщений
            if (update.message() != null && update.message().text() != null) {
                String text = update.message().text();
                long chatId = update.message().chat().id();

            // команда start
            if ("/start".equals(text)) {
                SendMessage message = new SendMessage(chatId, 
                    "Привет! Для получения справки используйте команду /help"
                );
                telegramBot.execute(message);
                continue;
            }

            // команда help
            if ("/help".equals(text)) {
                SendMessage message = new SendMessage(chatId, 
                    "Для создания напоминания отправьте сообщение в формате:\n" +
                    "ДД.ММ.ГГГГ ЧЧ:ММ текст напоминания\n" +
                    "Пример: 25.12.2026 10:00 Купить подарок"
                );
                telegramBot.execute(message);
                continue;
            }

            // сообщение с датой и временем
            Matcher matcher = REMINDER_PATTERN.matcher(text);
            if (matcher.matches()) {
                String dateTimeStr = matcher.group(1);
                String reminderText = matcher.group(3);

                try {
                    LocalDateTime remindTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
                        
                    NotificationTask task = new NotificationTask(chatId, reminderText, remindTime);
                    repository.save(task);

                    SendMessage confirmation = new SendMessage(chatId, 
                        "Сохранено!" + "\n" +
                        "Время: " + dateTimeStr + "\n" +
                        "Напоминание: " + reminderText);
                    telegramBot.execute(confirmation);
                        
                } catch (DateTimeParseException e) {
                    logger.error("Ошибка парсинга даты '{}': {}", dateTimeStr, e.getMessage());
                    telegramBot.execute(new SendMessage(chatId, 
                        "Ошибка! Используйте формат ДД.ММ.ГГГГ ЧЧ:ММ"));
                }
            } else {
                telegramBot.execute(new SendMessage(chatId, 
                    "️Неизвестное сообщение. Используйте команду /help"));
            }
        }
    }
        
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}

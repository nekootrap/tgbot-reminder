package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderScheduler {

    @Autowired
    private NotificationTaskRepository repository;

    @Autowired
    private TelegramBot telegramBot;

    @Scheduled(cron = "0 0/1 * * * *")
    public void checkAndSendReminders() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        // список задач на эту минуту
        List<NotificationTask> tasksToSend = repository.findByDateTime(now);

        for (NotificationTask task : tasksToSend) {
            SendMessage message = new SendMessage(task.getChatId(), "Напоминание: " + task.getMessage());
            
            try {
                telegramBot.execute(message);
            } catch (Exception e) {
                System.err.println("Ошибка при отправке напоминания для chatId=" + task.getChatId() + ": " + e.getMessage());
            }
        }
    }
}
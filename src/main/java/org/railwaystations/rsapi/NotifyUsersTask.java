package org.railwaystations.rsapi;

import org.railwaystations.rsapi.db.InboxDao;
import org.railwaystations.rsapi.db.UserDao;
import org.railwaystations.rsapi.mail.Mailer;
import org.railwaystations.rsapi.model.InboxEntry;
import org.railwaystations.rsapi.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Component
public class NotifyUsersTask {

    private static final Logger LOG = LoggerFactory.getLogger(NotifyUsersTask.class);

    private final UserDao userDao;
    private final InboxDao inboxDao;
    private final Mailer mailer;

    public NotifyUsersTask(final UserDao userDao, final InboxDao inboxDao, final Mailer mailer) {
        super();
        this.userDao = userDao;
        this.inboxDao = inboxDao;
        this.mailer = mailer;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void notifyUsers() {
        final List<InboxEntry> entries = inboxDao.findInboxEntriesToNotify();
        final Map<Integer, List<InboxEntry>> entriesPerUser = entries.stream()
                .collect(groupingBy(InboxEntry::getPhotographerId));
        entriesPerUser.forEach((userId, entriesForUser) -> userDao.findById(userId).ifPresent(user -> {
            if (user.getEmail() != null && user.isEmailVerified() && user.isSendNotifications()) {
                sendEmailNotification(user, entriesForUser);
            }
        }));
        final List<Integer> ids = entries.stream().map(InboxEntry::getId).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            inboxDao.updateNotified(ids);
        }
    }

    private void sendEmailNotification(@NotNull final User user, final List<InboxEntry> entriesForUser) {
        final StringBuilder report = new StringBuilder();
        entriesForUser.forEach(entry -> report.append(entry.getId()).append(". ").append(entry.getTitle())
                .append(entry.isProblemReport() ? " (" + entry.getProblemReportType() + ")" : "")
                .append(": ")
                .append(entry.getRejectReason() == null ? "accepted" : "rejected")
                .append(entry.getRejectReason() == null ? "" : " - " + entry.getRejectReason())
                .append("\n"));

        final String text = String.format("Hello %1$s,%n%n" +
                "thank you for your contributions.%n%n" +
                "Cheers%n" +
                "Your Railway-Stations-Team%n" +
                "%n---%n" +
                "Hallo %1$s,%n%n" +
                "vielen Dank für Deine Beiträge.%n%n" +
                "Viele Grüße%n" +
                "Dein Bahnhofsfoto-Team%n%n" +
                "---------------------------------%n%n%2$s" , user.getName(), report);
        mailer.send(user.getEmail(), "Railway-Stations.org review result", text);
        LOG.info("Email notification sent to {}", user.getEmail());
    }

}

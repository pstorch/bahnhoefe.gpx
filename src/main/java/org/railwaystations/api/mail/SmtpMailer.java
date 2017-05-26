package org.railwaystations.api.mail;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class SmtpMailer implements Mailer {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpMailer.class);

    private final String from;

    private final Session session;

    public SmtpMailer(@JsonProperty("host") final String host,
                      @JsonProperty("user") final String user,
                      @JsonProperty("passwd") final String passwd,
                      @JsonProperty("from") final String from) {
        final Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.auth", "true");

        session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, passwd);
            }
        });
        this.from = from;
    }

    @Override
    public void send(final String to, final String subject, final String text) {
        try {
            LOG.info("Sending mail to {}", to);
            final MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(text);
            Transport.send(message);
        } catch (final MessagingException e) {
            throw new RuntimeException("Unable to send mail", e);
        }
    }

}

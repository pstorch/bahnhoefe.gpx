package org.railwaystations.api.mail;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Properties;

public class SmtpMailer implements Mailer {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpMailer.class);

    private final String from;

    private final Session session;

    public SmtpMailer(@JsonProperty("host") final String host,
                      @JsonProperty("port") final String port,
                      @JsonProperty("user") final String user,
                      @JsonProperty("passwd") final String passwd,
                      @JsonProperty("from") final String from) {
        final Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.port", port);
        properties.setProperty("mail.smtp.starttls.enable", "true");

        session = Session.getInstance(properties, new UsernamePasswordAuthenticator(user, passwd));
        this.from = from;
    }

    @Override
    public void send(final String to, final String subject, final String text, final File qrCode) {
        try {
            LOG.info("Sending mail to {}", to);
            final MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);

            final Multipart multipart = new MimeMultipart();

            final MimeBodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setText(text);
            multipart.addBodyPart(textBodyPart);

            final MimeBodyPart messageBodyPart = new MimeBodyPart();
            final String fileName = "qr_code.png";
            final DataSource source = new FileDataSource(qrCode);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(fileName);
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);
            Transport.send(message);
        } catch (final MessagingException e) {
            throw new RuntimeException("Unable to send mail", e);
        }
    }

    private static class UsernamePasswordAuthenticator extends Authenticator {
        private final String user;
        private final String passwd;

        private UsernamePasswordAuthenticator(final String user, final String passwd) {
            this.user = user;
            this.passwd = passwd;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, passwd);
        }
    }
}

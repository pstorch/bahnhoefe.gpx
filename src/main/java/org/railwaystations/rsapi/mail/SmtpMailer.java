package org.railwaystations.rsapi.mail;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

public class SmtpMailer implements Mailer {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpMailer.class);

    private String host;
    private String port;
    private String user;
    private String passwd;
    private String from;
    private Session session;

    @PostConstruct
    public void init() {
        if (StringUtils.isNoneBlank(host)) {
            final Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", host);
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.port", port);
            properties.setProperty("mail.smtp.starttls.enable", "true");

            session = Session.getInstance(properties, new UsernamePasswordAuthenticator(user, passwd));
        }
    }

    @Override
    public void send(final String to, final String subject, final String text) {
        if (session == null) {
            LOG.info("Mailer not initialized, can't send mail to {} with subject {} and body {}", to, subject, text);
            return;
        }
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

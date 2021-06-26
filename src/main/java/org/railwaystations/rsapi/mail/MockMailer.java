package org.railwaystations.rsapi.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockMailer implements Mailer {

    private static final Logger LOG = LoggerFactory.getLogger(MockMailer.class);

    private String to;

    private String subject;

    private String text;

    @Override
    public void send(final String to, final String subject, final String text) {
        LOG.info("Sending mail to {} with subject {}\n{}", to, subject, text);
        this.to = to;
        this.subject = subject;
        this.text = text;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

}

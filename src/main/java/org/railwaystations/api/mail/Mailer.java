package org.railwaystations.api.mail;

public interface Mailer {

    void send(final String to, final String subject, final String text);

}

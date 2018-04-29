package org.railwaystations.api.mail;

import java.io.File;

public interface Mailer {

    void send(final String to, final String subject, final String text, final File qrCode);

}

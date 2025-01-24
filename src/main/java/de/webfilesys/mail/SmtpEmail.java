package de.webfilesys.mail;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import de.webfilesys.ArcoirisBlog;

public class SmtpEmail extends Thread {

    private static final String CONTENT_TYPE_PLAINTEXT = "text/plain; charset=ISO-8859-1";

    private ArrayList<String> receiverList = null;

    private String subject = null;

    private String messageText = null;

    private File attachmentFile = null;

    private String mailSenderName = null;

    private String mailSenderAddress = null;

    public SmtpEmail(ArrayList<String> receiverList, String subject, String msgText) {
        this.messageText = msgText;
        this.receiverList = receiverList;
        this.subject = subject;
    }

    public SmtpEmail(String receiver, String subject, String msgText) {
        receiverList = new ArrayList<String>();
        receiverList.add(receiver);
        this.subject = subject;
        this.messageText = msgText;
    }

    public SmtpEmail(String receiver, String subject, File attachment) {
        receiverList = new ArrayList<String>();
        receiverList.add(receiver);
        this.subject = subject;
        this.attachmentFile = attachment;
    }

    public SmtpEmail(String receiver, String subject, File attachment, String msgText) {
        this.messageText = msgText;
        receiverList = new ArrayList<String>();
        receiverList.add(receiver);
        this.subject = subject;
        this.attachmentFile = attachment;
    }

    public SmtpEmail(ArrayList<String> receiverList, String subject, File attachment) {
        this.receiverList = receiverList;
        this.subject = subject;
        this.attachmentFile = attachment;
    }

    public void setMailSenderName(String from) {
        mailSenderName = from;
    }

    public void setMailSenderAddress(String from) {
        mailSenderAddress = from;
    }

    private String getMailSenderAddress() {
        if (mailSenderAddress != null) {
            return mailSenderAddress;
        }
        return ArcoirisBlog.getInstance().getMailSenderAddress();
    }

    private String getMailSenderName() {
        if (mailSenderName != null) {
            return mailSenderName;
        }
        return ArcoirisBlog.getInstance().getMailSenderName();
    }

    public boolean sendSynchron() {
        return sendInternal();
    }

    public void send() {
        this.start();
    }

    public synchronized void run() {
        sendInternal();
    }

    public boolean sendInternal() {

        Session mailSession = ArcoirisBlog.getInstance().getMailSession();

        try {
            Message msg = new MimeMessage(mailSession);

            msg.setSentDate(new Date());

            msg.setFrom(new InternetAddress(getMailSenderAddress(), getMailSenderName()));

            for (String receiver : receiverList) {
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
            }

            msg.setSubject(subject);

            if (attachmentFile == null) {
                if (messageText != null) {
                    msg.setContent(messageText, CONTENT_TYPE_PLAINTEXT);
                }
            } else {
                Multipart multipart = new MimeMultipart();

                if (messageText != null) {
                    MimeBodyPart textBodyPart = new MimeBodyPart();
                    textBodyPart.setText(messageText);
                    multipart.addBodyPart(textBodyPart);
                }

                MimeBodyPart attachBodyPart = new MimeBodyPart();

                DataSource source = new FileDataSource(attachmentFile);

                attachBodyPart.setDataHandler(new DataHandler(source));

                attachBodyPart.setFileName(attachmentFile.getName());

                multipart.addBodyPart(attachBodyPart);

                msg.setContent(multipart);
            }

            // msg.setHeader("X-Mailer", mailer);

            msg.saveChanges();

            Transport tr = mailSession.getTransport();

            tr.connect(ArcoirisBlog.getInstance().getMailHost(), ArcoirisBlog.getInstance().getSmtpUser(), ArcoirisBlog.getInstance().getSmtpPassword());

            tr.sendMessage(msg, msg.getAllRecipients());

            tr.close();

            for (String receiver : receiverList) {
                Logger.getLogger(getClass()).info("e-mail sent to " + receiver);
            }

            return true;

        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("failed to send mail", ex);
            return false;
        }
    }

}
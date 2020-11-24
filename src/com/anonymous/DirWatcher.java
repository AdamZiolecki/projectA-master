package com.anonymous;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Created by Adam on 10.04.2018.
 */
public class DirWatcher implements Runnable{

    private Thread t;
    private String threadName = "Inuidisse Thread 1";
    private String path;

    DirWatcher(String path)
    {
        this.path = path;
    }

    @Override
    public void run() {
        watch();
    }

    void watch()
    {
        try {
            if (!Pattern.matches(".*files", path))
            {
                path += "\\files";
            }
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(path);
            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            System.out.println("Watch Service registered for dir: " + dir);

            while (true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }

                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread sleep error");
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    if (kind == ENTRY_CREATE) {
                        if (Pattern.matches("^InuAlert.*", fileName.toString())) {
                            System.out.println(kind.name() + ": " + fileName);
                            readFile(path + "\\" + fileName);
                        }
                    }
                    System.out.println(kind.name() + ": " + fileName);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            System.err.println(ex);
            return;
        }
    }

    public void start () {
        System.out.println("Starting " + threadName);
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }

    public void stop() {
        t.interrupt();
    }

    private void readFile(String filename) {
        String mailReceiver;
        String mailSubject;
        String mailText;

        try {
            System.out.println(filename);
            Scanner file = new Scanner(new FileReader(filename));
            StringBuilder sb = new StringBuilder();
            mailReceiver = file.nextLine();
            mailSubject = file.nextLine();
            while(file.hasNextLine()) {
                sb.append(file.nextLine());
                sb.append("\n");
            }
            file.close();
            mailText = sb.toString();
            sendMail(mailReceiver, mailSubject, mailText);
            Files.delete(Paths.get(filename));
        }
        catch (IOException e){
            System.out.println("Error due read file");
        }
    }

    private void sendMail(String receiver, String subject, String text) {
        MailSender mailSender = new MailSender();
        /***********************************************************************
         * Fill email address and password
         * mailSender.init(String receciver, String password)
         **********************************************************************/
        mailSender.init("bot9051@gmail.com", "bhuijn8@");
        mailSender.sendMessage(receiver, subject, text);
    }

}

package de.marmaro.krt.ffupdater.utils;

import android.content.Context;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Dead simple crash reporter for simplifying bug hunting with the end users.
 */
public class CrashReporter {
    public static final String SUBJECT = "FFUpdater crashed";
    public static final String MESSAGE = "I'm sorry for this crude way to display the error which crashed FFUpdater.\n";
    private final Context context;

    private CrashReporter(Context context) {
        this.context = context;
    }

    /**
     * Register a DefaultUncaughtExceptionHandler to get the message of an uncaught exception and
     * displaying it as an e-mail.
     *
     * @param context context
     */
    public static void register(Context context) {
        new CrashReporter(context).registerExceptionHandler();
    }

    private void registerExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((Thread thread, Throwable e) -> {
            sendStacktraceAsMail(e);
            System.exit(2);
        });
    }

    private void sendStacktraceAsMail(Throwable throwable) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, SUBJECT);

        StringWriter sw = new StringWriter();
        sw.write(MESSAGE);
        sw.write("Can you please send me this error message as an 'issue' on https://notabug.org/Tobiwan/ffupdater/issues?\n");
        sw.write("\n\n");

        throwable.printStackTrace(new PrintWriter(sw));

        intent.putExtra(Intent.EXTRA_TEXT, sw.toString());
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }
}

package de.marmaro.krt.ffupdater.utils;

import android.content.Context;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashReporter {

    private Context context;

    private CrashReporter(Context context) {
        this.context = context;
    }

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
        intent.putExtra(Intent.EXTRA_SUBJECT, "FFUpdater crashed with this stacktrace");

        StringWriter sw = new StringWriter();
        sw.write("I'm sorry for this very crude way to display the exception which crashed FFUpdater.\n");
        sw.write("Can you please send me this error message as an 'issue' on https://notabug.org/Tobiwan/ffupdater/issues?\n");
        sw.write("\n\n");

        throwable.printStackTrace(new PrintWriter(sw));

        intent.putExtra(Intent.EXTRA_TEXT, sw.toString());
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }
}

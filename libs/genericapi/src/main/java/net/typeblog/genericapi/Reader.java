package net.typeblog.genericapi;

import androidx.annotation.NonNull;

import java.io.IOException;

public interface Reader {
    @NonNull
    String getName();

    boolean isSecureElementPresent();

    @NonNull
    Session openSession() throws IOException;

    void closeSessions();
}

package net.typeblog.genericapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

public interface Channel extends java.nio.channels.Channel {
    boolean isBasicChannel();

    @Nullable
    byte[] getSelectResponse();

    @NonNull
    Session getSession();

    byte[] transmit(byte[] command) throws IOException;

    boolean selectNext();
}

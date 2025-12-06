package net.typeblog.genericapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;

public interface Session extends Closeable {
    @NonNull
    Reader getReader();

    @Nullable
    default byte[] getATR() {
        return null;
    }

    default void close() {
        closeChannels();
    }

    boolean isClosed();

    void closeChannels();

    @Nullable
    Channel openBasicChannel(byte[] aid, byte p2) throws IOException;

    @Nullable
    default Channel openBasicChannel(byte[] aid) throws IOException {
        return openBasicChannel(aid, (byte) 0x00);
    }

    @Nullable
    Channel openLogicalChannel(byte[] aid, byte p2) throws IOException;

    @Nullable
    default Channel openLogicalChannel(byte[] aid) throws IOException {
        return openLogicalChannel(aid, (byte) 0x00);
    }
}

package org.example;

import java.io.InputStream;
import java.io.IOException;

public class LimitedInputStream extends InputStream {
    private final InputStream in;
    private int remaining;

    public LimitedInputStream(InputStream in, int limit) {
        this.in = in;
        this.remaining = limit;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        int b = in.read();
        if (b != -1) {
            remaining--;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) {
            return -1;
        }

        int toRead = Math.min(len, remaining);
        int count = in.read(b, off, toRead);
        if (count > 0) {
            remaining -= count;
        }
        return count;
    }
}

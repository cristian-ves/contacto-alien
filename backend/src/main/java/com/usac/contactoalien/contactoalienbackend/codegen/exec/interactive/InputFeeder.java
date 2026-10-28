package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
Delivers the text typed by the user to the program's standard input, in order, from its own thread.
The pipe holds only a few KiB, so a program that is not reading would block whoever wrote to it;
queueing keeps the HTTP request fast and the pending amount is bounded.
*/
final class InputFeeder implements Runnable {

    enum Offer { ACCEPTED, CLOSED, FULL }

    private static final byte[] END_OF_INPUT = new byte[0];

    private final OutputStream stdin;
    private final int maxPendingBytes;
    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger pendingBytes = new AtomicInteger();
    private volatile boolean closed;
    private Thread thread;

    InputFeeder(OutputStream stdin, int maxPendingBytes) {
        this.stdin = stdin;
        this.maxPendingBytes = maxPendingBytes;
    }

    void start() {
        thread = Thread.ofVirtual().start(this);
    }

    /**
    Queues text for the program. The text given at start-up is not bounded (bounded = false); what the
    user types later is, so a stuck program cannot make the server hold an unlimited backlog.
    */
    synchronized Offer offer(byte[] bytes, boolean bounded) {
        if (closed) {
            return Offer.CLOSED;
        }
        if (bounded && pendingBytes.get() + bytes.length > maxPendingBytes) {
            return Offer.FULL;
        }
        pendingBytes.addAndGet(bytes.length);
        queue.add(bytes);
        return Offer.ACCEPTED;
    }

    // Closes the program's standard input once everything queued so far has been written.
    synchronized Offer endOfInput() {
        if (closed) {
            return Offer.CLOSED;
        }
        closed = true;
        queue.add(END_OF_INPUT);
        return Offer.ACCEPTED;
    }

    void shutdown() {
        closed = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                byte[] chunk = queue.take();
                if (chunk == END_OF_INPUT) {
                    break;
                }
                stdin.write(chunk);
                stdin.flush();
                pendingBytes.addAndGet(-chunk.length);
            }
        } catch (IOException e) {
            // The program ended without reading everything; the rest is dropped.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            stdin.close();
        } catch (IOException e) {
            // Already closed together with the process.
        }
    }
}

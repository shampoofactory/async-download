package com.github.shampoofactory.asynctest;

/**
 *
 * @author Vin Singh
 */
public final class Split {

    private final int id;
    private final long head;
    private final long tail;

    public Split(int id, long head, long tail) {
        if (tail <= head) {
            throw new IllegalArgumentException();
        }
        this.id = id;
        this.head = head;
        this.tail = tail;
    }

    public Split cut(long head) {
        if (tail < head) {
            throw new IllegalArgumentException();
        }
        return new Split(id, head, tail);
    }

    public int id() {
        return id;
    }

    public long head() {
        return head;
    }

    public long tail() {
        return tail;
    }

    public long length() {
        return tail - head;
    }

    @Override
    public String toString() {
        return "Split{" + "id=" + id + ", head=" + head + ", tail=" + tail + '}';
    }
}

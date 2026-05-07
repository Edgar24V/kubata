package ao.allon.kubata.faturacao.header;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class NotificationModel {

    public static final class Notification {
        private final long id;
        private final String text;
        private boolean read;

        Notification(long id, String text) {
            this.id = id;
            this.text = text;
            this.read = false;
        }

        public long getId() { return id; }
        public String getText() { return text; }
        public boolean isRead() { return read; }
        void markRead() { this.read = true; }
    }

    private final List<Notification> items = new ArrayList<>();
    private final AtomicLong counter = new AtomicLong(1);

    public synchronized Notification add(String text) {
        Notification n = new Notification(counter.getAndIncrement(), text);
        items.add(0, n);
        return n;
    }

    public synchronized void markRead(long id) {
        for (Notification n : items) {
            if (n.id == id) { n.markRead(); break; }
        }
    }

    public synchronized void markAllRead() {
        for (Notification n : items) n.markRead();
    }

    public synchronized int getUnreadCount() {
        int c = 0;
        for (Notification n : items) if (!n.isRead()) c++;
        return c;
    }

    public synchronized List<Notification> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }
}

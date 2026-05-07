package ao.allon.kubata.faturacao.header;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationModelTest {
    @Test
    void addAndCountUnread() {
        NotificationModel m = new NotificationModel();
        m.add("A");
        m.add("B");
        assertEquals(2, m.getUnreadCount());
    }

    @Test
    void markReadAndMarkAll() {
        NotificationModel m = new NotificationModel();
        NotificationModel.Notification n1 = m.add("N1");
        NotificationModel.Notification n2 = m.add("N2");
        m.markRead(n1.getId());
        assertEquals(1, m.getUnreadCount());
        m.markAllRead();
        assertEquals(0, m.getUnreadCount());
    }
}

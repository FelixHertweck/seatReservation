/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.felixhertweck.seatreservation.email;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.email.queue.EmailAttachment;
import de.felixhertweck.seatreservation.email.service.notifications.BoxOfficeConfirmationNotification;
import de.felixhertweck.seatreservation.email.service.notifications.EventReminderNotification;
import de.felixhertweck.seatreservation.email.service.notifications.EventReservationsCsvNotification;
import de.felixhertweck.seatreservation.email.service.notifications.PasswordChangedNotification;
import de.felixhertweck.seatreservation.email.service.notifications.ReservationConfirmationNotification;
import de.felixhertweck.seatreservation.email.service.notifications.ReservationUpdateNotification;
import de.felixhertweck.seatreservation.email.service.notifications.TwoFactorCodeNotification;
import de.felixhertweck.seatreservation.email.service.notifications.UsernameRecoveryNotification;
import de.felixhertweck.seatreservation.model.entity.EmailPriority;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import org.junit.jupiter.api.Test;

public class EmailNotificationsTest {

    private Template mockTemplate() {
        Template template = mock(Template.class);
        TemplateInstance instance = mock(TemplateInstance.class);
        when(template.data(any(), any())).thenReturn(instance);
        when(instance.data(any(), any())).thenReturn(instance);
        when(instance.render()).thenReturn("<html>Rendered Content</html>");
        return template;
    }

    @Test
    void testPasswordChangedNotification() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setFirstname("John");
        user.setLastname("Doe");

        PasswordChangedNotification notification =
                new PasswordChangedNotification(user, "Password Changed", mockTemplate());

        assertEquals(List.of("user@example.com"), notification.recipients());
        assertEquals("Password Changed", notification.subject());
        assertEquals("<html>Rendered Content</html>", notification.renderHtml());
        assertTrue(notification.attachments().isEmpty());
    }

    @Test
    void testUsernameRecoveryNotification() {
        UsernameRecoveryNotification notification =
                new UsernameRecoveryNotification(
                        "recover@example.com",
                        List.of("user1", "user2"),
                        "Username Recovery",
                        mockTemplate());

        assertEquals(List.of("recover@example.com"), notification.recipients());
        assertEquals("Username Recovery", notification.subject());
        assertEquals("<html>Rendered Content</html>", notification.renderHtml());
    }

    @Test
    void testTwoFactorCodeNotification() {
        User user = new User();
        user.setEmail("2fa@example.com");
        user.setUsername("tfuser");

        TwoFactorCodeNotification notification =
                new TwoFactorCodeNotification(user, "123456", "2FA Code", mockTemplate());

        assertEquals(List.of("2fa@example.com"), notification.recipients());
        assertEquals("2FA Code", notification.subject());
        assertEquals("<html>Rendered Content</html>", notification.renderHtml());
    }

    @Test
    void testReservationConfirmationNotification() {
        User user = new User();
        user.setEmail("user@example.com");

        byte[] seatmap = new byte[] {1, 2, 3};
        byte[] qrCode = new byte[] {4, 5, 6};

        ReservationConfirmationNotification notification =
                new ReservationConfirmationNotification(
                        user,
                        "extra@example.com",
                        "Confirmation",
                        "<html>Body</html>",
                        seatmap,
                        qrCode);

        assertEquals(List.of("user@example.com", "extra@example.com"), notification.recipients());
        assertEquals("Confirmation", notification.subject());
        assertEquals("<html>Body</html>", notification.renderHtml());
        assertTrue(notification.includeBcc());

        List<EmailAttachment> attachments = notification.attachments();
        assertEquals(2, attachments.size());
        assertEquals("seatmap-image", attachments.get(0).contentId());
        assertEquals("qrcode-image", attachments.get(1).contentId());
    }

    @Test
    void testBoxOfficeConfirmationNotification() {
        User user = new User();
        user.setEmail("user@example.com");

        byte[] qrCode = new byte[] {4, 5, 6};

        BoxOfficeConfirmationNotification notification =
                new BoxOfficeConfirmationNotification(
                        user,
                        "extra@example.com",
                        "Box Office Confirmation",
                        "<html>Body</html>",
                        qrCode);

        assertEquals(List.of("user@example.com", "extra@example.com"), notification.recipients());
        assertEquals("Box Office Confirmation", notification.subject());
        assertEquals("<html>Body</html>", notification.renderHtml());
        assertTrue(notification.includeBcc());

        List<EmailAttachment> attachments = notification.attachments();
        assertEquals(1, attachments.size());
        assertEquals("qrcode-image", attachments.get(0).contentId());
    }

    @Test
    void testReservationUpdateNotification() {
        User user = new User();
        user.setEmail("user@example.com");

        ReservationUpdateNotification notification =
                new ReservationUpdateNotification(
                        user, null, "Update", "<html>Updated</html>", new byte[0], new byte[0]);

        assertEquals(List.of("user@example.com"), notification.recipients());
        assertEquals("Update", notification.subject());
        assertEquals("<html>Updated</html>", notification.renderHtml());
        assertTrue(notification.attachments().isEmpty());
    }

    @Test
    void testEventReminderNotification() {
        User user = new User();
        user.setEmail("reminder@example.com");

        EventReminderNotification notification =
                new EventReminderNotification(
                        user, "Reminder", "<html>Reminder</html>", null, null);

        assertEquals(List.of("reminder@example.com"), notification.recipients());
        assertEquals("Reminder", notification.subject());
        assertEquals(EmailPriority.BULK, notification.priority());
        assertTrue(notification.attachments().isEmpty());
    }

    @Test
    void testEventReservationsCsvNotification() {
        User manager = new User();
        manager.setEmail("manager@example.com");

        EmailAttachment csv = EmailAttachment.file("test.csv", "text/csv", new byte[] {10, 20});
        EventReservationsCsvNotification notification =
                new EventReservationsCsvNotification(
                        manager, "CSV Export", "<html>CSV</html>", csv);

        assertEquals(List.of("manager@example.com"), notification.recipients());
        assertEquals("CSV Export", notification.subject());
        assertEquals(EmailPriority.BULK, notification.priority());
        assertEquals(1, notification.attachments().size());
        assertEquals("test.csv", notification.attachments().get(0).fileName());
    }
}

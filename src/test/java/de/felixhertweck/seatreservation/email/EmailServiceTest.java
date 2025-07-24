/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailVerificationRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock Mailer mailer;

    @Mock EmailVerificationRepository emailVerificationRepository;

    @InjectMocks EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService.baseUrl = "http://localhost:8080";
        emailService.expirationMinutes = 60;
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstname("Test");
        user.setLastname("User");
        return user;
    }

    @Test
    void sendEmailConfirmation_Success() throws IOException {
        User user = createTestUser();
        doNothing().when(mailer).send(any(Mail.class));

        emailService.sendEmailConfirmation(user);

        verify(emailVerificationRepository, times(1))
                .persist(
                        any(de.felixhertweck.seatreservation.model.entity.EmailVerification.class));
        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailer, times(1)).send(mailCaptor.capture());

        Mail sentMail = mailCaptor.getValue();
        assertEquals(user.getEmail(), sentMail.getTo().getFirst());
        assertEquals("Please Confirm Your Email Address", sentMail.getSubject());
        assertTrue(sentMail.getHtml().contains("http://localhost:8080/api/user/confirm-email"));
    }
}

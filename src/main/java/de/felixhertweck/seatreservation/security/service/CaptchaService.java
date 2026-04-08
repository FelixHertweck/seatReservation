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
package de.felixhertweck.seatreservation.security.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.security.dto.CaptchaVerifyRequestDTO;
import de.felixhertweck.seatreservation.security.dto.CaptchaVerifyResponseDTO;
import de.felixhertweck.seatreservation.security.exceptions.CaptchaValidationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CaptchaService {

    private static final Logger LOG = Logger.getLogger(CaptchaService.class);

    @ConfigProperty(name = "captcha.site-key")
    String siteKey;

    @ConfigProperty(name = "captcha.secret-key")
    String secretKey;

    public void verifyCaptcha(String captchaToken) {
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            throw new CaptchaValidationException("Captcha token is missing or empty.");
        }

        if (siteKey == null
                || siteKey.trim().isEmpty()
                || secretKey == null
                || secretKey.trim().isEmpty()) {
            LOG.warn(
                    "Captcha is not configured (missing site-key or secret-key). Skipping"
                            + " verification.");
            return;
        }

        String capUrl = "http://cap:3000/" + siteKey + "/siteverify";

        try (Client client = ClientBuilder.newClient()) {
            CaptchaVerifyRequestDTO requestBody =
                    new CaptchaVerifyRequestDTO(secretKey, captchaToken);

            Response response =
                    client.target(capUrl)
                            .request(MediaType.APPLICATION_JSON)
                            .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON));

            if (response.getStatus() == 200) {
                CaptchaVerifyResponseDTO verifyResponse =
                        response.readEntity(CaptchaVerifyResponseDTO.class);
                if (!verifyResponse.isSuccess()) {
                    throw new CaptchaValidationException(
                            "Captcha verification failed. Please try again.");
                }
            } else {
                LOG.error("Captcha verification endpoint returned status: " + response.getStatus());
                throw new CaptchaValidationException(
                        "Captcha verification error. Please try again later.");
            }
        } catch (Exception e) {
            LOG.error("Error during captcha verification: " + e.getMessage());
            if (e instanceof CaptchaValidationException) {
                throw e;
            }
            throw new CaptchaValidationException(
                    "Captcha verification error. Please try again later.");
        }
    }
}

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
package de.felixhertweck.seatreservation.testutil;

import java.util.UUID;

/**
 * Deterministically maps small integers to fixed UUIDs, so existing tests can keep using short,
 * readable fixture ids (1, 2, 3, ...) now that entity primary keys are {@link UUID}s instead of
 * sequential {@code Long}s. Distinct integers always map to distinct, stable UUIDs.
 */
public final class TestIds {

    private TestIds() {}

    public static UUID id(long n) {
        return new UUID(0L, n);
    }
}

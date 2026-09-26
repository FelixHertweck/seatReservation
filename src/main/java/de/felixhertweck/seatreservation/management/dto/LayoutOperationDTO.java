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
package de.felixhertweck.seatreservation.management.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * One step of a {@link LayoutBatchRequestDTO}. Subclasses wrap the regular request DTO of their
 * entity, so no second set of field definitions has to be maintained. The discriminator property is
 * {@code entity}.
 *
 * <ul>
 *   <li>{@code CREATE}: {@code data} required, {@code ref} optional. Later operations of the same
 *       batch can point at the created entity via that ref.
 *   <li>{@code UPDATE}: {@code id} and {@code data} required.
 *   <li>{@code DELETE}: {@code id} required.
 * </ul>
 */
@RegisterForReflection
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "entity")
@JsonSubTypes({
    @JsonSubTypes.Type(value = LocationOperationDTO.class, name = "LOCATION"),
    @JsonSubTypes.Type(value = EntranceOperationDTO.class, name = "ENTRANCE"),
    @JsonSubTypes.Type(value = AreaOperationDTO.class, name = "AREA"),
    @JsonSubTypes.Type(value = MarkerOperationDTO.class, name = "MARKER"),
    @JsonSubTypes.Type(value = SeatOperationDTO.class, name = "SEAT")
})
@Schema(
        type = SchemaType.OBJECT,
        oneOf = {
            LocationOperationDTO.class,
            EntranceOperationDTO.class,
            AreaOperationDTO.class,
            MarkerOperationDTO.class,
            SeatOperationDTO.class
        },
        discriminatorProperty = "entity",
        discriminatorMapping = {
            @DiscriminatorMapping(value = "LOCATION", schema = LocationOperationDTO.class),
            @DiscriminatorMapping(value = "ENTRANCE", schema = EntranceOperationDTO.class),
            @DiscriminatorMapping(value = "AREA", schema = AreaOperationDTO.class),
            @DiscriminatorMapping(value = "MARKER", schema = MarkerOperationDTO.class),
            @DiscriminatorMapping(value = "SEAT", schema = SeatOperationDTO.class)
        })
public abstract class LayoutOperationDTO {

    @NotNull(message = "Operation action must not be null")
    private LayoutOperationAction action;

    /** Target of an UPDATE or DELETE. */
    private UUID id;

    /** Client-chosen key of a CREATE, unique per entity type within one batch. */
    private String ref;

    protected LayoutOperationDTO() {}

    /** Discriminator, fixed per subclass. Read-only: on input it only selects the subclass. */
    @Schema(required = true)
    public abstract LayoutEntityType getEntity();

    public LayoutOperationAction getAction() {
        return action;
    }

    public void setAction(LayoutOperationAction action) {
        this.action = action;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }
}

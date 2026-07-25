cat << 'PATCH' > src/test/java/de/felixhertweck/seatreservation/management/service/EventLocationServiceTest.java.patch
--- src/test/java/de/felixhertweck/seatreservation/management/service/EventLocationServiceTest.java
+++ src/test/java/de/felixhertweck/seatreservation/management/service/EventLocationServiceTest.java
@@ -58,6 +58,7 @@
 import de.felixhertweck.seatreservation.model.repository.SeatRepository;
 import de.felixhertweck.seatreservation.model.repository.UserRepository;
 import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
+import io.quarkus.hibernate.orm.panache.PanacheQuery;
 import io.quarkus.test.InjectMock;
 import io.quarkus.test.junit.QuarkusTest;
 import org.junit.jupiter.api.BeforeEach;
@@ -312,8 +313,9 @@

     @Test
     void deleteEventLocation_Success_AsManager() {
-        when(eventLocationRepository.findByIdOptional(id(1)))
-                .thenReturn(Optional.of(existingLocation));
+        PanacheQuery<EventLocation> query = Mockito.mock(PanacheQuery.class);
+        when(eventLocationRepository.find(Mockito.anyString(), Mockito.any(List.class))).thenReturn(query);
+        when(query.list()).thenReturn(List.of(existingLocation));
         doNothing().when(eventLocationRepository).delete(any(EventLocation.class));

         eventLocationService.deleteEventLocation(List.of(id(1)), managerAuth);
@@ -323,8 +325,9 @@

     @Test
     void deleteEventLocation_NotFound() {
-        when(eventLocationRepository.findByIdOptional(any(UUID.class)))
-                .thenReturn(Optional.empty());
+        PanacheQuery<EventLocation> query = Mockito.mock(PanacheQuery.class);
+        when(eventLocationRepository.find(Mockito.anyString(), Mockito.any(List.class))).thenReturn(query);
+        when(query.list()).thenReturn(List.of());

         assertThrows(
                 EventLocationNotFoundException.class,
@@ -334,8 +337,9 @@

     @Test
     void deleteEventLocation_Success_AsAdmin() {
-        when(eventLocationRepository.findByIdOptional(id(1)))
-                .thenReturn(Optional.of(existingLocation));
+        PanacheQuery<EventLocation> query = Mockito.mock(PanacheQuery.class);
+        when(eventLocationRepository.find(Mockito.anyString(), Mockito.any(List.class))).thenReturn(query);
+        when(query.list()).thenReturn(List.of(existingLocation));
         doNothing().when(eventLocationRepository).delete(any(EventLocation.class));

         eventLocationService.deleteEventLocation(List.of(id(1)), adminAuth);
@@ -345,8 +349,9 @@

     @Test
     void deleteEventLocation_ForbiddenException_NotManagerOrAdmin() {
-        when(eventLocationRepository.findByIdOptional(id(1)))
-                .thenReturn(Optional.of(existingLocation));
+        PanacheQuery<EventLocation> query = Mockito.mock(PanacheQuery.class);
+        when(eventLocationRepository.find(Mockito.anyString(), Mockito.any(List.class))).thenReturn(query);
+        when(query.list()).thenReturn(List.of(existingLocation));

         assertThrows(
                 SecurityException.class,
PATCH
patch src/test/java/de/felixhertweck/seatreservation/management/service/EventLocationServiceTest.java src/test/java/de/felixhertweck/seatreservation/management/service/EventLocationServiceTest.java.patch

import re

def rewrite(filepath, old, new):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# AuthResource.java:100
# User user = authService.authenticate(user.id, loginRequest.getPassword());
# user.id should be loginRequest.getUsername()
rewrite('src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java', 'User user = authService.authenticate(user.id, loginRequest.getPassword());', 'User user = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());')

# LiveViewResource.java:60
# webSocketService.registerConnection(eventIdStr, connection, currentUser.id);
# .id is Long, but the method probably expects a String username
rewrite('src/main/java/de/felixhertweck/seatreservation/supervisor/resource/LiveViewResource.java', 'webSocketService.registerConnection(eventIdStr, connection, currentUser.id);', 'webSocketService.registerConnection(eventIdStr, connection, currentUser.getUsername());')

# LiveViewResource.java:78
# webSocketService.unregisterConnection(eventIdStr, connection, currentUser.id);
rewrite('src/main/java/de/felixhertweck/seatreservation/supervisor/resource/LiveViewResource.java', 'webSocketService.unregisterConnection(eventIdStr, connection, currentUser.id);', 'webSocketService.unregisterConnection(eventIdStr, connection, currentUser.getUsername());')

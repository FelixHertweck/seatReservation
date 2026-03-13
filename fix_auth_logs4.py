import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('loginRequest.getUsername());', 'user.id);\n        LOG.infof("User ID: %d logged in successfully.", user.id);', 1)
content = content.replace('registerRequest.getUsername());', 'user.id);\n        LOG.infof("User ID: %d registered successfully.", user.id);', 1)

# Fix logout
content = content.replace('LOG.debugf("User logged out successfully. JWT and refresh token cookies cleared.");', 'LOG.infof("User ID: %d logged out successfully.", currentUser.id);\n        LOG.debugf("User ID: %d logged out successfully. JWT and refresh token cookies cleared.", currentUser.id);')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

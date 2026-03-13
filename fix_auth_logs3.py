import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix compilation errors from previous replacements
content = content.replace('loginRequest.getUsername()', 'user.id', 1) # First occurrence is where it logs user ID: %d logged in successfully
content = content.replace('registerRequest.getUsername()', 'user.id', 1)

# Ensure "username: [HIDDEN]", "HIDDEN" is removed since there are no %s
content = re.sub(r'username:\s*\[HIDDEN\]",\s*"HIDDEN"\)', 'username: [HIDDEN]")', content)

# Add info level log for login, register, logout
content = content.replace('LOG.debugf("User logged out successfully', 'LOG.infof("User ID: %d logged out successfully.", currentUser.id);\n        LOG.debugf("User ID: %d logged out successfully', 1)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

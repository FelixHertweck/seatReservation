import re

def rewrite(filepath, pattern, replacement):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = re.sub(pattern, replacement, content)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# AuthResource.java:100 LOG.debugf("user ID: %d registered successfully. JWT and refresh token cookies set.", user.id);
# Oh wait, does user.id not exist here or it's passed as a String to %s?
# wait, %s needs string, %d needs integer. In line 100 it says `user: [HIDDEN] registered successfully.", "HIDDEN"`?
# Ah, I see from `git diff` that maybe it wasn't replaced successfully earlier.

rewrite('src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java', r'user ID: %d registered successfully\.",\s*user\.id', r'user ID: %d registered successfully.", user.id')
rewrite('src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java', r'user ID: %d registered successfully\.",\s*registerRequest\.getUsername\(\)', r'user: [HIDDEN] registered successfully.", "HIDDEN"')

# LiveViewResource.java:60, 78
# LOG.debugf("user ID: %d processed successfully.", username);
rewrite('src/main/java/de/felixhertweck/seatreservation/supervisor/resource/LiveViewResource.java', r'user ID: %d\.",\s*username\)', r'user: [HIDDEN].", "HIDDEN")')
rewrite('src/main/java/de/felixhertweck/seatreservation/supervisor/resource/LiveViewResource.java', r'user ID: %d processed successfully\.",\s*username\)', r'user: [HIDDEN] processed successfully.", "HIDDEN")')

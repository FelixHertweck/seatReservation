import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# I see "user ID: %d logged in successfully.", loginRequest.id which is wrong because loginRequest.id is the username string in the payload. We need the user.id
content = re.sub(r'user ID: %d logged in successfully\.",\s*loginRequest\.id\);', r'user ID: %d logged in successfully.", user.id);\n        LOG.infof("User ID: %d logged in successfully.", user.id);', content)

# I see "user ID: %d registered successfully.", registerRequest.id which is also wrong.
content = re.sub(r'user ID: %d registered successfully\.",\s*registerRequest\.id\);', r'user ID: %d registered successfully.", user.id);\n        LOG.infof("User ID: %d registered successfully.", user.id);', content)

# I see username string accessed as loginRequest.id which is incorrect, it was replaced earlier.
content = content.replace('loginRequest.id', 'loginRequest.getUsername()')
content = content.replace('registerRequest.id', 'registerRequest.getUsername()')

# But we shouldn't log username if it's an email or name in debug if we want privacy, though the regex broke `loginRequest.getUsername()` into `loginRequest.id`
# Let's fix that.
content = re.sub(r'username:\s*%s",\s*loginRequest\.getUsername\(\)', r'username: [HIDDEN]", "HIDDEN"', content)
content = re.sub(r'username:\s*%s",\s*registerRequest\.getUsername\(\)', r'username: [HIDDEN]", "HIDDEN"', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

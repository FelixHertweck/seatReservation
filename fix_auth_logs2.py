import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Let's fix the broken parts
content = re.sub(r'username:\s*%s",\s*loginRequest\.id', r'username: [HIDDEN]"', content)
content = re.sub(r'username:\s*%s",\s*registerRequest\.id', r'username: [HIDDEN]"', content)

content = re.sub(r'user ID: %d logged in successfully\.",\s*loginRequest\.id', r'user ID: %d logged in successfully.", user.id', content)
content = re.sub(r'user ID: %d registered successfully\.",\s*registerRequest\.id', r'user ID: %d registered successfully.", user.id', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

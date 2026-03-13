import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/userManagment/service/UserService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# I see `LOG.infof("User profile for username %s updated successfully.", username);`
# It might not have matched if it has formatting. Let's use regex
content = re.sub(r'LOG\.infof\("User profile for username %s updated successfully\.",\s*username\);', r'LOG.infof("User profile for user ID: %d updated successfully.", existingUser.id);', content)

# I see `LOG.infof("User created successfully: %s", user.id);`
content = re.sub(r'LOG\.infof\("User created successfully:\s*%s",\s*user\.id\);', r'LOG.infof("User created successfully. ID: %d", user.id);', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

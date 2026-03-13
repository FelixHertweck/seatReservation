import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/userManagment/service/UserService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# I see `LOG.infof("User profile for username %s updated successfully.", username);` which logs the username.
# Username might be an email. We need to avoid that.
# Let's see if we can use existingUser.id instead.
content = content.replace(
    'LOG.infof("User profile for username %s updated successfully.", username);',
    'LOG.infof("User profile for user ID: %d updated successfully.", existingUser.id);'
)

# And `LOG.infof("User created successfully: %s", user.id);` which is %s but using user.id
content = content.replace(
    'LOG.infof("User created successfully: %s", user.id);',
    'LOG.infof("User created successfully. ID: %d", user.id);'
)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

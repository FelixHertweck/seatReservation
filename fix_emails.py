import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('LOG.debugf("User ID: %d, Username ID: %d", user.id, user.id);', 'LOG.debugf("User ID: %d", user.id);')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

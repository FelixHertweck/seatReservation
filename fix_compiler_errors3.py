import re

def fix(filepath, old, new):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# EmailService
fix('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', 'skipForNullOrEmptyAddress(user.id)', 'skipForNullOrEmptyAddress(user.getEmail())')
fix('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', 'skipForNullOrEmptyAddress(currentUser.id)', 'skipForNullOrEmptyAddress(currentUser.getEmail())')
fix('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', 'replace("{{userId}}", user.id)', 'replace("{{userId}}", String.valueOf(user.id))')
fix('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', 'replace("{{userId}}", currentUser.id)', 'replace("{{userId}}", String.valueOf(currentUser.id))')
fix('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', 'replace("{{eventId}}", event.id)', 'replace("{{eventId}}", String.valueOf(event.id))')
fix('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', 'skipForLocalhostAddress(user.id)', 'skipForLocalhostAddress(user.getEmail())')
fix('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', 'skipForLocalhostAddress(currentUser.id)', 'skipForLocalhostAddress(currentUser.getEmail())')

# EventLocationResource, EventResource, EventService, EventLocationService
# LOG.debugf("user ID: %d found. Retrieving event allowances.", username); -> Should be username (which is a string, and probably hidden now)
# Let's check what it is

import re

# We need to add INFO logs for important CRUD operations where missing.
# Let's check Management services:
# SeatService, EventService, EventLocationService, EventReservationAllowanceService

filepath = 'src/main/java/de/felixhertweck/seatreservation/management/service/EventService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# I see `LOG.debugf("Event %s (ID: %d) updated successfully.", existingEvent.getName(), existingEvent.id);`
# It should be INFO for Create, Update, Delete.
content = re.sub(r'LOG\.debugf\(\s*"Event ID: %d updated successfully\.",\s*existingEvent\.id\);', r'LOG.infof("Event ID: %d updated successfully.", existingEvent.id);', content)
content = re.sub(r'LOG\.debugf\(\s*"Event ID: %d created successfully\.",\s*event\.id\);', r'LOG.infof("Event ID: %d created successfully.", event.id);', content)
content = re.sub(r'LOG\.debugf\(\s*"Event ID: %d deleted successfully\.",\s*id\);', r'LOG.infof("Event ID: %d deleted successfully.", id);', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

filepath = 'src/main/java/de/felixhertweck/seatreservation/management/service/EventLocationService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'LOG\.debugf\(\s*"Event location ID: %d updated successfully\.",\s*existingLocation\.id\);', r'LOG.infof("Event location ID: %d updated successfully.", existingLocation.id);', content)
content = re.sub(r'LOG\.debugf\(\s*"Event location ID: %d created successfully\.",\s*location\.id\);', r'LOG.infof("Event location ID: %d created successfully.", location.id);', content)
content = re.sub(r'LOG\.debugf\(\s*"Event location ID: %d deleted successfully\.",\s*id\);', r'LOG.infof("Event location ID: %d deleted successfully.", id);', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

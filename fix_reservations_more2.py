import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/management/service/EventService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# I see it was already logging `LOG.infof("Event '%s' (ID: %d) created successfully.", event.getName(), event.getId());`
# But we need to make sure we don't log names if they are user names. Event names are usually not PII.
# However, for consistency, we changed it to ID earlier? No, the regex was:
# content = re.sub(r'([Ee]vent)\s+%s\s+\(ID:\s*%d\)(.*?)",([\s\S]*?)((?:event|dto)\.getName\(\))\s*,\s*((?:event|dto)\.id)', r'\1 ID: %d\2",\3\5', content)
# But here it used `event.getId()` instead of `event.id`, so it missed it. Let's fix that.
content = re.sub(r"LOG\.infof\(\s*\"Event '%s' \(ID: %d\) created successfully\.\",\s*event\.getName\(\),\s*event\.getId\(\)\);", r'LOG.infof("Event ID: %d created successfully.", event.getId());', content)
content = re.sub(r"LOG\.infof\(\s*\"Event '%s' \(ID: %d\) created successfully by manager:\s*%s\s*\(ID: %d\).*?\",\s*event\.getName\(\),\s*event\.getId\(\),\s*manager\.getUsername\(\),\s*manager\.id\);", r'LOG.infof("Event ID: %d created successfully by manager ID: %d.", event.getId(), manager.id);', content)

# Updates and Deletes:
content = re.sub(r"LOG\.infof\(\s*\"Event '%s' \(ID: %d\) updated successfully\.\",\s*existingEvent\.getName\(\),\s*existingEvent\.getId\(\)\);", r'LOG.infof("Event ID: %d updated successfully.", existingEvent.getId());', content)
content = re.sub(r"LOG\.infof\(\s*\"Event '%s' \(ID: %d\) deleted successfully\.\",\s*existingEvent\.getName\(\),\s*existingEvent\.getId\(\)\);", r'LOG.infof("Event ID: %d deleted successfully.", existingEvent.getId());', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

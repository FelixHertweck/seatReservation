import re

def rewrite(filepath, pattern, replacement):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = re.sub(pattern, replacement, content)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

def replace(filepath, old, new):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# ReservationService.java:[135,12] reference to debugf is ambiguous
# LOG.debugf("Attempting to create reservation for user ID: %d for event ID %d with %d seats.", currentUser.id, dto.getEventId(), dto.getSeatIds().size());
rewrite('src/main/java/de/felixhertweck/seatreservation/reservation/service/ReservationService.java', r'LOG\.debugf\(\s*"Attempting to create reservation for user ID: %d for event ID %d with %d seats\.",\s*currentUser\.id,\s*dto\.getEventId\(\),\s*dto\.getSeatIds\(\)\.size\(\)\);', r'LOG.debugf("Attempting to create reservation for user ID: %d for event ID %d with %d seats.", currentUser.id, dto.getEventId(), (Integer) dto.getSeatIds().size());')

# AuthResource.java:[100,50] incompatible types: java.lang.Long cannot be converted to java.lang.String
# LOG.debugf("user ID: %d registered successfully. JWT and refresh token cookies set.", registerRequest.getUsername());
# Should just be user.id maybe? No, "registerRequest.getUsername()" returns a String. It was changed to "user.id" or "registerRequest.getUsername()" but it expects a string.
rewrite('src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java', r'user ID: %d registered successfully\.",\s*registerRequest\.getUsername\(\)\);', r'user: [HIDDEN] registered successfully.", "HIDDEN");')

# LiveViewResource.java:[60,80] incompatible types: java.lang.Long cannot be converted to java.lang.String
# LOG.debugf("Live View check-in info request for user ID: %d processed successfully.", username);
rewrite('src/main/java/de/felixhertweck/seatreservation/supervisor/resource/LiveViewResource.java', r'user ID: %d processed successfully\.",\s*username', r'user: [HIDDEN] processed successfully.", "HIDDEN"')
rewrite('src/main/java/de/felixhertweck/seatreservation/supervisor/resource/LiveViewResource.java', r'user ID: %d\.",\s*username', r'user: [HIDDEN].", "HIDDEN"')

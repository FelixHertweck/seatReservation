import re

def fix(filepath, old, new):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

fix('src/main/java/de/felixhertweck/seatreservation/reservation/resource/EventResource.java', 'user ID: %d", user.id', 'user ID: [HIDDEN]", "HIDDEN"')
fix('src/main/java/de/felixhertweck/seatreservation/reservation/resource/EventLocationResource.java', 'user ID: %d", user.id', 'user ID: [HIDDEN]", "HIDDEN"')
fix('src/main/java/de/felixhertweck/seatreservation/reservation/service/EventService.java', 'user ID: %d found.', 'user ID: [HIDDEN] found.')
fix('src/main/java/de/felixhertweck/seatreservation/reservation/service/EventService.java', 'user ID: %d", user.id', 'user ID: [HIDDEN]", "HIDDEN"')
fix('src/main/java/de/felixhertweck/seatreservation/reservation/service/EventLocationService.java', 'user ID: %d found.', 'user ID: [HIDDEN] found.')
fix('src/main/java/de/felixhertweck/seatreservation/reservation/service/EventLocationService.java', 'user ID: %d", user.id', 'user ID: [HIDDEN]", "HIDDEN"')

fix('src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java', 'LOG.infof("User ID: %d logged in successfully.", user.id);', 'LOG.infof("User ID: %d logged in successfully.", user.id);')
fix('src/main/java/de/felixhertweck/seatreservation/security/resource/AuthResource.java', 'user ID: %d logged in successfully.", loginRequest.getUsername()', 'user: [HIDDEN] logged in successfully.", "HIDDEN"')

# Check AuthService.java
fix('src/main/java/de/felixhertweck/seatreservation/security/service/AuthService.java', 'user ID: %d", registerRequest.getUsername()', 'user ID: [HIDDEN]", "HIDDEN"')

# Check CheckInResource.java & CheckInService.java (Not mentioned in errors but let's check LiveViewResource.java)
# supervisor/resource/LiveViewResource.java:[60,80] incompatible types: java.lang.Long cannot be converted to java.lang.String
# supervisor/resource/LiveViewResource.java:[78,82] incompatible types: java.lang.Long cannot be converted to java.lang.String
fix('src/main/java/de/felixhertweck/seatreservation/supervisor/resource/LiveViewResource.java', 'user.id', 'user.getId()') # wait, the log was probably 'user ID: %d", user.id' but it was expecting string?
# wait, %s is for string. Let's see.

# userManagment/resource/EmailConfirmationResource.java:[78,77] cannot find symbol symbol: variable user
fix('src/main/java/de/felixhertweck/seatreservation/userManagment/resource/EmailConfirmationResource.java', 'user ID: %d", user.id', 'user ID: [HIDDEN]", "HIDDEN"')

# userManagment/resource/UserResource.java:[115,96] cannot find symbol userCreationDTO.id
fix('src/main/java/de/felixhertweck/seatreservation/userManagment/resource/UserResource.java', 'userCreationDTO.id', 'userCreationDTO.getUsername()')

# userManagment/resource/UserResource.java:[270,92] cannot find symbol user
# userManagment/resource/UserResource.java:[291,85] cannot find symbol user
fix('src/main/java/de/felixhertweck/seatreservation/userManagment/resource/UserResource.java', 'user.id', 'user.getId()') # UserDTO doesn't have .id maybe? It has .id() or something, let's just make it id? Or userProfileUpdateDTO doesn't have it?

import re

filepaths = [
    'src/main/java/de/felixhertweck/seatreservation/model/repository/UserRepository.java',
    'src/main/java/de/felixhertweck/seatreservation/model/repository/LoginAttemptRepository.java',
    'src/main/java/de/felixhertweck/seatreservation/security/service/AuthService.java',
    'src/main/java/de/felixhertweck/seatreservation/supervisor/resource/CheckInResource.java',
    'src/main/java/de/felixhertweck/seatreservation/supervisor/service/CheckInService.java',
    'src/main/java/de/felixhertweck/seatreservation/userManagment/resource/EmailConfirmationResource.java',
    'src/main/java/de/felixhertweck/seatreservation/userManagment/service/UserService.java',
    'src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java'
]

for filepath in filepaths:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace logging of strings (emails, usernames) with [HIDDEN]
    # In UserRepository, username/email is passed as a string. We can hide it in logs.
    content = re.sub(r'username:\s*%s([^"]*)",\s*username', r'username: [HIDDEN]\1", "HIDDEN"', content)
    content = re.sub(r'username\s*\(optional\):\s*%s([^"]*)",\s*username', r'username: [HIDDEN]\1", "HIDDEN"', content)
    content = re.sub(r'username\s+%s([^"]*)",\s*username', r'username [HIDDEN]\1", "HIDDEN"', content)

    content = re.sub(r'email:\s*%s([^"]*)",\s*email', r'email: [HIDDEN]\1", "HIDDEN"', content)
    content = re.sub(r'email\s*%s([^"]*)",\s*(?:users\.size\(\),\s*)?email', r'email [HIDDEN]\1", "HIDDEN"', content) # might break size formatting, let's be simpler

    content = content.replace('email %s.", users.size(), email);', 'email [HIDDEN].", users.size(), "HIDDEN");')
    content = content.replace('email: %s", email);', 'email: [HIDDEN]", "HIDDEN");')
    content = content.replace('email: %s", userCreationDTO.email);', 'email: [HIDDEN]", "HIDDEN");')
    content = content.replace('email set to: %s", email);', 'email set to: [HIDDEN]", "HIDDEN");')

    # CheckIn
    content = content.replace('username %s.", username);', 'username [HIDDEN].", "HIDDEN");')
    content = content.replace('username %s not found.", username);', 'username [HIDDEN] not found.", "HIDDEN");')

    # Auth
    content = content.replace('username: %s", username);', 'username: [HIDDEN]", "HIDDEN");')
    content = content.replace('username: %s since: %s", username', 'username: [HIDDEN] since: %s", "HIDDEN"')
    content = content.replace('username %s: User not found.", username);', 'username [HIDDEN]: User not found.", "HIDDEN");')
    content = content.replace('username %s: Password mismatch.", username);', 'username [HIDDEN]: Password mismatch.", "HIDDEN");')

    # User service specific
    content = content.replace('username: %s", userCreationDTO.id);', 'username: [HIDDEN]", "HIDDEN");')
    content = content.replace('username: %s.", username);', 'username: [HIDDEN].", "HIDDEN");')
    content = content.replace('user ID %d (%s) marked as verified.", user.id, user.id);', 'user ID %d marked as verified.", user.id);')
    content = content.replace('resent to %s for user ID: %d", user.id, user.id);', 'resent for user ID: %d", user.id);')

    # Email Service
    content = content.replace('address: %s", address);', 'address: [HIDDEN]", "HIDDEN");')

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

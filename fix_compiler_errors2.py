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

# UserService.java:[247,53] incompatible types: java.lang.Long cannot be converted to java.lang.String
# LOG.errorf("User ID %d already exists", user.id); -> user is DTO, should be existingUser.id or just id
rewrite('src/main/java/de/felixhertweck/seatreservation/userManagment/service/UserService.java', r'LOG\.errorf\(\s*"User ID %d already exists",\s*user\.id\);', r'LOG.errorf("User ID %d already exists", id);')

# UserService.java:[346,21] cannot find symbol symbol: variable id location: variable user
# updateCurrentUserProfile ... "User profile for user ID: %d updated successfully.", user.id
replace('src/main/java/de/felixhertweck/seatreservation/userManagment/service/UserService.java', 'user.id', 'existingUser.id')

# EmailService.java:[264,44] incompatible types: java.lang.Long cannot be converted to java.lang.String
# EmailService.java:335, 478, 524, 722, 725 String.replace(Long) -> String.replace(String.valueOf(Long))
replace('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', '.replace("{{userId}}", user.id)', '.replace("{{userId}}", String.valueOf(user.id))')
replace('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', '.replace("{{userId}}", currentUser.id)', '.replace("{{userId}}", String.valueOf(currentUser.id))')
# What about EmailService 264? "No valid email addresses provided for reservation confirmation." user ID %d - it probably uses %s?
# wait, %s needs a String, not Long.
rewrite('src/main/java/de/felixhertweck/seatreservation/email/service/EmailService.java', r'LOG\.warnf\(\s*"No reservations provided for confirmation email to user ID: %s\.",\s*user\.id\);', r'LOG.warnf("No reservations provided for confirmation email to user ID: %d.", user.id);')
# Actually let's just restore EmailService and re-apply correctly.
# Let's check EmailService.java 264

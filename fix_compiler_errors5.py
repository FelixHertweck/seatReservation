import re

def rewrite(filepath, pattern, replacement):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = re.sub(pattern, replacement, content)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# We incorrectly replaced user.id with existingUser.id in places where it should be user.id
rewrite('src/main/java/de/felixhertweck/seatreservation/userManagment/service/UserService.java', 'existingUser.id', 'user.id')
# And now fix the ones that actually needed to be existingUser.id?
# Wait, user is AdminUserUpdateDTO in some methods, User in others.

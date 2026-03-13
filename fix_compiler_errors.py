import re

def fix_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

fix_file('src/main/java/de/felixhertweck/seatreservation/userManagment/service/UserService.java', [
    ('String.valueOf(user.id)', 'String.valueOf(existingUser.id)'),
    ('user.id', 'existingUser.id'), # wait, user is AdminUserUpdateDTO, doesn't have id? Yes, it's just 'id' which is passed as method arg
])
# Let's write a python script to run sed-like things properly since 'user.id' is too general.

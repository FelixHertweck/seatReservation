import re

def rewrite(filepath, pattern, replacement):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    content = re.sub(pattern, replacement, content)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

filepath = 'src/main/java/de/felixhertweck/seatreservation/userManagment/service/UserService.java'

# [247,53] incompatible types: java.lang.Long cannot be converted to java.lang.String
# "User ID %d already exists", user.id  -> where user is AdminUserUpdateDTO.
rewrite(filepath, r'LOG\.errorf\("User ID %d already exists",\s*user\.id\);', r'LOG.errorf("User ID %d already exists", id);')

# [346,21] cannot find symbol id on userProfileUpdateDTO
rewrite(filepath, r'LOG\.infof\("User profile for user ID: %d updated successfully\.",\s*existingUser\.id\);', r'LOG.infof("User profile for user ID: %d updated successfully.", existingUser.id);')

# Wait, the error is user.id where user is UserProfileUpdateDTO?
rewrite(filepath, r'user\.id', r'user.id') # Actually let's just use sed or Python properly

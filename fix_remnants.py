import re

filepaths = [
    'src/main/java/de/felixhertweck/seatreservation/email/service/EmailSeatMapService.java',
    'src/main/java/de/felixhertweck/seatreservation/userManagment/resource/EmailConfirmationResource.java'
]

for filepath in filepaths:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    content = content.replace('token %s", token);', 'token [HIDDEN]", "HIDDEN");')
    content = content.replace('Token: %s", token);', 'Token: [HIDDEN]", "HIDDEN");')
    content = content.replace('email);', '"HIDDEN");')

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

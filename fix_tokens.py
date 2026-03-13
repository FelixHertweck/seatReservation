import re
import glob

# Tokens are secrets, so they shouldn't be logged either.
filepaths = glob.glob('src/main/java/**/*.java', recursive=True)

for filepath in filepaths:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Hide tokens
    content = content.replace('token: %s", token);', 'token: [HIDDEN]", "HIDDEN");')
    content = content.replace('code: %s", verificationCode);', 'code: [HIDDEN]", "HIDDEN");')
    content = content.replace('seatmap token: %s", seatmapToken);', 'seatmap token: [HIDDEN]", "HIDDEN");')

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

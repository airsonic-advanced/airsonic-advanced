import os
import re

# Find all Java files
root_path = r'C:\Users\zlhe\Source\Repos\airsonic-advanced'
java_files = []

for root, dirs, files in os.walk(root_path):
    # Skip target directories
    if 'target' in root:
        continue
    for file in files:
        if file.endswith('.java'):
            java_files.append(os.path.join(root, file))

# Define replacements
replacements = [
    (r'import javax\.annotation\.', 'import jakarta.annotation.'),
    (r'import javax\.servlet\.', 'import jakarta.servlet.'),
    (r'import javax\.validation\.', 'import jakarta.validation.'),
    (r'import javax\.persistence\.', 'import jakarta.persistence.'),
]

updated_files = []
total_replacements = 0

# Process each file
for filepath in java_files:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        file_replacements = 0
        
        # Apply all replacements
        for pattern, replacement in replacements:
            matches = len(re.findall(pattern, content))
            if matches > 0:
                content = re.sub(pattern, replacement, content)
                file_replacements += matches
        
        # Write back if changed
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            updated_files.append(filepath)
            total_replacements += file_replacements
    except Exception as e:
        print(f'Error processing {filepath}: {e}')

print(f'Migration complete!')
print(f'Files updated: {len(updated_files)}')
print(f'Total import statements replaced: {total_replacements}')
print(f'\nSample of updated files:')
for f in updated_files[:10]:
    print(f'  {f}')
if len(updated_files) > 10:
    print(f'  ... and {len(updated_files) - 10} more files')

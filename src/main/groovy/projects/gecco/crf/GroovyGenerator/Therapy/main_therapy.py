import os
import shutil
import subprocess

keyword = "Therapy"

src = f"./GroovyGenerator/{keyword}/Constant"
dest = f"./GroovyGenerator/{keyword}/Final"

# Delete all files in Final
for filename in os.listdir(dest):
    file_path = os.path.join(dest, filename)
    try:
        if os.path.isfile(file_path) or os.path.islink(file_path):
            os.unlink(file_path)
        elif os.path.isdir(file_path):
            shutil.rmtree(file_path)
    except Exception as e:
        print('Failed to delete %s. Reason: %s' % (file_path, e))

# Move files from Constant to Final
src_files = os.listdir(src)
for file_name in src_files:
    full_file_name = os.path.join(src, file_name)
    if os.path.isfile(full_file_name):
        shutil.copy(full_file_name, dest)

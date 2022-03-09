import os
import shutil
import subprocess


def src(keyword):
    return f"./GroovyGenerator/{keyword}/Constant"


dest = "."

# Delete old files in crf
for filename in os.listdir(dest):
    file_path = os.path.join(dest, filename)

    try:
        if os.path.isfile(file_path) or os.path.islink(file_path):
            os.unlink(file_path)
        elif os.path.isdir(file_path):
            shutil.rmtree(file_path)
    except Exception as e:
        print('Failed to delete %s. Reason: %s' % (file_path, e))

# Get General Files

# Generate Anamnesis files
subprocess.run(['python', f"./GroovyGenerator/Anamnesis/main_anamnesis.py"])

# Generate Imaging files
subprocess.run(['python', f"./GroovyGenerator/Imaging/main_imaging.py"])

# Generate Demographics files
subprocess.run(['python', f"./GroovyGenerator/Demographics/main_demographics.py"])

# Generate Remaining files
# .... Add later ....

# Copy files from respective *Folder*/Final folders to crf folders

# Generate final ExportResourceMappingConfig.json

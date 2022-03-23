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

# Generate Epidemiology files
subprocess.run(['python', f"./GroovyGenerator/Demographics/main_epidemiology.py"])

# Generate Complications files
subprocess.run(['python', f"./GroovyGenerator/Complications/main_complications.py"])

# Generate Onset of Illness files
subprocess.run(['python', f"./GroovyGenerator/Onset of Illness/main_onset_illness.py"])

# Generate Laboratory Values
subprocess.run(['python', f"./GroovyGenerator/Laboratory Values/main_lab_values.py"])

# Generate Vital Signs Values
subprocess.run(['python', f"./GroovyGenerator/Medication/main_medication.py"])

# # Generate Vital Signs Values
# subprocess.run(['python', f"./GroovyGenerator/Vital Signs/main_vital_signs.py"])

# Generate Remaining files
# .... Add later ....

# Copy files from respective *Folder*/Final folders to crf folders

# Generate final ExportResourceMappingConfig.json

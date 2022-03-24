import os
import shutil
import subprocess


def src(keyword):
    return f"./GroovyGenerator/{keyword}/Constant"


dest = "."

# Delete old files in crf (not folders)
for filename in os.listdir(dest):
    file_path = os.path.join(dest, filename)
    try:
        if os.path.isfile(file_path) or os.path.islink(file_path):
            os.unlink(file_path)
    except Exception as e:
        print('Failed to delete %s. Reason: %s' % (file_path, e))

# Generate General Files

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

# Generate Laboratory Values files
subprocess.run(['python', f"./GroovyGenerator/Laboratory Values/main_lab_values.py"])

# Generate Medication files
subprocess.run(['python', f"./GroovyGenerator/Medication/main_medication.py"])

# Generate Outcome at Discharge files
subprocess.run(['python', f"./GroovyGenerator/Outcome at Discharge/main_outcome_discharge.py"])

# Generate Study Enrollment files
subprocess.run(['python', f"./GroovyGenerator/Study Enrollment/main_study_enrollment.py"])

# Generate Symptoms files
subprocess.run(['python', f"./GroovyGenerator/Symptoms/main_symptoms.py"])

# Generate Therapy files
subprocess.run(['python', f"./GroovyGenerator/Therapy/main_therapy.py"])

# Generate Vital Signs Values
subprocess.run(['python', f"./GroovyGenerator/Vital Signs/main_vital_signs.py"])

# Copy files from respective *Folder*/Final folders to crf folders

# Generate final ExportResourceMappingConfig.json

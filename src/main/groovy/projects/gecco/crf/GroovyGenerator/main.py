import os
import shutil
import subprocess

src = "./GroovyGenerator/"
dest = "."

# Delete old files in crf (not folders)
for filename in os.listdir(dest):
    file_path = os.path.join(dest, filename)
    try:
        if os.path.isfile(file_path) or os.path.islink(file_path):
            os.unlink(file_path)
    except Exception as e:
        print('Failed to delete %s. Reason: %s' % (file_path, e))


# Generate beginning of ExportResourceMappingConfig
with open(dest + "/ExportResourceMappingConfig.json", "w") as f:
    new_str = f"""{{
    "description": "This configuration links a CentraXX entity (selectFromCxxEntity) to a FHIR resource (exportToFhirResource) by conversion through a transformation template (transformByTemplate). Only the template can be changed. The same entity can be configured to the same FHIR resource by multiple templates. The configuration can be changed during runtime without CentraXX restart. The mapping order is important, if the target system checks referential integrity (e.g. blaze store).",
    "mappings": ["""
    f.write(new_str)


# Iterate over the Folders in src
for folder in os.listdir(src):

    # Select folders only
    folder_path = os.path.join(src, folder)
    final_path = f"{folder_path}/Final"
    if not os.path.isdir(folder_path):
        continue

    # Generate respective files based on main_*.py
    py_file = [filename for filename in os.listdir(folder_path) if filename.endswith(".py")][0]

    subprocess.run(['python', f"{folder_path}/{py_file}"])

    # Get content from partial_ExportResourceMappingConfig and past into main ExportResourceMappingConfig
    with open(f"{final_path}/partial_ExportResourceMappingConfig.txt", "r") as f:
        partial_str = f.read()

        with open(dest + "/ExportResourceMappingConfig.json", "a") as f2:
            f2.write(partial_str)

    os.unlink(f"{final_path}/partial_ExportResourceMappingConfig.txt")

    # Copy files from respective Final to main folder
    for file in os.listdir(final_path):
        file_path = os.path.join(final_path, file)
        shutil.copy(file_path, dest)

# Finalize ExportResourceMappingConfig file
with open(dest + "/ExportResourceMappingConfig.json", "r") as f:
    full_file = f.read()

with open(dest + "/ExportResourceMappingConfig.json", "w") as f:
    f.write(full_file[:-1] + "\n    ]\n}")

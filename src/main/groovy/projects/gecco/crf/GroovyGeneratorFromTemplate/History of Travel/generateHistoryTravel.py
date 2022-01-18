import os

# Number of times the history of travel must be created
nb_iterations = 2
# Path for the template file
template = "historyTravelTemplate"

# Auxiliar file with excerpt for ExportResourceMappingConfig
aux_file_name = "aux_ExportResourceMappingConfig.txt"
if os.path.exists(aux_file_name):
    os.remove(aux_file_name)

# Load corresponding template file as a string
with open(template, "r") as f:
    templateString = f.read()

# Iterate over each "area"
for i in range(nb_iterations):
    # Replace iter by nb
    updated_template = templateString.replace(f"##iter##", str(i))

    # save newly generated groovy file
    new_file_name = "observationHistoryOfTravel_" + str(i)
    with open(new_file_name + ".groovy", "w") as f:
        f.write(updated_template)

    # Add new file info to excerpt of ExportResourceMappingConfig
    with open(aux_file_name, "a") as f:
        append_str = f"""{{
  "selectFromCxxEntity": "STUDY_VISIT_ITEM",
  "transformByTemplate": "observationHistoryOfTravel_{i}",
  "exportToFhirResource": "Observation"
}},\n"""
        f.write(append_str)

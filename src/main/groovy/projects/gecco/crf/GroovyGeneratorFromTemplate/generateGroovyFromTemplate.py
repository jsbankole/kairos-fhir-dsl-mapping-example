import pandas as pd
import numpy as np
import os

############# LUNG DISEASES ##################
# template_file = "template_LungDiseases"
# values_file = "values_LungDiseases.xlsx"
# file_name_root = "conditionChronicLungDisease_"
##############################################

######### CARDIOVASCULAR DISEASES ############
# template_file = "template_CardiovascularDiseases"
# values_file = "values_CardiovascularDiseases.xlsx"
# file_name_root = "conditionCardiovascularDisease_"
##############################################

############## LIVER DISEASES ################
# template_file = "template_LiverDiseases"
# values_file = "values_LiverDiseases.xlsx"
# file_name_root = "conditionLiverDisease_"
##############################################

############ RHEUMA/IMUNO DISEASES ###########
# template_file = "template_RheumaImunoDiseases"
# values_file = "values_RheumaImunoDiseases.xlsx"
# file_name_root = "conditionRheumaImunoDisease_"
##############################################

############### NEURO DISEASES ###############
template_file = "template_NeuroDiseases"
values_file = "values_NeuroDiseases.xlsx"
file_name_root = "conditionNeuroDisease_"
##############################################

# Load template file as a string
with open(template_file, "r") as f:
    templateString = f.read()

# Load values from excel
values_df = pd.read_excel(values_file)
values_df = values_df.replace(np.nan, '')

# Fields to replace in template (name of column in the excel)
available_fields = ["ParameterCodeDisease", "IdComplement", "ICDCode", "DiseaseName-EN", "SnomedCode"]

# Auxiliar file with excerpt for ExportResourceMappingConfig
aux_file_name = "aux_ExportResourceMappingConfig.txt"
if os.path.exists(aux_file_name):
    os.remove(aux_file_name)

# Iterate over the diseases from the excel file
for _, row in values_df.iterrows():

    # Replace disease specific values in the template
    updated_template = templateString
    for field_name in available_fields:
        updated_template = updated_template.replace(f"##{field_name}##", str(row[field_name]))

    # save newly generated groovy file
    new_file_name = file_name_root + row["IdComplement"].lower()
    with open(new_file_name + ".groovy", "w") as f:
        f.write(updated_template)

    # Add new file info to excerpt of ExportResourceMappingConfig
    with open(aux_file_name, "a") as f:
        append_str = f"""{{
      "selectFromCxxEntity": "STUDY_VISIT_ITEM",
      "transformByTemplate": "{new_file_name}",
      "exportToFhirResource": "Condition"
}},\n"""
        f.write(append_str)

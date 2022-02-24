import pandas as pd
import numpy as np

src = f"./GroovyGenerator/Anamnesis/Diseases/"
dest = f"./GroovyGenerator/Anamnesis/Final/"

template_file_list = []
values_file_list = []
file_name_root_list = []

# ####### CARDIOVASCULAR DISEASES ############
template_file_list.append("template_CardiovascularDiseases")
values_file_list.append("values_CardiovascularDiseases.xlsx")
file_name_root_list.append("conditionCardiovascularDisease_")
##############################################

# ############ LIVER DISEASES ################
template_file_list.append("template_LiverDiseases")
values_file_list.append("values_LiverDiseases.xlsx")
file_name_root_list.append("conditionLiverDisease_")
##############################################

# ########### LUNG DISEASES ##################
template_file_list.append("template_LungDiseases")
values_file_list.append("values_LungDiseases.xlsx")
file_name_root_list.append("conditionLungDiseases_")
##############################################

# ############# NEURO DISEASES ###############
template_file_list.append("template_NeuroDiseases")
values_file_list.append("values_NeuroDiseases.xlsx")
file_name_root_list.append("conditionNeuroDisease_")
##############################################

# ########## RHEUMA/IMUNO DISEASES ###########
template_file_list.append("template_RheumaImunoDiseases")
values_file_list.append("values_RheumaImunoDiseases.xlsx")
file_name_root_list.append("conditionRheumaImunoDisease_")
##############################################

# Define partial_ExportResourceMappingConfig path
aux_file_name = dest + "partial_ExportResourceMappingConfig.txt"


# Iterate over each "area"
for (template_file, values_file, file_name_root) in zip(template_file_list, values_file_list, file_name_root_list):
    # Load corresponding template file as a string
    with open(src + template_file, "r") as f:
        templateString = f.read()

    # Load corresponding values from excel
    values_df = pd.read_excel(src + values_file)
    values_df = values_df.replace(np.nan, '')

    # Fields to replace in template (name of column in the excel)
    available_fields = ["ParameterCodeDisease", "IdComplement", "ICDCode", "DiseaseName-EN", "SnomedCode"]

    # Iterate over the diseases from the excel file
    for _, row in values_df.iterrows():

        # Replace disease specific values in the template
        updated_template = templateString
        for field_name in available_fields:
            updated_template = updated_template.replace(f"##{field_name}##", str(row[field_name]))

        # save newly generated groovy file
        new_file_name = file_name_root + row["IdComplement"].lower()
        with open(dest + new_file_name + ".groovy", "w") as f:
            f.write(updated_template)

        # Add new file info to excerpt of ExportResourceMappingConfig
        with open(aux_file_name, "a") as f:
            append_str = f"""
    {{
        "selectFromCxxEntity": "STUDY_VISIT_ITEM",
        "transformByTemplate": "{new_file_name}",
        "exportToFhirResource": "Condition"
    }},"""
            f.write(append_str)


Generate groovy files based on template txt file and excel with required values

### template_****
This file contains the template for the fhir resource. Changes in this file will be applied to all the diseases from the same "area"
Words with this pattern ##**## will be searched and replaced (** will correspond to the name of the column in the values_**** excel)
Currently the entries required are:
ParameterCodeDisease - (Corresponds to the centraxx code for this disease
IdComplement - (name of the disease to be used in the name of the file
ICDCode - ICD Code of the disease (leave empty if not exists)
DiseaseName-EN - Name of the disease in English
SnomedCode - Snomed Code of the disease (leave empty if not exists)


### values_****.xlsx
Excel file with the required values to substitute in the template
Each row corresponds to a disease
If desired new disease just add it here


### generateGroovyFromTemplate.py
python file that generates the groovy files
Only required to (un)comment the desired "area" to which we want to generate the groovy files
The files are generated in the current directory
Afterwards just need to move the groovy files from the current directory to the desired one
After running each time the aux_ExportResourceMappingConfig.txt is also created, here is the extract with the required string to be added in the ExportResourceMappingConfig







Generate groovy files based on template txt file and excel with required values

### template_****
This file contains the template for the fhir resource. Changes in this file will be applied to all the organs specified
Words with this pattern ##**## will be searched and replaced (** will correspond to the name of the column in the values_**** excel)
Currently the entries required are:
ParameterCodeOrgan - Corresponds to the centraxx code for this organ
IdComplement - name of the organ to be used in the name of the file
ICDCode - ICD Code of the organ (leave empty if not exists)
OrganName-EN - Name of the organ in English
SnomedCode - Snomed Code of the disease (leave empty if not exists)


### values_****.xlsx
Excel file with the required values to substitute in the template
Each row corresponds to an organ
If desired organ just add it here


### main_*.py
python file that generates the groovy files
Only required to (un)comment the desired "area" to which we want to generate the groovy files
Possible to select more than one area simultaneously
The files are generated in the current directory
ALL the files for ALL organs specified will be created
And moves the groovy files to the desired folder
After running each time the aux_ExportResourceMappingConfig.txt is also created, that is later used to build the complete one






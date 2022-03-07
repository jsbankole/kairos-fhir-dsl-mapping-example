
Generate groovy files for Immunization based on template txt file

All files have the same content but with different iteration, which will correspond to the respective line in the Centrax form

### immunizationTemplate
Has the string corresponding to the general code


### main_anamnesis_immunization.py
python file that generates the groovy files
Only required to change the nb_iterations and path of the template
The files are generated in the current directory
Afterwards just need to move the groovy files from the current directory to the desired one
After running each time the aux_ExportResourceMappingConfig.txt is also created, here is the extract with the required string to be added in the ExportResourceMappingConfig






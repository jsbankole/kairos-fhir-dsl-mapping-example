import pandas as pd
import numpy as np

src = f"./GroovyGenerator/General/Consent/"
values_file = "values_policy.xlsx"

# Load corresponding values from excel
values_df = pd.read_excel(src + values_file)
values_df = values_df.replace(np.nan, '')

available_fields = ["Level/Typ", "Code", "Bezeichnung", "Codesystem", "SystemID"]

final_str = """static String[] mapConsentData(final String cxxConsentPart){
  switch(cxxConsentPart) {"""

# Iterate over the diseases from the excel file
for _, row in values_df.iterrows():
    final_str = final_str + f"""
        case ("{row["Bezeichnung"]}"):
      return ["{row["Code"]}", "{row["Bezeichnung"]}"]"""

final_str = final_str + "\n   }\n}"

print(final_str)


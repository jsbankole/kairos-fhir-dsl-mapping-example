package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/respiratorytherapies-procedure
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 */
procedure {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL") {
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus != "APPROVED") {
    return //no export
  }

  final def crfItemRespThera = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_SAUERSTOFFTHERAPIE" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemRespThera) {
    return
  }
  if (crfItemRespThera[CrfItem.CATALOG_ENTRY_VALUE] != []) {

    id = "Procedure/RespiratoryTherapies-" + context.source[studyVisitItem().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/respiratory-therapies"
    }

    crfItemRespThera[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
      final def STATUScode = matchResponseToSTATUS(item[CatalogEntry.CODE] as String)
      if (STATUScode) {
        status = STATUScode

        if (STATUScode == "in-progress"){

          final def crfItemRespTheraDate = context.source[studyVisitItem().crf().items()].find {
            "COV_GECCO_SAUERSTOFFTHERAPIE_DATE" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
          }

          performedDateTime {
            if(crfItemRespTheraDate && crfItemRespTheraDate[CrfItem.DATE_VALUE]){
              date = crfItemRespTheraDate[CrfItem.DATE_VALUE].toString().substring(6, 16)
            }else{
              extension {
                url = "http://hl7.org/fhir/StructureDefinition/data-absent-reason"
                valueCode = "unknown"
              }
            }
          }

        }else if(STATUScode == "not-done"){
          performedDateTime {
            extension {
              url = "http://hl7.org/fhir/StructureDefinition/data-absent-reason"
              valueCode = "not-performed"
            }
          }

        }else{
          performedDateTime {
            extension {
              url = "http://hl7.org/fhir/StructureDefinition/data-absent-reason"
              valueCode = "unknown"
            }
          }
        }
      }
    }

    category {
      coding {
        system = "http://snomed.info/sct"
        code = "277132007"
      }
    }

    code {
      coding {
        system = "http://snomed.info/sct"
        code = "53950000"
      }
    }
    subject {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
    }
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}

static String matchResponseToSTATUS(final String resp) {
  switch (resp) {
    case ("COV_JA"):
      return "in-progress"
    case ("COV_NEIN"):
      return "not-done"
    case ("COV_UNBEKANNT"):
      return "unknown"
    default: null
  }
}

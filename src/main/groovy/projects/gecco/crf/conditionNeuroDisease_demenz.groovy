package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Old version
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/chronicneurologicalmentaldiseases
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 *
 * New version
 * @author Mário Macedo
 */
condition {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL") {
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus == "OPEN") {
    return //no export
  }

  final def crfItemNeuro = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_NEURO_ERKRANKUNG_DEMENZ" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  if (crfItemNeuro[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "Condition/NeuroDisease-Demenz-" + context.source[studyVisitItem().crf().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/chronic-neurological-mental-diseases"
    }

    crfItemNeuro[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
      final def VERcode = matchResponseToVerificationStatus(item[CatalogEntry.CODE] as String)
      final def VERcode_JA = matchResponseToVerificationStatus("COV_JA")
      final def VERcode_NEIN = matchResponseToVerificationStatus("COV_NEIN")
      final def VERcode_UNBEKANNT = matchResponseToVerificationStatus("COV_UNBEKANNT")

      // Disease confirmed Present
      if (VERcode == VERcode_JA) {

        verificationStatus {
          coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
            code = matchResponseToVerificationStatusHL7(item[CatalogEntry.CODE] as String)
          }
          coding {
            system = "http://snomed.info/sct"
            code = VERcode
          }
        }

        // Disease confirmed Absense
      } else if (VERcode == VERcode_NEIN) {
        verificationStatus {
          coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
            code = matchResponseToVerificationStatusHL7(item[CatalogEntry.CODE] as String)
          }
          coding {
            system = "http://snomed.info/sct"
            code = VERcode
          }
        }

        // Disease presence unknown
      } else if (VERcode == VERcode_UNBEKANNT) {
        modifierExtension {
          url = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/uncertainty-of-presence"
          valueCodeableConcept {
            coding {
              system = "http://snomed.info/sct"
              code = VERcode_UNBEKANNT
              display = "Unknown (qualifier value)"
            }
            text =  "Presence of condition is unknown."
          }
        }
      }
    }

    category {
      coding {
        system = "http://snomed.info/sct"
        code = "394591006"
        display = "Neurology"
      }
      coding {
        system = "http://snomed.info/sct"
        code = "394587001"
        display = "Psychiatry"
      }
    }

    code {
      final def ICDcode = "F03"
      if (ICDcode != "") {
        coding {
          system = "http://fhir.de/CodeSystem/dimdi/icd-10-gm"
          version = "2020"
          code = ICDcode
          display = "Dementia"
        }
      }

      final def SNOMEDcode = "52448006"
      if (SNOMEDcode != "") {
        coding {
          system = "http://snomed.info/sct"
          code = SNOMEDcode
          display = "Dementia"
        }
      }
    }

    subject {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().id()]
    }

    recordedDate {
      date = normalizeDate(crfItemNeuro[CrfItem.CREATIONDATE] as String)
      precision = TemporalPrecisionEnum.DAY.toString()
    }

  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}

static String matchResponseToVerificationStatus(final String resp) {
  switch (resp) {
    case null:
      return null
    case ("COV_UNBEKANNT"):
      return "261665006"
    case ("COV_NEIN"):
      return "410594000"
    //"COV_JA"
    default: "410605003"
  }
}

static String matchResponseToVerificationStatusHL7(final String resp) {
  switch (resp) {
    case null:
      return null
    case ("COV_UNBEKANNT"):
      return "unconfirmed"
    case ("COV_NEIN"):
      return "refuted"
    //"COV_JA"
    default: "confirmed"
  }
}
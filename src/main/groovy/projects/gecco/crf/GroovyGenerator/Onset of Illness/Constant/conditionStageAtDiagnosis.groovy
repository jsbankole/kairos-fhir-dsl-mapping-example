package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/diagnosiscovid19
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 *
 * NOTE: Due to the Cardinality-restraint (1..1) for "code", multiple selections in CXX for this parameter
 *       will be added as additional codings.
 */
condition {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL") {
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_KRANKHEITSBEGINN / AUFNAHME" || studyVisitStatus != "APPROVED") {
    return //no export
  }
  final def crfItemStage = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_STAGE_DIAGNOSIS" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemStage) {
    return
  }
  if (crfItemStage[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "Condition/StageAtDiagnosis-s" + context.source[studyVisitItem().crf().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/diagnosis-covid-19"
    }

    crfItemStage[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
      final def VERcode = matchResponseToVerificationStatus(item[CatalogEntry.CODE] as String)
      if (VERcode == "261665006") {
        modifierExtension {
          url = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/uncertainty-of-presence"
          valueCodeableConcept {
            coding {
              system = "http://snomed.info/sct"
              code = "261665006"
              display = "Unknown (qualifier value)"
            }
          }
        }
      } else {
        clinicalStatus{
          coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-clinical"
            code = "active"
            display = "Active"
          }
        }

        verificationStatus {
          coding {
            system = "http://snomed.info/sct"
            code = "410605003"
            display = "Confirmed present (qualifier value)"
          }
          coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
            code = "confirmed"
            display = "Confirmed"
          }
        }
      }
    }

    category {
      coding {
        system = "http://snomed.info/sct"
        code = "394807007"
        display = "Infectious diseases (specialty) (qualifier value)"
      }
    }

    subject {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
    }

    code {
      coding {
        system = "http://snomed.info/sct"
        code = "840539006"
        display = "Disease caused by Severe acute respiratory syndrome coronavirus 2 (disorder)"
      }
    }

    recordedDate {
      date = normalizeDate(context.source[studyVisitItem().lastApprovedOn()] as String)
      precision = TemporalPrecisionEnum.DAY.toString()
    }

    final def stageInfo = matchResponseToSNOMED(crfItemStage[CrfItem.CATALOG_ENTRY_VALUE][CatalogEntry.CODE][0] as String)

    stage {
      summary {
        coding {
          system = "http://snomed.info/sct"
          code = stageInfo[0]
          display = stageInfo[2]
        }
        text = stageInfo[1]
      }
      type {
        coding{
          system = "http://loinc.org"
          code = "88859-4"
          display = "Disease stage score for risk calculation"
        }
      }
    }
  }
}


static String[] matchResponseToSNOMED(final String resp) {
  switch (resp) {
    case ("COV_UNCOMPLICATED_PHASE"):
      return ["255604002", "Uncomplicated phase", "Mild to moderate (qualifier value)"]
    case ("COV_COMPLICATED_PHASE"):
      return ["371924009", "Complicated phase", "Moderate to severe (qualifier value)"]
    case ("COV_CRITICAL_PHASE"):
      return ["442452003", "Critical phase", "Life threatening severity (qualifier value)"]
    case ("COV_RECOVERY_PHASE"):
      return ["277022003", "Recovery phase", "Remission phase (qualifier value)"]
    case ("COV_DEAD"):
      return ["399166001", "Dead", "Fatal (qualifier value)"]
    case ("COV_UNKNOWN"):
      return ["261665006", "Unknown", "Unknown (qualifier value)"]
    default: [null, null, null]
  }
}

static String matchResponseToVerificationStatus(final String resp) {
  switch (resp) {
    case null:
      return null
    case ("COV_UNKNOWN"):
      return "261665006"
    default: "410605003"
  }
}

static String matchResponseToVerificationStatusHL7(final String resp) {
  switch (resp) {
    case null:
      return null
    case ("COV_UNKNOWN"):
      return "unconfirmed"
    default: "confirmed"
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}

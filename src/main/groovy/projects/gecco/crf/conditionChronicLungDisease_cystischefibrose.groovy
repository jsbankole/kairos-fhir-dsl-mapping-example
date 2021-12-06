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
 * Specified by https://simplifier.net/forschungsnetzcovid-19/chroniclungdiseases
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

  final def crfItemLung = context.source[studyVisitItem().crf().items()].find {
    getParameterCodeDisease() == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  if (crfItemLung[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = getIdComplement() + context.source[studyVisitItem().crf().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile getMetaProfile()
    }

    crfItemLung[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
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
        code = getCategoryCode()
        display = getCategoryDisplay()
      }
    }

    code {
      final def ICDcode = getICDCode()
      if (ICDcode) {
        coding {
          system = "http://fhir.de/CodeSystem/dimdi/icd-10-gm"
          version = "2020"
          code = ICDcode
          display = getDiseaseName()
        }
      }

      final def SNOMEDcode = getSnomedCode()
      if (SNOMEDcode) {
        coding {
          system = "http://snomed.info/sct"
          code = SNOMEDcode
          display = getDiseaseName()
        }
      }
    }

    subject {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().id()]
    }

    recordedDate {
      date = normalizeDate(crfItemLung[CrfItem.CREATIONDATE] as String)
      precision = TemporalPrecisionEnum.DAY.toString()
    }

  }
}

///////////////////// Category Attributes //////////////////////////
// Lung Diseases
static String getMetaProfile() {
  return "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/chronic-lung-diseases"
}
static String getCategoryCode() {
  return "418112009"
}
static String getCategoryDisplay() {
  return "Pulmonary medicine"
}

///////////////////// SUB - Category Attributes //////////////////////////

static String getParameterCodeDisease() {
  return "COV_GECCO_LUNGENERKRANKUNG_CYSTISCHE_FIBROSE"
}
static String getIdComplement() {
  return "Condition/ChronicLungDisease-CystischeFibrose-"
}
static String getICDCode() {
  return "E84.9"
}
static String getSnomedCode() {
  return "190905008"
}
static String getDiseaseName() {
  return "Cystische Fibrose"
}

///////////////////// Generic Functions //////////////////////////

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


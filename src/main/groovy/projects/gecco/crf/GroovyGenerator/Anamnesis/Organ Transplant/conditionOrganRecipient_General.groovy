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
 * Specified by https://simplifier.net/forschungsnetzcovid-19/cardiovasculardiseases
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
  if (crfName != "SarsCov2_ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus != "APPROVED") {
    return //no export
  }

  final def crfItem = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_ORGANTRANSPLANTATION" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  if (crfItem[CrfItem.CATALOG_ENTRY_VALUE] == []) {
    return
  }

  final def VERcode = crfItem[CrfItem.CATALOG_ENTRY_VALUE][0][CatalogEntry.CODE]

  // Organ Transplant confirmed - If confirmed, then no need for general
  if(VERcode == "COV_JA"){
    return
  }

  id = "Condition/OrganRecipient-General-" + context.source[studyVisitItem().crf().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/organ-recipient"
  }

  // Organ Transplant confirmed Absense
  if (VERcode == "COV_NEIN") {
    verificationStatus {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
        code = matchResponseToVerificationStatusHL7("COV_NEIN")
      }
      coding {
        system = "http://snomed.info/sct"
        code = matchResponseToVerificationStatus("COV_NEIN")
        display = "Definitely NOT present (qualifier value)"
      }
    }

    // Organ Transplant presence unknown
  } else if (VERcode == "COV_UNBEKANNT") {
    modifierExtension {
      url = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/uncertainty-of-presence"
      valueCodeableConcept {
        coding {
          system = "http://snomed.info/sct"
          code = matchResponseToVerificationStatus("COV_UNBEKANNT")
          display = "Unknown (qualifier value)"
        }
        text =  "Presence of condition is unknown."
      }
    }
  }

  category {
    coding {
      system = "http://snomed.info/sct"
      code = "788415003"
      display = "Transplant medicine"
    }
  }

  code {
    coding {
      system = "http://snomed.info/sct"
      code = "161663000"
      display = "History of being a tissue or organ recipient"
    }
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  recordedDate {
    date = normalizeDate(context.source[studyVisitItem().lastApprovedOn()] as String)
    precision = TemporalPrecisionEnum.DAY.toString()
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
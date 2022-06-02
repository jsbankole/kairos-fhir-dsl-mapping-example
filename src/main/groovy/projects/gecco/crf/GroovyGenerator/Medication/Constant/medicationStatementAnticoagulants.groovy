package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/pharmacologicaltherapyanticoagulants
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 */
medicationStatement {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL") {
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_MEDIKATION" || studyVisitStatus != "APPROVED") {
    return //no export
  }
  final def crfItemATC = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_ANTIKOAGULATION" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemATC) {
    return
  }
  if (crfItemATC[CrfItem.CATALOG_ENTRY_VALUE] == []) {
    return
  }
  final def crfItemThera = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_ANTIKOAGULATION_ABSICHT" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  id = "MedicationStatement/AntiCoagulation-" + context.source[studyVisitItem().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/pharmacological-therapy-anticoagulants"
  }

  crfItemATC[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
    status = matchResponseToSTATUS(item[CatalogEntry.CODE] as String)
  }

  medicationCodeableConcept {
    crfItemATC[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
      final def ATCinfo = getAnticoagulantATCInfo(item[CatalogEntry.CODE] as String)

      if (ATCinfo[0]) {
        coding{
          system = "http://snomed.info/sct"
          code = "81839001"
          display = "Medicinal product acting as anticoagulant agent (product)"
        }

        coding {
          system = "http://fhir.de/CodeSystem/dimdi/atc"
          code = ATCinfo[0]
          display = ATCinfo[1]
        }
      }
    }
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  effectiveDateTime {
    date = normalizeDateTime(context.source[studyVisitItem().lastApprovedOn()] as String)
  }

  reasonCode{
    crfItemThera[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
      final def theraIntentInfo = getTherapeuticIntentSnomedInfo(item[CatalogEntry.CODE] as String)

      if (theraIntentInfo[0]) {
        coding {
          system = "http://snomed.info/sct"
          code = theraIntentInfo[0]
          display = theraIntentInfo[1]
        }
      }
    }
  }
}


static String normalizeDateTime(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static String[] getAnticoagulantATCInfo(final String resp) {
  switch (resp) {
    case ("COV_UNFRAK_HEPARIN"):
      return ["B01AB01", "Heparin"]
    case ("COV_NIEDERMOL_HEPARIN"):
      return ["B01AB08", "Reviparin"] //?
    case ("COV_ARGATROBAN"):
      return ["B01AE03", "Argatroban"]
    case ("COV_PLAETTCHENAGGRHEMMER"):
      return ["B01AC", "Thrombozytenaggregationshemmer, exkl. Heparin"]
    case ("COV_DANAPAROID"):
      return ["B01AB09", "Danaparoid"]
    case ("COV_PHENPROCOUMON"):
      return ["B01AA04", "Phenprocoumon"]
    case ("COV_DOAK"):
      return ["B01AE", "Direkte Thrombininhibitoren"]
    default: [null, null]
  }
}

static String[] getTherapeuticIntentSnomedInfo(final String resp) {
  switch (resp) {
    case ("COV_GECCO_THERAPEUTISCH_ABSICHT_PROPHYLAKTISCH"):
      return ["360271000", "Prophylaxis - procedure intent (qualifier value)"]
    case ("COV_GECCO_THERAPEUTISCH_ABSICHT_THERAPEUTISCH"):
      return ["373808002", "Curative - procedure intent (qualifier value)"]
    default: [null, null]
  }
}

static String matchResponseToSTATUS(final String resp) {
  switch (resp) {
    case ("COV_NEIN"):
      return "not-taken"
    default: "active"
  }
}

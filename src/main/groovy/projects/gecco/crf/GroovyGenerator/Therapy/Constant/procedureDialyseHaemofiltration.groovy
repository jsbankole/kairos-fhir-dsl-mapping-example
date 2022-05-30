package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/dialysis
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
  if (crfName != "SarsCov2_THERAPIE" || studyVisitStatus != "APPROVED") {
    return //no export
  }
  final def crfItemRespHemo = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_DIALYSE" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemRespHemo || crfItemRespHemo[CrfItem.CATALOG_ENTRY_VALUE] == []) {
    return
  }

  String STATUScode = null
  crfItemRespHemo[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
    STATUScode = matchResponseToSTATUS(item[CatalogEntry.CODE] as String)
  }

  if (!STATUScode){
    return
  }

  id = "Procedure/Dialyse-" + context.source[studyVisitItem().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/dialysis"
  }

  status = STATUScode

  category {
    coding {
      system = "http://snomed.info/sct"
      code = "277132007"
      display = "Therapeutic procedure (procedure)"
    }
  }

  code {
    coding {
      system = "http://fhir.de/CodeSystem/dimdi/ops"
      code = "8-85"
      version = "2021"
    }
    coding {
      system = "http://snomed.info/sct"
      code = "108241001"
      display = "Dialysis procedure (procedure)"
    }
    text = "Dialysis"
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  performedDateTime {
    if(STATUScode == "in-progress"){
      date = normalizeDate(context.source[studyVisitItem().lastApprovedOn()] as String)
      precision = TemporalPrecisionEnum.DAY.toString()
    }
    else if(STATUScode == "not-done"){
      extension {
        url = "http://hl7.org/fhir/StructureDefinition/data-absent-reason"
        valueCode = "not-performed"
      }
    }
    else{
      extension {
        url = "http://hl7.org/fhir/StructureDefinition/data-absent-reason"
        valueCode = "unknown"
      }
    }
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
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

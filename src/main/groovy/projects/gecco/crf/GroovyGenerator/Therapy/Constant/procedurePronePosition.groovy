package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/proneposition-procedure
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
  if (crfName != "SarsCov2_THERAPIE" || studyVisitStatus == "OPEN") {
    return //no export
  }

  final def crfItemRespProne = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_BAUCHLAGE" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemRespProne || crfItemRespProne[CrfItem.CATALOG_ENTRY_VALUE] == []) {
    return
  }

  String[] STATUS = [null, null]
  crfItemRespProne[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
    STATUS = matchResponseToSTATUS(item[CatalogEntry.CODE] as String)
  }

  if (!STATUS[0]){
    return
  }

  id = "Procedure/PronePosition-" + context.source[studyVisitItem().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/prone-position"
  }

  status = STATUS[0]

  category {
    coding {
      system = "http://snomed.info/sct"
      code = "225287004"
    }
  }

  code {
    coding {
      system = "http://snomed.info/sct"
      code = "431182000"
    }
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  performedDateTime {
    if(STATUS[0] != "in-progress"){
      extension {
        url = "http://hl7.org/fhir/StructureDefinition/data-absent-reason"
        valueCode = "unknown"
      }
    }
    else{
      date = normalizeDate(context.source[studyVisitItem().crf().creationDate()] as String)
      precision = TemporalPrecisionEnum.DAY.toString()
    }
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static String[] matchResponseToSTATUS(final String resp) {
  switch (resp) {
    case ("COV_JA"):
      return ["in-progress", ""]
    case ("COV_NEIN"):
      return ["not-done", "not-performed"]
    case ("COV_UNBEKANNT"):
      return ["unknown", "unknown"]
    default: [null, null]
  }
}

package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import javax.management.openmbean.CompositeDataInvocationHandler

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
  if (crfName != "SarsCov2_THERAPIE" || studyVisitStatus != "APPROVED") {
    return //no export
  }

  final def crfItemRespThera = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_BEATMUNGSTYP" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemRespThera || crfItemRespThera[CrfItem.CATALOG_ENTRY_VALUE] == []) {
    return
  }

  final def fields = getFields(crfItemRespThera[CrfItem.CATALOG_ENTRY_VALUE][0][CatalogEntry.CODE] as String)
  if(!fields[0]) {
    return
  }

  id = "Procedure/VentilationType-" + context.source[studyVisitItem().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/respiratory-therapies"
  }

  category {
    coding {
      system = "http://snomed.info/sct"
      code = "277132007"
      display = "Therapeutic procedure (procedure)"
    }
  }

  status = fields[0]

  code {
    coding {
      system = "http://snomed.info/sct"
      code = fields[1]
      display = fields[2]
    }
  }

  if(fields[3]){
    usedCode {
      coding {
        system = "http://snomed.info/sct"
        code = fields[3]
        display = fields[4]
      }
    }
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  performedDateTime {
    if(fields[4] == "in-progress"){
      date = normalizeDateTime(context.source[studyVisitItem().lastApprovedOn()] as String)
    }
    else if(fields[4] == "not-done"){
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


static String normalizeDateTime(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static String[] getFields(final String resp) {
  // return [Procedure.status	Procedure.code x2	Procedure.usedCode x2]

  switch (resp) {
    case ("COV_NEIN"):
      return ["not-done", "40617009", "Artificial respiration (procedure)", null, null]

    case ("COV_NA"):
      return ["unknown", "40617009", "Artificial respiration (procedure)", null, null]

    case ("COV_NHFST"):
      return ["in-progress", "371907003", "Oxygen administration by nasal cannula (procedure)", "426854004", "High flow oxygen nasal cannula (physical object)"]

    case ("COV_NIB"):
      return ["in-progress", "428311008", "Noninvasive ventilation (procedure)", null, null]

    case ("COV_INVASIVE_BEATMUNG"):
      return ["in-progress", "40617009", "Artificial respiration (procedure)", "26412008", "Endotracheal tube, device (physical object)"]

    case ("COV_TRACHEOTOMIE"):
      return ["in-progress", "40617009", "Artificial respiration (procedure)", "129121000", "Tracheostomy tube, device (physical object)"]

    default: [null, null, null, null, null]
  }
}

package projects.gecco.crf.labVital

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.FlexiStudy
import de.kairos.fhir.centraxx.metamodel.LaborFindingLaborValue
import de.kairos.fhir.centraxx.metamodel.LaborValue
import de.kairos.fhir.centraxx.metamodel.StudyMember
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborMapping
import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/bloodgaspanel
 * @author Lukas Reinert
 * @since KAIROS-FHIR-DSL.v.1.9.0, CXX.v.3.18.1.7
 *
 */

observation {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL") {
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_VITALPARAMETER" || studyVisitStatus != "APPROVED") {
    return //no export
  }

  final def labValPaO2 = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_PAO2" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  final def labValPaCO2 = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_PACO2" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  final def labValFiO2 = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_FIO2" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  final def labValOxySat = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_PERI_O2" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  final def labVal_pH = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_PH_BLUT" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  if (!labValPaO2 &&
          !labValPaCO2 &&
          !labValFiO2 &&
          !labValOxySat &&
          !labVal_pH) {
    return //no export
  }

  id = "Observation/GasPanel-" + context.source[studyVisitItem().crf().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-gas-panel"
  }

  identifier {
    type{
      coding {
        system = "http://terminology.hl7.org/CodeSystem/v2-0203"
        code = "OBI"
      }
    }
    system = "http://www.acme.com/identifiers/patient"
    value = "Observation/PaO2-" + context.source[studyVisitItem().crf().id()]
    assigner {
      reference = "Assigner/" + context.source[studyVisitItem().crf().creator().id()]
    }
  }

  status = Observation.ObservationStatus.UNKNOWN

  category {
    coding {
      system = "http://loinc.org"
      code = "26436-6"
    }
    coding {
      system = "http://terminology.hl7.org/CodeSystem/observation-category"
      code = "laboratory"
    }
    coding {
      system = "http://loinc.org"
      code = "18767-4"
    }
  }

  code {
    coding {
      system = "http://loinc.org"
      code = "24336-0"
      display = "Gas panel - Arterial blood"
    }
    text = "Gas panel - Arterial blood"
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  for (labVal in [labValPaO2, labValPaCO2, labValFiO2, labValOxySat, labVal_pH]) {
    if(labVal){
      effectiveDateTime {
        date = normalizeDate(labVal[CrfItem.CREATIONDATE] as String)
        precision = TemporalPrecisionEnum.DAY.toString()
      }
      break
    }
  }

  if(labValPaO2){
    hasMember {
      reference = "Observation/PaO2-" + context.source[studyVisitItem().crf().id()]
    }
  }
  if(labValPaCO2){
    hasMember {
      reference = "Observation/PaCO2-" + context.source[studyVisitItem().crf().id()]
    }
  }
  if(labValFiO2){
    hasMember {
      reference = "Observation/FiO2-" + context.source[studyVisitItem().crf().id()]
    }
  }
  if(labValOxySat){
    hasMember {
      reference = "Observation/PeriO2Saturation-" + context.source[studyVisitItem().crf().id()]
    }
  }
  if(labVal_pH){
    hasMember {
      reference = "Observation/pH-" + context.source[studyVisitItem().crf().id()]
    }
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

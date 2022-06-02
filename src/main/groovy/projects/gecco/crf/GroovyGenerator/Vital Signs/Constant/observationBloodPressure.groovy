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
 * Specified by https://simplifier.net/forschungsnetzcovid-19/bloodpressure
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

  final def labValDia = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_BLUTDRUCK_DIA" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  final def labValSys = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_BLUTDRUCK_SYS" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!labValDia && !labValSys) {
    return
  }

  id = "Observation/BloodPressure-" + context.source[studyVisitItem().crf().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-pressure"
  }

  status = Observation.ObservationStatus.UNKNOWN

  category {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/observation-category"
      code = "vital-signs"
    }
  }

  code {
    coding {
      system = "http://loinc.org"
      code = "85354-9"
      display = "Blood pressure panel with all children optional"
    }
    coding {
      system = "http://snomed.info/sct"
      code = "75367002"
      display = "Blood pressure (observable entity)"
    }
    text = "Blood pressure"
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  effectiveDateTime {
    date = normalizeDateTime(context.source[studyVisitItem().lastApprovedOn()] as String)
  }

  // Systolic Blood pressure
  def numValSys = labValSys[CrfItem.NUMERIC_VALUE]
  if (numValSys) {
    component {
      code {
        coding{
          system = "http://loinc.org"
          code = "8480-6"
          display = "Systolic blood pressure"
        }
        coding{
          system =  "http://snomed.info/sct"
          code = "271649006"
          display = "Systolic blood pressure"
        }
        text = "Systolic blood pressure"
      }
      valueQuantity {
        value = numValSys
        unit = "mmHg"
        system = "http://unitsofmeasure.org"
        code = "mm[Hg]"
      }
    }
  }

  // Diastolic Blood pressure
  def numValDia = labValDia[CrfItem.NUMERIC_VALUE]
  if (numValDia) {
    component {
      code {
        coding{
          system = "http://loinc.org"
          code = "8462-4"
          display = "Diastolic blood pressure"
        }
        coding{
          system = "http://snomed.info/sct"
          code = "271650006"
          display = "Diastolic blood pressure (observable entity)"
        }
        text = "Diastolic blood pressure"
      }
      valueQuantity {
        value = numValDia
        unit = "mmHg"
        system = "http://unitsofmeasure.org"
        code = "mm[Hg]"
      }
    }
  }
}


static String normalizeDateTime(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

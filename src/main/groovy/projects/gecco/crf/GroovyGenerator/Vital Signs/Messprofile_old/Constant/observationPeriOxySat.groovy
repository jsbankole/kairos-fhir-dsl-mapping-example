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
 * Specified by https://simplifier.net/forschungsnetzcovid-19/oxygensaturation
 * @author Lukas Reinert
 * @since KAIROS-FHIR-DSL.v.1.9.0, CXX.v.3.18.1.7
 *
 */

observation {
  final def studyMember = context.source[laborMapping().relatedPatient().studyMembers()].find {
    it[StudyMember.STUDY][FlexiStudy.CODE] == "GECCO FINAL"
  }
  if (!studyMember) {
    return //no export
  }
  final def profileName = context.source[laborMapping().laborFinding().laborMethod().code()]
  if (profileName != "COV_GECCO_VITALPARAMTER") {
    return //no export
  }

  final def labVal = context.source[laborMapping().laborFinding().laborFindingLaborValues()].find {
    "COV_GECCO_PERI_O2" == it[LaborFindingLaborValue.LABOR_VALUE][LaborValue.CODE]
  }
  if (!labVal || !labVal[LaborFindingLaborValue.NUMERIC_VALUE]) {
    return
  }

  id = "Observation/PeriO2Saturation-" + context.source[laborMapping().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/oxygen-saturation"
  }

  identifier {
    type{
      coding {
        system = "http://terminology.hl7.org/CodeSystem/v2-0203"
        code = "OBI"
      }
    }
    system = "http://www.acme.com/identifiers/patient"
    value = "Observation/PaCO2-" + context.source[laborMapping().id()]
    assigner {
      reference = "Assigner/" + context.source[laborMapping().creator().id()]
    }
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
      code = "2708-6"
      display = "Oxygen saturation in Arterial blood"
    }
    coding {
      system = "http://snomed.info/sct"
      code = "431314004"
    }
    text = "Oxygen saturation in Arterial blood"
  }

  subject {
    reference = "Patient/Patient-" + context.source[laborMapping().relatedPatient().idContainer()]["psn"][0]
  }

  effectiveDateTime {
    date = normalizeDate(context.source[laborMapping().creationDate()] as String)
    precision = TemporalPrecisionEnum.DAY.toString()
  }

  valueQuantity {
    value = labVal[LaborFindingLaborValue.NUMERIC_VALUE]
    unit = "%"
    system = "http://unitsofmeasure.org"
    code = "%"
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

package projects.gecco.crf.labVital

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
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
 * Represented by a CXX LaborMapping
 * Specified by https://simplifier.net/forschungsnetzcovid-19/bodytemperature
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

  final def labVal = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_KOERPERTEMPERATURE" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!labVal[CrfItem.NUMERIC_VALUE]) {
    return //no export
  }

  id = "Observation/BodyTemp-" + context.source[studyVisitItem().crf().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/body-temperature"
  }

  identifier {
    type{
      coding {
        system = "http://terminology.hl7.org/CodeSystem/v2-0203"
        code = "OBI"
      }
    }
    system = "http://www.acme.com/identifiers/patient"
    value = "Observation/BodyTemp-" + context.source[studyVisitItem().crf().id()]
    assigner {
      reference = "Assigner/" + context.source[studyVisitItem().crf().creator().id()]
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
      code = "8310-5"
      display = "Body temperature"
    }
    coding {
      system = "http://snomed.info/sct"
      code = "386725007"
      display = "Body temperature (observable entity)"
    }
    text = "Body temperature"
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  effectiveDateTime {
    date = normalizeDate(labVal[CrfItem.CREATIONDATE] as String)
    precision = TemporalPrecisionEnum.DAY.toString()
  }

  valueQuantity {
    value = labVal[CrfItem.NUMERIC_VALUE]
    unit = "°C"
    system = "http://unitsofmeasure.org"
    code = "Cel"
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

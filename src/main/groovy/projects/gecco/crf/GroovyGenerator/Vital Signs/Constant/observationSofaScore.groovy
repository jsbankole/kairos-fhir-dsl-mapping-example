package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.FlexiStudy
import de.kairos.fhir.centraxx.metamodel.LaborFindingLaborValue
import de.kairos.fhir.centraxx.metamodel.LaborValue
import de.kairos.fhir.centraxx.metamodel.StudyMember
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborMapping
import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/sofa
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
    "COV_GECCO_SOFA" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!labVal[CrfItem.NUMERIC_VALUE]) {
    return
  }

  id = "Observation/SOFAScore-" + context.source[studyVisitItem().crf().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sofa-score"
  }

  identifier {
    type{
      coding {
        system = "http://terminology.hl7.org/CodeSystem/v2-0203"
        code = "OBI"
      }
    }
    system = "http://www.acme.com/identifiers/patient"
    value = "Observation/SOFAScore-" + context.source[studyVisitItem().crf().id()]
    assigner {
      reference = "Assigner/" + context.source[studyVisitItem().crf().creator().id()]
    }
  }

  status = Observation.ObservationStatus.UNKNOWN

  category {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/observation-category"
      code = "survey"
    }
  }

  code {
    coding {
      system = "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/ecrf-parameter-codes"
      code = "06"
      display = "SOFA-Score"
    }
    text = "Sepsis-related organ failure assessment score"
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  effectiveDateTime {
    date = normalizeDateTime(context.source[studyVisitItem().lastApprovedOn()] as String)
  }

  valueInteger(labVal[CrfItem.NUMERIC_VALUE] as Integer)
}


static String normalizeDateTime(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

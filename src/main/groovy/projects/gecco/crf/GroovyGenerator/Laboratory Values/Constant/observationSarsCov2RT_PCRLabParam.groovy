package projects.gecco.crf

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
 * Specified by https://simplifier.net/forschungsnetzcovid-19/sarscov2rtpcr
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.9.0, CXX.v.3.18.1.7
 *
 * hints:
 *  A StudyEpisode is no regular episode and cannot reference an encounter
 */

observation {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL") {
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_LABORPARAMETER" || studyVisitStatus == "OPEN") {
    return //no export
  }

  final def labVal = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_SARS_COV_2_PCR" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!labVal) {
    return
  }

  id = "Observation/SarsCov2RT-PCR-" + context.source[studyVisitItem().crf().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-rt-pcr"
  }

  // TODO check identifier
  identifier {
    type {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/v2-0203"
        code = "OBI"
      }
    }
    system = "https://www.charite.de/fhir/CodeSystem/lab-identifiers"
    value = "Observation/SarsCov2RT-PCR-" + context.source[studyVisitItem().crf().id()]  // "94500-6_SARS-CoV-2-RNA-Presence-in-Respiratory-specimen"
    assigner {
      reference = "Assigner/" + context.source[studyVisitItem().crf().creator().id()]  // "Organization/Charité"
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
  }

  code {
    coding {
      system = "http://loinc.org"
      code = "94500-6"
      display = "SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by NAA with probe detection"
    }
  }

  effectiveDateTime {
    date = normalizeDate(labVal[CrfItem.CREATIONDATE] as String)
    precision = TemporalPrecisionEnum.DAY.toString()
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
  }

  //Vaccine codes
  valueCodeableConcept {
    labVal[CrfItem.CATALOG_ENTRY_VALUE].each{ final catEntry ->
      final def res = mapDiscSNOMED(catEntry[CatalogEntry.CODE] as String)
      coding {
        system = "http://snomed.info/sct"
        code = res[0]
        display = res[1]
      }
      text = res[2]
    }
  }
}

static String[] mapDiscSNOMED(final String discharge) {
  switch (discharge) {
    default:
      return ["419984006", "Inconclusive (qualifier value)", "SARS-CoV-2-RNA nicht eindeutig"]
    case "COV_POSITIV":
      return ["260373001", "Detected (qualifier value)", "SARS-CoV-2-RNA positiv"]
    case "COV_NEGATIV":
      return ["260415000", " Not detected (qualifier value)", "SARS-CoV-2-RNA negativ"]
  }
}
static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}
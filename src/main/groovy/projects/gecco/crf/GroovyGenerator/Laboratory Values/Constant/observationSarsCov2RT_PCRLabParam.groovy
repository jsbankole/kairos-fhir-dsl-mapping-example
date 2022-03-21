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
  final def studyMember = context.source[laborMapping().relatedPatient().studyMembers()].find{
    it[StudyMember.STUDY][FlexiStudy.CODE] == "GECCO FINAL"
  }
  if (!studyMember) {
    return //no export
  }
  final def profileName = context.source[laborMapping().laborFinding().laborMethod().code()]
  if (profileName != "COV_GECOO_LABOR") {
    return //no export
  }

  final def labValDisc = context.source[laborMapping().laborFinding().laborFindingLaborValues()].find {
    "COV_GECCO_SARS_COV_2_PCR" == it[LaborFindingLaborValue.LABOR_VALUE][LaborValue.CODE]
  }
  if (!labValDisc) {
    return
  }

  id = "Observation/SarsCov2RT-PCR-" + context.source[laborMapping().id()]

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
    value = "94500-6_SARS-CoV-2-RNA-Presence-in-Respiratory-specimen"
    assigner {
      reference = "Organization/Charité"
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
    date = normalizeDate(context.source[laborMapping().creationDate()] as String)
    precision = TemporalPrecisionEnum.DAY.toString()
  }

  subject {
    reference = "Patient/Patient-" + context.source[laborMapping().relatedPatient().idContainer()]["psn"][0]
  }

  //Vaccine codes
  valueCodeableConcept {
    labValDisc[LaborFindingLaborValue.CATALOG_ENTRY_VALUE].each{ final catEntry ->
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
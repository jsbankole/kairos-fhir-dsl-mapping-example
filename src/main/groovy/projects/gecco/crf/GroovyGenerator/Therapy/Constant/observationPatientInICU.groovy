package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/patientinicu
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
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
  if (crfName != "SarsCov2_THERAPIE" || studyVisitStatus != "APPROVED") {
    return //no export
  }
  final def crfItemPatinICU = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_INTENSIVSTATION" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemPatinICU) {
    return
  }
  if (crfItemPatinICU[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "Observation/PatientInICU-" + context.source[studyVisitItem().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/patient-in-icu"
    }

    status = "final"

    category {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/observation-category"
        code = "survey"
        display = "Survey"
      }
    }

    code {
      coding {
        system = "http://loinc.org"
        code = "95420-6"
        display = "Whether the patient was admitted to intensive care unit (ICU) for condition of interest"
      }
    }

    subject {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
    }

    effectiveDateTime {
      date = normalizeDateTime(context.source[studyVisitItem().lastApprovedOn()] as String)
    }

    valueCodeableConcept {
      crfItemPatinICU[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
        final def fields = getFields(item[CatalogEntry.CODE] as String)
        if (fields[0]) {
          coding {
            system = "http://snomed.info/sct"
            code = fields[0]
            display = fields[1]
          }
        }
      }
    }
  }
}

static String normalizeDateTime(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static String[] getFields(final String patInICU) {
  switch (patInICU) {
    default:
      return [null, null]
    case "COV_JA":
      return ["373066001", "Yes (qualifier value)"]
    case "COV_NEIN":
      return ["373067005", "No (qualifier value)"]
    case "COV_UNBEKANNT":
      return ["261665006", "Unknown (qualifier value)"]
  }
}

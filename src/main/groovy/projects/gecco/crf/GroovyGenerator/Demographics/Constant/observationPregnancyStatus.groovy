package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/pregnancystatus
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
  if (crfName != "SarsCov2_DEMOGRAPHIE" || studyVisitStatus != "APPROVED") {
    return //no export
  }

  final def crfItemPreg = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_SCHWANGERSCHAFT" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemPreg) {
    return
  }
  if (crfItemPreg[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "Observation/Pregnancy-" + context.source[studyVisitItem().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/pregnancy-status"
    }

    status = Observation.ObservationStatus.UNKNOWN

    category {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/observation-category"
        code = "social-history"
      }
    }

    code {
      coding {
        system = "http://loinc.org"
        code = "82810-3"
      }
    }

    subject {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
    }

    effectiveDateTime {
      date = normalizeDateTime(context.source[studyVisitItem().lastApprovedOn()] as String)
    }

    valueCodeableConcept {
      crfItemPreg[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
        final def fields = getPregInfo(item[CatalogEntry.CODE] as String)

        if (fields[0]) {
          coding {
            system = "http://snomed.info/sct"
            code = fields[0]
          }
          if (fields[1]) {
            coding {
              system = "http://loinc.org"
              code = fields[1]
            }
          }
        }
      }
    }
  }
}

static String normalizeDateTime(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static String[] getPregInfo(final String pregStatus) {
  //[SnomedCode, LoincCode]
  switch (pregStatus) {
    default:
      return null
    case "COV_JA":
      return ["77386006", "LA15173-0"]
    case "COV_NEIN":
      return ["60001007", "LA26683-5"]
    case "COV_UNBEKANNT":
      return ["261665006", "LA4489-6"]
  }
}


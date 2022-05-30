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
 * Specified by https://simplifier.net/forschungsnetzcovid-19/sexassignedatbirth
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

  final def crfItemGen = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_GESCHLECHT_GEBURT" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemGen) {
    return
  }
  if (crfItemGen[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "Observation/SexAssignedAtBirth-" + context.source[studyVisitItem().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sex-assigned-at-birth"
    }

    status = Observation.ObservationStatus.FINAL

    category {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/observation-category"
        code = "social-history"
        display = "Social History"
      }
    }

    code {
      coding {
        system = "http://loinc.org"
        code = "76689-9"
        display = "Sex assigned at birth"
      }
    }

    subject {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
    }

    effectiveDateTime {
      date = normalizeDate(context.source[studyVisitItem().lastApprovedOn()] as String)
    }

    valueCodeableConcept {
      crfItemGen[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
        final def fields = getGenderFields(item[CatalogEntry.CODE] as String)
        if (!fields[0]) {
          return
        }
        coding {
          system = fields[1]
          code = fields[0]
        }
      }
    }
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static String[] getGenderFields(final String gender) {
  switch (gender) {
    case "COV_MAENNLICH":
      return ["male", "http://hl7.org/fhir/administrative-gender"]
    case "COV_WEIBLICH":
      return ["female", "http://hl7.org/fhir/administrative-gender"]
    case "COV_KEINE_ANGABE":
      return ["X", "http://fhir.de/CodeSystem/gender-amtlich-de"]
    case "COV_DIVERS":
      return ["D", "http://fhir.de/CodeSystem/gender-amtlich-de"]
    case "COV_UNBEKANNT":
      return ["unknown", "http://hl7.org/fhir/administrative-gender"]
    default:
      [null, null]
  }
}

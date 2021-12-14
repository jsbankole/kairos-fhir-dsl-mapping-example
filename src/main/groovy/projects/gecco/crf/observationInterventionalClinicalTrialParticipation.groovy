package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/interventionalclinicaltrialparticipation
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
  if (crfName != "SarsCov2_STUDIENEINSCHLUSS / EINSCHLUSSKRITERIEN" || studyVisitStatus == "OPEN") {
    return //no export
  }
  final def crfItemStudy = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_OUTCOME_INTERVENTIONELL_STUDIE" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemStudy || crfItemStudy[CrfItem.CATALOG_ENTRY_VALUE] == []) {
    return
  }

  final String answerCODE = crfItemStudy[CrfItem.CATALOG_ENTRY_VALUE][0][CatalogEntry.CODE]

  id = "Observation/InterventionalTrialParticipation-" + context.source[studyVisitItem().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/interventional-clinical-trial-participation"
  }

  // TODO: What about identifier ?

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
      code = "03"
      display = "Participation in interventional clinical trials"
    }
    text = "Has the patient participated in one or more interventional clinical trials?"
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().id()]
  }

  effectiveDateTime {
    date = normalizeDate(context.source[studyVisitItem().crf().creationDate()] as String)
    precision = TemporalPrecisionEnum.SECOND.toString()
  }

  valueCodeableConcept {
    final def SNOMEDfields = getSnomedFields(answerCODE)
    if (SNOMEDfields[0]) {
      coding {
        system = "http://snomed.info/sct"
        code = SNOMEDfields[0]
        display = SNOMEDfields[1]
      }
    }

  }

  // EudraCT Number
  final def crfItemEudraCT = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_STUDIE_EUDRACT_NUMMER" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  if (crfItemEudraCT[CrfItem.STRING_VALUE]) {
    component {
      code {
        coding {
          system = "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/ecrf-parameter-codes"
          code = "04"
          display = "EudraCT Number"
        }
        text = "EudraCT (European Union Drug Regulating Authorities Clinical Trials) registration number"
      }
      valueString = new StringType(crfItemEudraCT[CrfItem.STRING_VALUE] as String)
    }
  }

  // NCT Number
  final def crfItemNCT = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_STUDIE_NCT_NUMMER" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (crfItemNCT[CrfItem.STRING_VALUE]) {
    component {
      code {
        coding {
          system = "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/ecrf-parameter-codes"
          code = "05"
          display = "NCT Number"
        }
        text = "NCT number"
      }
      valueString = new StringType(crfItemNCT[CrfItem.STRING_VALUE] as String)
    }
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static String[] getSnomedFields(final String inclusion) {
  switch (inclusion) {
    default:
      return null
    case "COV_JA":
      return ["373066001", "Yes (qualifier value)"]
    case "COV_NEIN":
      return ["373067005", "No (qualifier value)"]
    case "COV_UNBEKANNT":
      return ["261665006", "Unknown (qualifier value)"]
    case "COV_NA":
      return ["385432009", "Not applicable (qualifier value)"]
  }
}

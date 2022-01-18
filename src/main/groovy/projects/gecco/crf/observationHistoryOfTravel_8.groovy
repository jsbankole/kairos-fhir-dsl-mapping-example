package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue
import de.kairos.fhir.centraxx.metamodel.MultilingualEntry
import de.kairos.fhir.centraxx.metamodel.PrecisionDate
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/historyoftravel
 * @author Mike Wähnert
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
  if (crfName != "SarsCov2_ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus == "OPEN") {
    return //no export
  }

  final def crfItemTravel = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_REISE" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  if (!crfItemTravel) {
    return
  }

  final def crfItemTravelValue = crfItemTravel[CrfItem.CATALOG_ENTRY_VALUE][0][CatalogEntry.CODE] as String

  final def SNOMEDcode = mapTravel(crfItemTravelValue)
  if (!SNOMEDcode[0]) {
    return
  }

  final def iter = 8

  // If there is no line do not generate file
  if(crfItemTravelValue == "COV_JA") {
    final def crfItemStartDate = context.source[studyVisitItem().crf().items()].findAll {
      iter == it[CrfItem.VALUE_INDEX] as int && "COV_GECCO_REISE_STARTDATUM" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
    }
    if (!crfItemStartDate) {
      return
    }
  }

  id = "Observation/HistoryOfTravel-" + context.source[studyVisitItem().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/history-of-travel"
  }

  //TODO: Check identifier
  identifier {
    type {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/v2-0203"
        code = "OBI"
      }
    }
    system = "https://www.charite.de/fhir/CodeSystem/observation-identifiers"
    value = "8691-8_HistoryOfTravel"
    assigner {
      reference = "Organization/Charité"
    }
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
      code = "8691-8"
      display = "History of Travel"
    }
    coding{
      system = "http://snomed.info/sct"
      code = "443846001"
      display = "Detail of history of travel (observable entity)"
    }
    text = "History of travel"
  }

  subject {
    reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().id()]
  }

  effectiveDateTime {
    date = normalizeDateTime(context.source[studyVisitItem().crf().creationDate()] as String)
    precision = TemporalPrecisionEnum.DAY.toString()
  }

  valueCodeableConcept {
    coding {
      system = "http://snomed.info/sct"
      code = SNOMEDcode[0]
      display = SNOMEDcode[1]
    }
  }

  if(crfItemTravelValue == "COV_JA"){

    final def crfItemStartDate = context.source[studyVisitItem().crf().items()].findAll {
      iter == it[CrfItem.VALUE_INDEX] as int && "COV_GECCO_REISE_STARTDATUM" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
    }

    final def crfItemEndDate = context.source[studyVisitItem().crf().items()].findAll {
      iter == it[CrfItem.VALUE_INDEX] as int && "COV_GECCO_REISE_ENDDATUM" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
    }

    final def crfItemCountry = context.source[studyVisitItem().crf().items()].findAll {
      iter == it[CrfItem.VALUE_INDEX] as int && "COV_GECCO_REISE_LAND" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
    }

    final def crfItemState = context.source[studyVisitItem().crf().items()].findAll {
      iter == it[CrfItem.VALUE_INDEX] as int && "COV_GECCO_REISE_BUNDESLAND" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
    }

    final def crfItemCity = context.source[studyVisitItem().crf().items()].findAll {
      iter == it[CrfItem.VALUE_INDEX] as int && "COV_GECCO_REISE_STADT" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
    }

    final def startDate = normalizeDate(crfItemStartDate[CrfItem.DATE_VALUE][PrecisionDate.DATE][0] as String)
    final def endDate = normalizeDate(crfItemEndDate[CrfItem.DATE_VALUE][PrecisionDate.DATE][0] as String)
    final def country = crfItemCountry[CrfItem.CATALOG_ENTRY_VALUE][0][0][CatalogEntry.NAME_MULTILINGUAL_ENTRIES][MultilingualEntry.VALUE][1]
    final def countryCode = (crfItemCountry[CrfItem.CATALOG_ENTRY_VALUE][0][0][CatalogEntry.CODE] as String).split("_")[-1]
    final def state = crfItemState[CrfItem.STRING_VALUE][0]
    final def city = crfItemCity[CrfItem.STRING_VALUE][0]

    component {
      code {
        coding  {
          system = "http://loinc.org"
          code = "82752-7"
          display = "Date travel started"
        }
        text = "Travel start date"
      }
      valueDateTime = startDate
    }

    component {
      code {
        coding {
          system = "http://loinc.org"
          code = "94651-7"
          display = "Country of travel"
        }

        text = "Country of travel"
      }
      valueCodeableConcept {
        coding {
          system = "urn:iso:std:iso:3166"
          code = countryCode
          display = country
        }
        text = country
      }
    }

    if(state){
      component {
        code {
          coding {
            system = "http://loinc.org"
            code = "82754-3"
            display = "State of travel"
          }
          text = "State of travel"
        }
        valueCodeableConcept {
          coding {
            display = state
          }
          text = state
        }
      }
    }

    component {
      code {
        coding {
          system = "http://loinc.org"
          code = "94653-3"
          display = "City of travel"
        }
        text = "City of travel"
      }
      valueString = new StringType(city as String)
    }

    component {
      code {
        coding {
          system = "http://loinc.org"
          code = "91560-3"
          display = "Date of departure from travel destination"
        }

        text = "Travel end date"
      }
      valueDateTime = endDate
    }
  }
}


static String normalizeDateTime(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}

static String[] mapTravel(final String travel) {
  switch (travel) {
    case "COV_JA":
      return ["373066001", "Yes (qualifier value)"]
    case "COV_NEIN":
      return ["373067005", "No (qualifier value)"]
    case "COV_UNBEKANNT":
      return ["261665006", "Unknown (qualifier value)"]
    default:
      return [null, null]
  }
}

package projects.gecco


import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import java.time.LocalDate
import java.time.Period

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/patient
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 *
 * hints:
 *  A StudyEpisode is no regular episode and cannot reference an encounter
 */
patient {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL") {
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_DEMOGRAPHIE" || studyVisitStatus == "OPEN") {
    return //no export
  }

  id = "Patient/Cxx-Patient-" + context.source[studyVisitItem().studyMember().patientContainer().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient"
  }

  // Ethnicity
  final def crfItemEthn = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_ETHNISCHE_ZUGEHOERIGKEIT" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  if (crfItemEthn && crfItemEthn[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    final def fields = getEthnicityInfo(crfItemEthn[CrfItem.CATALOG_ENTRY_VALUE][0][CatalogEntry.CODE] as String)
    if(fields[0]){
      extension {
        url = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/ethnic-group"
        valueCoding {
          system = fields[2]
          code = fields[0]
          display = fields[1]
        }
      }
    }
  }

  // Birthdate & Age
  final def crfItemBirthdate = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_GEBURTSDATUM" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }

  if (crfItemBirthdate) {
    final def birthDateStr = crfItemBirthdate[CrfItem.DATE_VALUE].toString().substring(6, 16)
    final def currentDateStr = normalizeDate(context.source[studyVisitItem().crf().lastChangedOn()] as String)
    extension {
      extension {
        url = "dateTimeOfDocumentation"
        valueDateTime = currentDateStr
      }
      extension {
        url = "age"
        valueAge {
          value = computeAge(birthDateStr, currentDateStr)
          system = "http://unitsofmeasure.org"
          code = "a"
          unit = "years"
        }
      }
      url = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/age"
    }
    birthDate = birthDateStr
  }

  active = context.source[studyVisitItem().studyMember().patientContainer().patientStatus()]

  // Gender
  final def crfItemGender = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_GESCHLECHT_GEBURT" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (crfItemGender && crfItemGender[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    gender = mapGender(crfItemGender[CrfItem.CATALOG_ENTRY_VALUE][0][CatalogEntry.CODE] as String)
  }
}

static String mapGender(final String gender) {
  switch (gender) {
    case "COV_MAENNLICH":
      return "male"
    case "COV_WEIBLICH":
      return "female"
    case "COV_DIVERS":
      return "other"
    default:
      return "unknown"
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}

//Compute age of patient from birthdate
static int computeAge(final String startDateString, final String endDateString) {

  String[] startStr = startDateString.split("-")
  String[] endStr = endDateString.split("-")

  LocalDate startDate = LocalDate.of(startStr[0] as int, startStr[1] as int, startStr[2] as int )
  LocalDate endsDate = LocalDate.of(endStr[0] as int, endStr[1] as int, endStr[2] as int )

  return Period.between(startDate, endsDate).years

}

//Function to map ethnicities
static String[] getEthnicityInfo(final String ethnicity) {
  switch (ethnicity) {
    case "COV_KAUKASIER":
      return ["14045001", "Caucasian (ethnic group)", "http://snomed.info/sct"]
    case "COV_AFRIKANER":
      return ["18167009", "Black African (ethnic group)", "http://snomed.info/sct"]
    case "COV_ASIATE":
      return ["315280000", "Asian - ethnic group (ethnic group)", "http://snomed.info/sct"]
    case "COV_ARABISCH":
      return ["90027003", "Arabs (ethnic group)", "http://snomed.info/sct"]
    case "COV_LATEIN_AMERIKANISCH":
      return ["2135-2", "Hispanic or Latino", "urn:oid:2.16.840.1.113883.6.238"]
    case "COV_GEMISCHTE":
      return ["26242008", "Mixed (qualifier value)", "http://snomed.info/sct"]
    case "COV_ETHNISCHE_ZUGEHOERIGKEIT_ANDERE":
      return ["372148003", "Ethnic group (ethnic group)", "http://snomed.info/sct"]
    default:
      [null, null, null]
  }
}


/*

final def DateOfDeath = "2020-01-01"//context.source[studyVisitItem().studyMember().patientContainer().dateOfDeath()] //TODO
static int computeAge(final String dateString, final String dateOfDeath) {
  if (dateOfDeath){
    final int dod = dateOfDeath.substring(0,4).toInteger()
    final int doe = dateString.substring(0, 4).toInteger()
    return dod - doe
  }
  else{
    final int now = Calendar.getInstance().get(Calendar.YEAR)
    final int doe = dateString.substring(0, 4).toInteger()
    return now - doe
  }
}

 */

package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.DateTimeType

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/donotresuscitateorder
 * @author Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 */
consent {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL") {
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus != "APPROVED") {
    return //no export
  }

  final def crfItemDNR = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_DNR_STATUS" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemDNR) {
    return //no export
  }
  if (crfItemDNR[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "Consent/DoNotResucitateOrder-" + context.source[studyVisitItem().crf().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/do-not-resuscitate-order")
    }

    status = "active"

    scope {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/consentscope"
        code = "adr"
      }
    }

    category {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/consentcategorycodes"
        code = "dnr"
      }
    }

    patient {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
    }

    dateTime = new DateTimeType(normalizeDate(context.source[studyVisitItem().lastApprovedOn()] as String))

    policy{
      uri = "https://www.aerzteblatt.de/archiv/65440/DNR-Anordnungen-Das-fehlende-Bindeglied"
    }

    provision {

      type = Consent.ConsentProvisionType.PERMIT

      code {
        crfItemDNR[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
          final def DNRcode = mapDNR(item[CatalogEntry.CODE] as String)
          if (DNRcode[0]) {
            coding {
              system = "http://snomed.info/sct"
              code = DNRcode[0]
              display = DNRcode[1]
            }
          }
        }
      }
    }
  }
}


static String[] mapDNR(final String resp) {
  switch (resp) {
    case ("COV_JA"):
      return ["304253006", "Not for resuscitation (finding)"]
    case ("COV_NEIN"):
      return ["304252001", "For resuscitation (finding)"]
    case ("COV_UNBEKANNT"):
      return ["261665006", "Unknown (qualifier value)"]
    default: [null, null]
  }
}

//static DateTimeType getCurrentDate(){
//  def calendar = Calendar.getInstance()
//  return new DateTimeType("" + calendar.get(Calendar.YEAR) + "-" +
//          String.format("%02d", (calendar.get(Calendar.MONTH) + 1)) + "-" +
//          String.format("%02d", (calendar.get(Calendar.DAY_OF_MONTH))))
//}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}

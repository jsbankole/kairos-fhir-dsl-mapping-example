package projects.gecco.crf

import de.kairos.fhir.centraxx.metamodel.ConsentableAction
import de.kairos.fhir.centraxx.metamodel.OrganisationUnit
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.DateTimeType

import static de.kairos.fhir.centraxx.metamodel.RootEntities.consent
import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborMapping

/**
 * Represented by a CXX Consent
 * Specified by http://fhir.de/ConsentManagement/StructureDefinition/Consent
 * @author Lukas Reinert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 */

consent {

  final def consentCode = context.source[consent().consentType().code()]
  if (consentCode != "CONSENT_PEBF_V4.3") {
    return //no export
  }

  final def validUntil = context.source[consent().validUntil().date()]
  final def validFrom = context.source[consent().validFrom().date()]
  final String interpretedStatus = validUntilInterpreter(validUntil as String)
  final def studyID = context.source[consent().consentType().flexiStudy().id()]

  id = "Consent/Consent-PEBF_V4_3-" + context.source[consent().id()]

  meta{
    source = "https://fhir.centraxx.de"
    profile "http://fhir.de/ConsentManagement/StructureDefinition/Consent"
  }

  extension {
    url = "http://fhir.de/ConsentManagement/StructureDefinition/DomainReference"
    extension{
      url = "domain"
      valueReference {
        reference = "ResearchStudy/" + studyID
      }
    }
    extension{
      url = "status"
      valueCoding {
        system = "http://hl7.org/fhir/publication-status"
        code = interpretedStatus
      }
    }
  }

  status = interpretedStatus

  scope {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/consentscope"
      code = "research"
    }
  }

  category {
    coding {
      system = "http://loinc.org"
      code = "57016-8"
    }
  }

  patient{
    reference = "Patient/Patient-" +context.source[consent().patientContainer().idContainer()]["psn"][0]
  }

  dateTime = new DateTimeType(context.source[consent().validFrom().date()])

  context.source[consent().patientContainer().organisationUnits()].each { final oe ->
    organization {
      reference = "Organisationseinheit/" + oe[OrganisationUnit.ID]
      display = oe[OrganisationUnit.CODE]
    }
  }

  sourceReference {
    reference = "PatientConsent/" + context.source[consent().id()]
  }

  // TODO check this one
  policyRule {
    text = "Patienteneinwilligung MII|1.6.f"
  }

  final def consentParts = []

  // TODO: check the all
  // All accepted - NOT only parts accepted
  if (!context.source[consent().consentPartsOnly()]){
    consentParts.addAll(
            "IDAT_erheben",
            "IDAT_speichern_verarbeiten",
            "IDAT_zusammenfuehren_Dritte",
            "IDAT_bereitstellen_EU_DSGVO_NIVEAU",
            "MDAT_zusammenfuehren_Dritte",
            "BIOMAT_erheben",
            "BIOMAT_lagern_verarbeiten",
            "BIOMAT_Eigentum_uebertragen",
            "BIOMAT_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU",
            "BIOMAT_Analysedaten_zusammenfuehren_Dritte",
            "Rekontaktierung_Ergebnisse_erheblicher_Bedeutung",
            "Rekontaktierung_Ergaenzungen")
  }
  // Partially accepted
  else {

    final def consentedParts = context.source[consent().consentElements().consentableAction().code()]

    // Mentioned as 1 but not 2
    if(consentedParts.contains("Daten und Biomaterialien")){
      consentParts.addAll(
              "IDAT_zusammenfuehren_Dritte",
              "IDAT_bereitstellen_EU_DSGVO_NIVEAU",
              "MDAT_zusammenfuehren_Dritte",
              "BIOMAT_erheben",
              "BIOMAT_lagern_verarbeiten",
              "BIOMAT_Eigentum_uebertragen",
              "BIOMAT_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU",
              "BIOMAT_Analysedaten_zusammenfuehren_Dritte"
      )
    }

    // Mentioned as 2 but not 1
    if(consentedParts.contains("Rekontakt")){
      consentParts.addAll(
              "Rekontaktierung_Ergebnisse_erheblicher_Bedeutung",
              "Rekontaktierung_Ergaenzungen"
      )
    }

    // Mentioned as both 1 or 2
    if(consentedParts.contains("Daten und Biomaterialien") || consentedParts.contains("Rekontakt")){
      consentParts.addAll(
              "IDAT_erheben",
              "IDAT_speichern_verarbeiten",
      )
    }

    // Mentioned as both 1 and 2
    if(consentedParts.contains("Daten und Biomaterialien") && consentedParts.contains("Rekontakt")){
//      consentParts.addAll(
//      )
    }
  }

  provision {
    consentParts.each{ final cA ->
      type = Consent.ConsentProvisionType.PERMIT
      period {
        start = validFrom
        if (validUntil){
          end = validUntil
        }
      }
      code{
        coding{
          // TODO Check system and so on ->  inspired from here https://ig.fhir.de/einwilligungsmanagement/stable/Consent.html
          system = "urn:oid:2.16.840.1.113883.3.1937.777.24.5.3"
          code = mapConsentData(cA as String)[1]
          display = mapConsentData(cA as String)[1]
        }
      }
    }
  }
}

static String validUntilInterpreter(String validUntilDate){

  if(!validUntilDate){
    return "active"
  }

  def untilDate = Date.parse("yyyy-MM-dd", validUntilDate.substring(0,10))
  def currDate = new Date().clearTime()

  if (currDate <= untilDate){
    return "active"
  }
  else {
    return "inactive"
  }
}

static String[] mapConsentData(final String cxxConsentPart){
  switch(cxxConsentPart) {
        case ("PATDAT_erheben_speichern_nutzen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.1", "PATDAT_erheben_speichern_nutzen"]
        case ("IDAT_erheben"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.2", "IDAT_erheben"]
        case ("IDAT_speichern_verarbeiten"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.3", "IDAT_speichern_verarbeiten"]
        case ("IDAT_zusammenfuehren_Dritte"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.4", "IDAT_zusammenfuehren_Dritte"]
        case ("IDAT_bereitstellen_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.5", "IDAT_bereitstellen_EU_DSGVO_NIVEAU"]
        case ("MDAT_erheben"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.6", "MDAT_erheben"]
        case ("MDAT_speichern_verarbeiten"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.7", "MDAT_speichern_verarbeiten"]
        case ("MDAT_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.8", "MDAT_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU"]
        case ("MDAT_zusammenfuehren_Dritte"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.9", "MDAT_zusammenfuehren_Dritte"]
        case ("Rekontaktierung_Ergebnisse_erheblicher_Bedeutung"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.37", "Rekontaktierung_Ergebnisse_erheblicher_Bedeutung"]
        case ("PATDAT_retrospektiv_verarbeiten_nutzen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.44", "PATDAT_retrospektiv_verarbeiten_nutzen"]
        case ("MDAT_retro_speichern_verarbeiten"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.45", "MDAT_retro_speichern_verarbeiten"]
        case ("MDAT_retro_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.46", "MDAT_retro_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU"]
        case ("MDAT_retro_zusammenfuehren_Dritte"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.47", "MDAT_retro_zusammenfuehren_Dritte"]
        case ("PATDAT_Weitergabe_non_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.48", "PATDAT_Weitergabe_non_DSGVO_NIVEAU"]
        case ("MDAT_bereitstellen_non_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.49", "MDAT_bereitstellen_non_EU_DSGVO_NIVEAU"]
        case ("KKDAT_retrospektiv_uebertragen_speichern_nutzen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.10", "KKDAT_retrospektiv_uebertragen_speichern_nutzen"]
        case ("KKDAT_5J_retro_uebertragen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.11", "KKDAT_5J_retro_uebertragen"]
        case ("KKDAT_5J_retro_speichern_verarbeiten"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.12", "KKDAT_5J_retro_speichern_verarbeiten"]
        case ("KKDAT_5J_retro_wissenschaftlich_nutzen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.13", "KKDAT_5J_retro_wissenschaftlich_nutzen"]
        case ("KKDAT_5J_retro_uebertragen_KVNR"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.38", "KKDAT_5J_retro_uebertragen_KVNR"]
        case ("KKDAT_prospektiv_uebertragen_speichern_nutzen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.14", "KKDAT_prospektiv_uebertragen_speichern_nutzen"]
        case ("KKDAT_5J_pro_uebertragen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.15", "KKDAT_5J_pro_uebertragen"]
        case ("KKDAT_5J_pro_speichern_verarbeiten"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.16", "KKDAT_5J_pro_speichern_verarbeiten"]
        case ("KKDAT_5J_pro_wissenschaftlich_nutzen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.17", "KKDAT_5J_pro_wissenschaftlich_nutzen"]
        case ("KKDAT_5J_pro_uebertragen_KVNR"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.39", "KKDAT_5J_pro_uebertragen_KVNR"]
        case ("BIOMAT_erheben_lagern_nutzen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.18", "BIOMAT_erheben_lagern_nutzen"]
        case ("BIOMAT_erheben"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.19", "BIOMAT_erheben"]
        case ("BIOMAT_lagern_verarbeiten"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.20", "BIOMAT_lagern_verarbeiten"]
        case ("BIOMAT_Eigentum_uebertragen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.21", "BIOMAT_Eigentum_uebertragen"]
        case ("BIOMAT_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.22", "BIOMAT_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU"]
        case ("BIOMAT_Analysedaten_zusammenfuehren_Dritte"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.23", "BIOMAT_Analysedaten_zusammenfuehren_Dritte"]
        case ("BIOMAT_Zusatzentnahme"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.24", "BIOMAT_Zusatzentnahme"]
        case ("BIOMAT_Zusatzmengen_entnehmen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.25", "BIOMAT_Zusatzmengen_entnehmen"]
        case ("BIOMAT_retrospektiv_speichern_nutzen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.50", "BIOMAT_retrospektiv_speichern_nutzen"]
        case ("BIOMAT_retro_lagern_verarbeiten"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.51", "BIOMAT_retro_lagern_verarbeiten"]
        case ("BIOMAT_retro_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.52", "BIOMAT_retro_wissenschaftlich_nutzen_EU_DSGVO_NIVEAU"]
        case ("BIOMAT_retro_Analysedaten_zusammenfuehren_Dritte"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.53", "BIOMAT_retro_Analysedaten_zusammenfuehren_Dritte"]
        case ("BIOMAT_Weitergabe_non_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.54", "BIOMAT_Weitergabe_non_EU_DSGVO_NIVEAU"]
        case ("BIOMAT_bereitstellen_non_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.55", "BIOMAT_bereitstellen_non_EU_DSGVO_NIVEAU"]
        case ("Rekontaktierung_Ergaenzungen"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.26", "Rekontaktierung_Ergaenzungen"]
        case ("Rekontaktierung_Verknuepfung_Datenbanken"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.27", "Rekontaktierung_Verknuepfung_Datenbanken"]
        case ("Rekontaktierung_weitere_Erhebung"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.28", "Rekontaktierung_weitere_Erhebung"]
        case ("Rekontaktierung_weitere_Studien"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.29", "Rekontaktierung_weitere_Studien"]
        case ("Rekontaktierung_Zusatzbefund"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.30", "Rekontaktierung_Zusatzbefund"]
        case ("Z1_GECCO83_Nutzung_NUM_CODEX"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.32", "Z1_GECCO83_Nutzung_NUM_CODEX"]
        case ("MDAT_GECCO83_komplettieren_einmalig"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.40", "MDAT_GECCO83_komplettieren_einmalig"]
        case ("MDAT_GECC083_erheben"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.43", "MDAT_GECC083_erheben"]
        case ("MDAT_GECCO83_bereitstellen_NUM_CODEX"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.33", "MDAT_GECCO83_bereitstellen_NUM_CODEX"]
        case ("MDAT_GECCO83_speichern_verarbeiten_NUM_CODEX"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.34", "MDAT_GECCO83_speichern_verarbeiten_NUM_CODEX"]
        case ("MDAT_GECCO83_wissenschaftlich_nutzen_NUM_CODEX_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.56", "MDAT_GECCO83_wissenschaftlich_nutzen_NUM_CODEX_EU_DSGVO_NIVEAU"]
        case ("Z1_GECCO83_Weitergabe_NUM_CODEX_non_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.35", "Z1_GECCO83_Weitergabe_NUM_CODEX_non_EU_DSGVO_NIVEAU"]
        case ("MDAT_GECCO83_bereitstellen_NUM_CODEX_non_EU_DSGVO_NIVEAU"):
      return ["2.16.840.1.113883.3.1937.777.24.5.3.36", "MDAT_GECCO83_bereitstellen_NUM_CODEX_non_EU_DSGVO_NIVEAU"]
   }
}
